# Codex Prompt 09 — 첫 사용자 UX: 데모 시나리오 즉시 로드

목표:
심사장 3분 데모에서 제품/지식/FTO/인증 입력 과정을 생략하고 3개 완성 시나리오를 바로 실행/조회한다.

작업:
1. `DiagnosisNew.jsx`에 `LoadDemoScenarioButton` 또는 카드 3개를 추가한다.
2. 카드 내용: 창원 배터리, 부산 마린 액추에이터, 대구 메디센서.
3. 클릭 시 seed productId와 targetCountry를 채운다.
4. 이미 완료된 seed diagnosis가 있으면 “완료 결과 보기”, 새로 진단하면 “새 진단 실행”을 분리한다.
5. synthetic warning toast를 표시한다.

수용 조건:
- 로그인 후 2클릭 이내에 S00~S10 결과가 보인다.
- demo data는 live proof로 오인되지 않는다.
- fallback/live 상태별 CTA가 표시된다.
