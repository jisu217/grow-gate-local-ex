# Codex Prompt 12 — one-command demo와 제출 ZIP 정리

목표:
백엔드 실행 재현성, legacy/mock 흔적 제거, 데모 계정/데이터 고정을 완료한다.

작업:
1. `docker-compose.demo.yml`, `.env.demo.example`, `scripts/run-demo.sh` 추가.
2. `scripts/package-submission.sh`가 다음을 제외하도록 수정: artifacts/legacy-backups, node_modules, .env, local logs, old demo-only fixtures.
3. `SUBMISSION_SCOPE.md` 생성: 최종 코드 범위, seed 데이터 설명, synthetic warning, 실행 방법.
4. `scripts/check-submission-zip.sh` 작성:
   - unzip -l 결과에서 legacy/mock/backup/.env/node_modules 있으면 실패
   - SYNTHETIC_DATA_POLICY 존재 확인
   - FE build/test command 안내
5. 최종 smoke path 문서화: login → demo scenario → diagnosis result → S10 proof → report download.

수용 조건:
- 새 환경에서 one-command demo가 DB schema/seed까지 적용한다.
- 제출 ZIP 검사 통과.
- 심사장 인터넷/API key 없는 상태에서도 seed 시나리오는 볼 수 있고, live proof는 “미연동/재수집 필요”로 정직하게 표시된다.
