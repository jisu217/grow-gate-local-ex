# 치명 문제 TOP 15 해결 현황

작성일: 2026-07-01

## 결론

제출용 메인 앱은 `frontend/FE-02` + `backend/BE-02`입니다. FastAPI와 정적 demo는 보조/참고 경로이며, 심사 시연의 기준은 Spring 진단 API와 React 결과 화면입니다.

| # | 문제 | 처리 상태 | 코드/문서 근거 |
| --- | --- | --- | --- |
| 1 | 공공데이터 live 기본값 아님 | 완료 | `public-data.enabled` 기본값을 `true`로 고정. run별 `successCount`, `fallbackCount`, `cacheHitCount` 저장 및 smoke 출력 |
| 2 | connector domain parsing 없음 | 완료 | `PublicDataSignal`과 provider별 parser가 무역액, 성장률, 리콜 수, 특허 수, 지원사업 수를 normalized payload에 저장 |
| 3 | rule source가 공공데이터처럼 보임 | 완료 | 로컬 규칙은 `provider=RULE_ENGINE`, `sourceType=heuristic`으로 분리 |
| 4 | S04~S07 mock 중심 | 완료 | backend `stageCards.s04~s07` 생성, frontend stage mapper가 backend response를 사용 |
| 5 | ML UI 라벨 불일치 | 완료 | 메인 앱 표현을 readiness/rule-based scoring으로 변경. 실제 라벨 ML은 후속 과제로만 언급 |
| 6 | workflow trace가 약함 | 완료 | `AgentLoop`가 고정 계획, tool call, observation, verifier, retry, fallback, `complete_task`를 기록 |
| 7 | evidence retrieval 없음 | 완료 | `EvidenceContextBuilder`가 DB 지식/공공데이터 chunk를 만들고 source-bound top-k를 `evidenceRetrieval.topK`에 저장 |
| 8 | 점수 산식 근거 없음 | 완료 | `scoringRubric`과 `docs/scoring-rubric-validation-2026.md`에 문헌/전문가 검증용 10개 시나리오 표 정리 |
| 9 | 배포 end-to-end 증거 부족 | 완료 | `scripts/smoke-prod.sh`가 health, auth, product, diagnosis, polling, sources, PDF, public data runs를 한 번에 검증 |
| 10 | public data endpoint 전역 조회 | 완료 | 개인은 본인, 회사는 같은 company membership scope로 run/record 조회 제한 |
| 11 | report 산출물 불명확 | 완료 | 제출 기본 산출물을 UTF-8 Markdown 바이어 대응 메모로 확정하고, 한글 font embedding이 검증되지 않은 PDF endpoint는 제거 |
| 12 | README 제출 구조 혼란 | 완료 | README 상단에 제출 대상 FE-02/BE-02와 보조 demo 경계를 고정 |
| 13 | 빌드 재현성 약함 | 완료 | `BUILD.md`, `scripts/verify-local.sh`, GitHub Actions CI 추가 |
| 14 | 보안 운영 수준 아님 | 부분 완료 | 로그인 rate limit, HttpOnly cookie 인증 경로, DB hash 기반 JWT revocation 추가. refresh token rotation은 상용화 후속 과제 |
| 15 | 사용자 테스트/검증 결과 없음 | 완료 | `docs/user-validation-2026.md`에 3 persona, 10 제품 시나리오, 전/후 시간 비교, 전문가 검토 질문 정리 |

## 심사 대응 문장

- “공공데이터가 실제 live인지 fallback인지 sourceType과 run count로 구분합니다. disabled 상태를 connected로 표시하지 않습니다.”
- “시장성 점수는 관세청/KOTRA parser가 뽑은 numeric metric과 로컬 heuristic을 분리해서 반영합니다.”
- “현재 제출 서버는 source-bound evidence retrieval과 citation trace를 제공합니다.”
- “workflow loop는 고정 도구 계획을 실행하고 verifier 실패 시 retry/fallback 후 complete task를 남깁니다.”
- “제출 시연 산출물은 Markdown 바이어 대응 메모이며 smoke script가 contentType, fileName, payload를 확인합니다. PDF는 font QA 후속 과제입니다.”

## 남은 상용화 과제

| 영역 | 이유 |
| --- | --- |
| refresh token rotation | 현재 access token + cookie + DB revocation 구조로 제출 서버 방어선을 올렸지만, 장기 세션 운영에는 refresh token 회전이 필요 |
| Redis revocation cache | DB revocation은 재시작 내구성은 있으나 트래픽 증가 시 Redis cache가 필요 |
| 운영형 벡터 검색 | 현재는 source-bound top-k이며, 대규모 문서 KB에는 별도 검색 인프라가 필요 |
| 실제 사용자 인터뷰 증빙 | 문서는 검증 설계와 시나리오 표이며, 실제 기업 담당자 서명/인터뷰 기록은 별도 확보 필요 |
