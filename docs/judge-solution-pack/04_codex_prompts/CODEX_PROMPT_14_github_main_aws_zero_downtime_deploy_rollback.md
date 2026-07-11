# Codex Prompt 14 — GitHub Main 병합, AWS 무중단 배포, 롤백 검증

목표:
보안 점검 이후 변경사항을 GitHub `main` 기준으로 정리하고, AWS Lightsail 운영 서버에 중단 시간을 최소화해 반영한 뒤, 실패 시 즉시 이전 인스턴스로 되돌릴 수 있는 증거 기반 배포를 수행한다.

기준 문서:
- `SECURITY_AUDIT_REPORT.md`가 있으면 최우선 기준으로 삼는다.
- 현재 저장소에 해당 파일이 없으면 `docs/security-deployment-audit-2026-07-01.md`, `docs/ops-deploy-observability-report.md`, `docs/lightsail-8gb-upsize-runbook-2026-07-06.md`, `docs/production-readiness-checklist.md`를 기준으로 삼고, 보고서 부재를 결과에 명시한다.
- 보안 문서와 runtime 동작이 충돌하면 runtime 검증 결과를 우선하고 문서 보정 항목으로 남긴다.

절대 원칙:
- 운영 secret, `.env`, SSH key, JWT secret, DB password, API key를 출력하지 않는다.
- `git reset --hard`, 강제 push, 운영 DB 초기화, 운영 데이터 삭제는 사용하지 않는다.
- 최종 Gate는 deterministic rule score와 risk/fallback 조건만 소유한다. LLM/Agent가 `finalGate`를 바꿨다고 표현하지 않는다.
- 배포 전후 모든 검증은 실행한 명령, commit SHA, URL, 결과를 증거로 남긴다.
- 실패하면 즉시 중단하고 롤백 여부를 판단한다. 실패를 숨기고 배포 완료로 기록하지 않는다.

사전 상태 확인:
1. `git status --short --branch`
2. `git rev-parse HEAD`
3. `git log --oneline --decorate -12`
4. `git remote -v`
5. `git fetch --prune origin`
6. `git diff --stat origin/main..HEAD`
7. `git diff --stat main..HEAD`
8. `gh auth status` 또는 GitHub 플러그인/웹 UI 접근 가능성 확인

병합 판단:
- 현재 branch HEAD가 `origin/main`과 같으면 추가 main 병합은 필요 없다고 기록한다.
- 로컬 `main`만 뒤처진 경우 운영 기준은 `origin/main`으로 판단하고, 로컬 `main` 갱신은 별도 작업으로 분리한다.
- PR이 열려 있으면 CI, review, mergeability, base branch가 `main`인지 확인한다.
- GitHub CLI token이 만료되었거나 GitHub 플러그인이 private repo에 접근하지 못하면 병합을 자동 진행하지 않는다. 사용자에게 인증 복구를 요청하거나 GitHub UI 병합 절차를 명시한다.

배포 전 로컬/CI 검증:
1. `./scripts/check-risk-gates.sh`
2. `cd backend/BE-02 && ./gradlew test --no-daemon`
3. `cd frontend/FE-02 && npm ci && npm run test:mappers && npm run build`
4. 필요 시 `BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh`로 현재 운영 기준선을 먼저 저장한다.

AWS Lightsail 운영 정보:
- Production URL: `https://growgate.54.180.56.24.sslip.io`
- Static IP: `growgate-static-ip` / `54.180.56.24`
- Current instance: `growgate-8gb-20260706`
- Rollback instance: `growgate-fresh-20260703-2134`
- Snapshot: `growgate-pre-8gb-20260706-1611`

무중단에 가까운 배포 절차:
1. 운영 서버 접속 전, 현재 운영 스모크가 통과하는지 확인한다.
2. 서버에서 현재 배포 commit, service 상태, 여유 메모리, 디스크 사용량을 확인한다.
3. 기존 jar/dist를 timestamp가 포함된 백업 경로에 보관한다.
4. 가능하면 로컬/CI에서 생성한 jar/dist artifact를 서버로 전송한다. 운영 서버에서 full Gradle/Vite build를 반복하지 않는다.
5. 서버에서 새 artifact를 별도 release directory에 배치한다.
6. Spring backend는 systemd service를 짧게 restart한다. 프론트는 Nginx 정적 root symlink 또는 atomic directory swap으로 반영한다.
7. `sudo systemctl status growgate-backend --no-pager`와 `sudo nginx -t`를 확인한다.
8. `sudo systemctl reload nginx`를 실행한다.
9. `/health`, `/api/diagnosis` 401, `/public-data/daily-refresh` 200을 확인한다.
10. 전체 운영 smoke를 실행한다.

배포 후 필수 검증:
```bash
curl -s -o /dev/null -w '%{http_code} %{time_total}\n' https://growgate.54.180.56.24.sslip.io/health
curl -s -o /dev/null -w '%{http_code} %{time_total}\n' https://growgate.54.180.56.24.sslip.io/public-data/daily-refresh
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh
```

검증 수용 기준:
- `/health`는 `200`.
- 인증 없는 `/api/diagnosis`는 `401`.
- signup/login은 성공하고 `gg_access_token`은 `HttpOnly` cookie로 설정된다.
- cookie-only `/api/users/me`가 `200`.
- diagnosis run은 `done`.
- S04-S07 stage card가 존재한다.
- Markdown report API가 presentation-ready payload를 반환한다.
- public-data dashboard가 응답한다.
- `sourceTrace`, `evidenceRetrieval`, `evidenceGuardrails`, `llmFallbackReasons`, `gateDecision`, `claimVerifications`가 내부 데이터가 있을 때 유지된다.
- `gateDecision.llmInfluencedGate=false`가 유지된다.

롤백 트리거:
- `/health` 비정상.
- 로그인 또는 cookie session restore 실패.
- diagnosis run 생성 또는 완료 실패.
- S04-S07 stage card 누락.
- report API 실패.
- public-data endpoint 5xx 또는 fallback 폭증.
- Nginx 502/504, backend crash loop, OOM, DB 연결 실패.
- 보안 헤더 또는 HTTPS redirect가 사라짐.

동일 인스턴스 rollback:
1. 백업해 둔 이전 jar/dist로 symlink 또는 file path를 되돌린다.
2. `sudo systemctl restart growgate-backend`
3. `sudo nginx -t && sudo systemctl reload nginx`
4. 운영 smoke 재실행.

인스턴스 단위 rollback:
```bash
aws lightsail attach-static-ip \
  --profile jiyongrock \
  --region ap-northeast-2 \
  --static-ip-name growgate-static-ip \
  --instance-name growgate-fresh-20260703-2134
```

롤백 후 확인:
```bash
curl -s -o /dev/null -w '%{http_code} %{time_total}\n' \
  https://growgate.54.180.56.24.sslip.io/health
BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh
```

결과 기록 형식:
- 기준 commit SHA
- 배포 대상 instance
- 배포 전 smoke 결과
- 배포 후 smoke 결과
- public-data live/cache/fallback count
- diagnosisId, collectionRunId
- rollback instance 보존 여부
- 실패/잔여 위험
- 실행하지 못한 검증과 이유

현재 2026-07-11 점검 메모:
- `SECURITY_AUDIT_REPORT.md`는 현재 작업트리에서 발견되지 않았다.
- 로컬 HEAD `520676a1f15c07aba291305f65f83bb62b7a0d58`는 로컬 추적 정보상 `origin/main`과 같다.
- 로컬 `main`은 `fca1b8a7`에 있어 `origin/main`보다 뒤처져 있다.
- `gh auth status`는 token invalid로 실패했지만, 배포 대상 commit이 이미 `origin/main`과 같아 GitHub 병합 작업은 필요 없었다.
- GitHub 플러그인은 private repo 접근에 404를 반환했다.
- `aws login --profile jiyongrock`로 AWS 세션을 복구했다.
- 현재 작업자 IP `211.217.156.140/32`를 Lightsail SSH allowlist에 임시 추가하고, 배포 후 제거했다.
- 운영 스모크는 `2026-07-11 10:47 KST`에 통과했다. 결과: `diagnosisId=a4ebddb0-daf2-474a-9cc8-0e57b5360d76`, `collectionRunId=6635f773-4c2c-4b3f-8597-69db1d552ddc`, `liveCount=3`, `cacheCount=24`, `fallbackCount=1`, `operationEndpointLiveCount=14`, `apiKeyConfiguredLiveCount=26`.
- 운영 artifact 배포는 `2026-07-11 11:08 KST`에 수행했다.
- 서버 `DEPLOY_COMMIT`는 `520676a1f15c07aba291305f65f83bb62b7a0d58`로 갱신됐다.
- 이전 배포 marker는 `75c8968c`였고, 이전 jar와 frontend root는 `/opt/growgate-releases/20260711-110641/` 아래에 보존했다.
- 배포 후 운영 스모크는 `2026-07-11 11:10 KST`에 통과했다. 결과: `diagnosisId=9a387892-f61b-4c27-b12c-a1f3b5439be3`, `collectionRunId=fe39445a-2c03-41bd-bd53-bae95ee89232`, `liveCount=0`, `cacheCount=27`, `fallbackCount=1`, `operationEndpointLiveCount=14`, `apiKeyConfiguredLiveCount=26`.
