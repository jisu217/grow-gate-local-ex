# 제출 준비도 점검 및 보완 내역

작성일: 2026-06-30
범위: GROW-GATE Local EX 메인 앱, 공개 제출 문서, 배포/GitHub 반영 상태
제외: 대외비 공모전 전략, 심사 대응 내부 판단, 비공개 API key/credential

## 1. 총평

현재 메인 제출 앱은 단순 데모 화면이 아니라 React FE-02와 Spring Boot BE-02를 중심으로 진단 생성, 결과 조회, 공공데이터 출처 trace, 리포트 다운로드, 권한/감사 로그를 연결한 서비스형 구조로 보강됐다.

서류 단계에서 가장 먼저 확인될 수 있는 항목은 다음 네 가지다.

| 점검 축 | 현재 상태 | 확인 근거 |
| --- | --- | --- |
| 문제 정의 | 지역 제조기업의 수출 규제·인증·리콜·FTO 준비 부담을 제품/HS Code 기준 진단 문제로 정의 | 메인 홈 hero copy, 진단 생성 화면 |
| 공공데이터 활용 | 관세청, KOTRA, KATS/Safety Korea, KIPRIS, 지원사업 provider connector와 live/cache/fallback 저장 구조 반영 | `publicdata` backend package, `public_data_records` schema |
| 서비스 구현 | React에서 진단 생성, polling, 결과 상세, source trace, report payload 흐름 연결 | FE-02 diagnosis pages/services |
| 운영 가능성 | JWT secret 검증, token revocation, 권한 검사, audit pagination, soft delete, retry/backoff 반영 | BE-02 auth/security/knowledge/product/http client 변경 |

## 2. 메인 화면 점검 및 수정

### 보완 전 리스크

- 첫 화면이 "AI 진단"과 "3분" 메시지 중심이라 서류 검토자가 공공데이터 활용성, 운영 구현 수준, 제출 산출물을 즉시 파악하기 어려웠다.
- "연동 공공데이터 API 7종" 표현은 실제 live/cache connector 수와 다르게 보일 수 있어 과장으로 읽힐 수 있었다.
- 기능 카드의 이모지 아이콘은 기존 스타일에는 맞지만 제출용 서비스의 신뢰감 측면에서는 다소 가벼워 보일 수 있었다.

### 보완 사항

- 홈 hero 제목을 `지역 제조기업의 수출 막힘을 근거로 풉니다.`로 수정해 문제-해결 연결을 명확히 했다.
- hero 하단에 `공공데이터`, `서비스 구현`, `운영 근거`, `제출 산출물` 4개 readiness block을 추가했다.
- 통계 문구를 실제 구현 범위에 맞춰 `5종 live/cache 공공데이터 provider`, `6축 점수`, `출처 trace 포함 진단 결과`, `90일 실행 로드맵`으로 조정했다.
- 기능 카드 아이콘을 숫자형 배지로 바꿔 제출용 톤을 정돈했다.
- 모바일에서는 readiness block이 1열로 쌓이도록 반응형 CSS를 추가했다.

## 3. 공개 문서 정리

| 문서 | 목적 | 공개 가능 여부 |
| --- | --- | --- |
| `docs/submission-update-report.md` | 변경사항, 업데이트, 추가 보완사항 정리 | 가능 |
| `docs/evaluation-resolution-status.md` | 첨부 평가서 기준 완료/미완료 항목 구분 | 가능 |
| `docs/submission-readiness-audit.md` | 제출 전면 점검 및 공개 가능한 보완 결과 | 가능 |
| `docs/superpowers/specs/2026-06-30-live-public-data-collection-design.md` | live 공공데이터 수집 설계 | 가능 |
| `docs/superpowers/plans/2026-06-30-live-public-data-collection.md` | live 공공데이터 구현 계획 | 가능 |

명시적인 공모전 전략 문서는 새로 만들지 않았고, 기존 공개 문서에도 평가자 공략 방식이나 비공개 전략을 넣지 않았다.

## 4. 남은 리스크

| 리스크 | 상태 | 대응 |
| --- | --- | --- |
| 실제 개인 API key | 미포함 | 보안상 repository에 넣지 않고 `DATAGO_SERVICE_KEY`, `KIPRIS_PLUS_KEY` 등 env로 주입 |
| KOTRA/Safety/KIPRIS operation endpoint | 일부 승인 필요 | 공식 카탈로그 URL은 설정했으며, 활용신청 후 실제 요청 URL로 `*_API_URL` 교체 |
| AWS 배포 credential | 환경 의존 | 기본 AWS credential이 없으면 배포 불가. 성공/실패 결과를 최종 상태에 기록 |
| GitHub 인증 | 환경 의존 | GitHub CLI token이 만료되어 있으면 push 불가. git remote push 가능 여부를 별도 확인 |
| 브라우저 스크린샷 | 환경 의존 | macOS sandbox에서 Chromium 실행 제한이 있어 HTTP 응답/빌드 검증으로 대체 가능 |

## 5. 검증 체크리스트

| 항목 | 결과 |
| --- | --- |
| React production build | 통과: `npm run build`, `npm run build -- --base=/grow_gate_local_ex/` |
| Spring Boot test | 통과: `./gradlew test` with Java 17 |
| `git diff --check` | 통과 |
| 로컬 메인 HTML 응답 | `http://127.0.0.1:5173/` 응답 확인 |
| 공개 문서 내 대외비 전략 포함 여부 | 포함하지 않음 |

## 6. 배포 및 GitHub 반영 상태

| 항목 | 상태 | 비고 |
| --- | --- | --- |
| 소스 브랜치 push | 완료 | `origin/codex/user-scoped-db-signup` |
| 정적 배포 브랜치 push | 완료 | `origin/gh-pages`에 FE-02 `dist` 산출물 배포 |
| GitHub Pages URL | 설정 확인 필요 | `https://jiyongkim615.github.io/grow_gate_local_ex/` 확인 시 404. Pages source가 `gh-pages`로 활성화되지 않았거나 전파 전일 수 있음 |
| AWS S3 배포 | 인증 대기 | 기본 AWS credential 없음, `kbds-happyloop-invoke-agent` profile도 session expired. `aws login` 브라우저 인증이 필요 |
