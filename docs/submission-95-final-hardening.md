# GrowGate Local EX 95점 방어 보강 노트

작성일: 2026-07-01

## 한 줄 포지셔닝

GrowGate Local EX는 지역 수출 담당자가 제품 단위로 시장성, 규제, 리콜, FTO, 지원사업 준비도를 빠르게 점검하고, 공공데이터 출처와 담당자 지식을 순환 학습시키는 수출 준비도 진단 서비스다.

## 이번 8대 감점 요인 조치

| 평가 지적 | 조치 | 코드/문서 기준 |
| --- | --- | --- |
| live 공공 API가 기본 경로가 아님 | `PUBLIC_DATA_ENABLED` 기본값을 `true`로 전환. key/operation endpoint 부재 시 `fallback`으로 투명 기록 | `backend/BE-02/src/main/resources/application.yml` |
| 공공 API 결과가 도메인 필드로 파싱되지 않음 | normalized payload에 HS/국가/제품명 매칭, record count, numeric token, evidence strength 저장 | `AbstractPublicDataConnector` |
| 로컬 규칙 출처가 실제 기관 출처처럼 보임 | 로컬 산식 출처 provider를 `RULE_ENGINE`, sourceType을 `heuristic`으로 분리 | `DiagnosisAsyncProcessor` |
| S04~S07 상세 카드가 mock 기반 | S04 시장, S05 규제, S06 리콜, S07 FTO/지원 stageCards를 백엔드에서 생성하고 프론트 매핑 | `DiagnosisAsyncProcessor`, `diagnosisService.js` |
| ML UI 표기가 실제 구현과 다름 | S03을 `RULE SCORING LAB`으로 변경하고 rule rubric + LLM assisted로 표기 | `S03PredLab.jsx` |
| workflow loop 과장 위험 | `AgentLoop`로 고정 plan/tool execution/verification/retry/fallback/complete_task trace를 생성 | `DiagnosisAsyncProcessor` |
| retrieval 과장 위험 | LLM은 점수 근거가 아니라 DB 지식/공공데이터 context 기반 보조 설명 및 지식화 후보 생성으로 제한 | `EvidenceContextBuilder`, `AgentInferenceService` |
| 점수 산식이 임의로 보임 | `scoringRubric`에 weight, evidence, guardrail을 결과 JSON으로 저장 | `DiagnosisAsyncProcessor` |

## 기획서에서 밀 강점

1. **공공데이터를 실패까지 추적하는 구조**
   - live 호출 성공만 보여주는 데모가 아니라 `live`, `cache`, `fallback`, `heuristic`, `agent`를 분리한다.
   - API key 미발급, operation endpoint 미확정, 네트워크 실패가 발생해도 결과 JSON에 사유가 남는다.

2. **제품 단위 수출 준비도 산식**
   - 종합점수는 시장성 25%, 규제 22%, 리콜 16%, FTO 20%, 지원 17%로 분리된다.
   - 각 점수는 source trace와 연결되어 설명 가능한 진단으로 방어할 수 있다.

3. **담당자 지식 루프**
   - 담당자가 등록한 자료, 정책, 판단 근거, accepted loop item이 다음 추론 context에 다시 들어간다.
   - 공공데이터로 비어 있는 부분은 담당자 문서 지식화로 보강할 수 있다.

4. **LLM 운영 안정성**
   - Gemini를 1차 추론 provider로, OpenRouter를 백업 provider로 사용한다.
   - 키는 환경변수로만 주입하고, 오류 메시지와 fallback reason은 secret sanitizer로 마스킹한다.

5. **정직한 기술 범위**
   - 현재 제출 표현은 실제 라벨 기반 ML, 운영형 벡터 검색, LLM 동적 도구 발견형 자율 시스템이라고 주장하지 않는다.
   - 대신 공공 API 수집, source trace, 규칙 산식, bounded LLM reasoning, 지식 순환이라는 구현된 강점을 전면에 둔다.

## 발표/서류 표현 가이드

사용 가능한 표현:

- “공공 API live/cache/fallback을 sourceType으로 구분해 진단 근거를 남긴다.”
- “담당자 지식과 공공데이터 context를 LLM 보조 추론에 연결한다.”
- “점수 산식과 source trace가 함께 저장되어 왜 이 점수가 나왔는지 설명 가능하다.”
- “외부 LLM 실패 시 Spring 규칙 진단으로 degrade되는 provider fallback 구조다.”

피해야 할 표현:

- “실제 라벨 기반 ML 모델이 운영 중이다.”
- “운영형 벡터 검색이 완성됐다.”
- “LLM이 자율적으로 모든 도구를 계획하고 재시도한다.”
- “API key 없이도 항상 live 공공데이터가 수집된다.”

## 남은 현실적 리스크

| 리스크 | 현재 방어 | 후속 보완 |
| --- | --- | --- |
| 일부 공공데이터는 활용신청 후 operation endpoint 필요 | fallback record로 명시 | 실제 발급 URL과 key 확보 후 smoke test |
| 실제 성과 라벨 기반 ML 부재 | rule rubric으로 투명화 | 진단/수출 결과 라벨 축적 후 ML 전환 |
| 운영형 벡터 검색 부재 | DB context injection과 지식화 루프 | 별도 검색 인프라와 citation score 도입 |
| workflow 고도화 | Spring `AgentLoop`는 고정 도구 계획과 retry/fallback을 수행 | 사용자 승인형 action과 checkpoint/resume 추가 |
