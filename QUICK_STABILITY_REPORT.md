# GROW-GATE Local EX Quick Stability Report

작성일: 2026-07-06

## 현재 상태

현재 main 배포본은 공모전 시연의 핵심 흐름인 홈 진입, Gate 판단 생성, 이력 상태 갱신, 진단 결과 확인, S04-S10 근거 화면, 공공데이터 proof, 리포트 다운로드 위험 게이트까지 통과한 상태다. 운영 smoke 기준 새 진단은 `running`에서 `done`까지 약 6초 안에 전이되었고, 공공데이터는 cache/fallback 상태를 포함해 투명하게 표시된다. 단, 모든 공공데이터 provider가 실시간 운영 연동 완료 상태는 아니므로 일부 provider는 "운영 키 등록 예정", "추가 구축 예정", "수집 안정화 대상"으로 설명해야 안전하다.

## 실행한 명령

- `git status --short`
- `rg --files`
- `rg "TODO|FIXME|mock|seed|fallback|synthetic|dummy|sample|임시|가짜|테스트용"`
- `rg "onClick|button|Link|navigate|fetch|axios|api|diagnosis|report|public-data|agent"`
- `rg "app.agent.enabled|app.fastapi.enabled|timeout|retry|fallback|CORS|JWT|SECRET|PASSWORD"`
- `npm run test:mappers`
- `npm run build`
- `./gradlew test`
- `python3 -m compileall grow_gate_local_ex_demo ml data scripts`
- `scripts/check-risk-gates.sh`
- `BASE_URL=https://growgate.54.180.56.24.sslip.io ./scripts/smoke-prod.sh`

## 고친 P0/P1 목록

- P0: Gate 판단 이력에서 `분석 중`으로 오래 고착되는 문제를 상태 저장 분리와 timeout/partial 상태 처리로 완화했다.
- P0: 같은 제품, HS Code, 대상국, 바이어 요구 조합의 중복 생성은 짧은 시간 내 기존 active 진단을 재사용하도록 방어했다.
- P1: 외부 공공데이터 live 수집이 진단 완료를 막지 않도록 진단 경로는 최신 cache/snapshot 중심으로 빠르게 완료되게 했다.
- P1: 프론트 polling은 초반 빠르게 확인하고 이후 지연 상태를 사람이 이해하는 문구로 표시하도록 정리했다.
- P1: public-data proof 화면의 실패처럼 보이는 raw 문구와 내부 점수 컬럼을 사람이 읽는 상태/판단 영향 중심으로 낮췄다.

## 아직 남은 위험

- 일부 공공데이터 provider는 운영 키, operation endpoint, 제품군 매핑이 필요하다. 시연에서는 "추가 구축 예정" 또는 "운영 키 등록 예정"으로 설명한다.
- 운영 smoke에서 `publicData.fallbackCount=1`이 남아 있다. 이는 실패를 성공으로 표시하지 말고 fallback-aware diagnosis로 설명한다.
- FastAPI/LLM 연계는 최종 Gate 결정자가 아니다. 최종 Gate는 rule score와 risk/fallback 조건으로 결정된다고 설명한다.
- `python3 -m compileall`은 프로젝트 하위 가상환경까지 포함해 출력이 많았다. syntax error는 없었지만 다음부터는 venv 제외 옵션을 두는 편이 낫다.
- 로컬에는 untracked QA artifact가 남아 있다. 제출 패키지에는 의도한 산출물만 포함한다.

## 시연 시 안전한 클릭 순서

1. 홈 화면에서 서비스 흐름을 짧게 소개한다.
2. 로그인 또는 준비된 데모 진입 경로로 들어간다.
3. `수출 Gate 판단 이력`에서 `+ 새 Gate 판단`을 누른다.
4. 준비된 제품명, HS Code, 대상국, 바이어 문의를 입력한다.
5. 이력 카드가 즉시 생성되는 것을 보여주고 `완료` 상태까지 기다린다.
6. 완료 카드에서 진단 상세로 이동한다.
7. 00 진단 개요, 04 시장성, 05 규제·인증, 06 리콜 위험, 07 FTO·지원 순서로 본다.
8. 08 실행 보드에서 담당자 할 일을 보여준다.
9. 10 근거 데이터에서 source trace와 공공데이터 상태를 보여준다.
10. 09 리포트에서 다운로드 또는 리포트 내용을 확인한다.
11. 시간이 남으면 `/public-data/daily-refresh`에서 현재 반영 데이터와 추가 구축 예정 데이터를 분리해 보여준다.

## 실패할 수 있는 화면과 회피 방법

- 일부 provider 상세가 `운영 키 등록 예정` 또는 `추가 구축 예정`으로 표시될 수 있다. "현재 진단에 직접 반영한 데이터와 후속 구축 대상을 분리해 관리한다"고 설명한다.
- 진단이 30초 이상 지연되면 같은 버튼을 반복 클릭하지 않는다. 이력에서 최신 완료 샘플을 열고 "외부 API 응답 지연 시 partial/timeout 상태로 분리된다"고 설명한다.
- public-data live 호출 결과가 0건이면 "조건 일치 데이터 없음"이며 수집 실패가 아니라고 설명한다.
- FastAPI/Agent 연결 화면을 보여줄 때는 "LLM assisted report" 또는 "bounded workflow agent loop"라고 말하고, 최종 판단은 rule-based Gate라고 설명한다.

## 이미 구현이라고 말해도 되는 것

- Rule-based Gate 판단
- 진단 생성, 상태 조회, 이력, 상세 결과 흐름
- S04-S07 시장성, 규제·인증, 리콜, FTO·지원 근거 표시
- S08 실행 보드
- S09 리포트와 다운로드 오류 처리
- S10 source trace, evidence guardrails, public-data proof
- fallback-aware diagnosis와 source quality status 표시
- 운영 smoke와 risk gate 검증

## 추가 구축 예정이라고 말해야 안전한 것

- 모든 공공데이터 provider의 100% 실시간 운영 연동
- 일부 운영 API 키 등록과 provider별 operation endpoint 안정화
- 제품군, 품목명, 인증/리콜 유형의 고도화 매핑
- LLM/Agent 기반 문장 보강의 운영 자동화 확대
- 장기 운영용 관리자 repair UI와 상세 모니터링 대시보드
