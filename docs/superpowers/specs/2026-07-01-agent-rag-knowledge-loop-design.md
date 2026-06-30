# Agent RAG Knowledge Loop Design

작성일: 2026-07-01

## 목표

Gemini Flash 계열 API를 1차 추론 agent로, OpenRouter API를 백업 추론 provider로 사용해 진단 안정성을 높인다. 동시에 담당자가 마이페이지에서 작성한 제품 지식, 정책, 출처, 진단 판단 근거가 다음 추론에 다시 쓰이는 지식화 루프를 만든다.

## 설계 원칙

- LLM은 점수의 단일 근거가 아니라 Spring 규칙 진단을 보강하는 추론 계층이다.
- provider 장애, quota, timeout이 있어도 진단은 기존 Spring 규칙 기반으로 완료된다.
- prompt와 raw API 응답 전체를 DB에 저장하지 않는다.
- 지식화는 자동 확정하지 않고 담당자 승인 후 KB에 반영한다.
- 운영 모델명과 endpoint는 환경변수로 바꿀 수 있어야 한다.

## Agent Provider

`AgentInferenceService`가 단일 진입점이 된다.

1. Gemini provider 호출
2. Gemini 실패 시 OpenRouter provider 호출
3. 둘 다 실패하면 빈 결과와 fallback trace 반환

환경변수:

- `APP_AGENT_ENABLED`
- `APP_AGENT_GEMINI_ENABLED`
- `APP_AGENT_GEMINI_API_KEY`
- `APP_AGENT_GEMINI_MODEL`
- `APP_AGENT_OPENROUTER_ENABLED`
- `APP_AGENT_OPENROUTER_API_KEY`
- `APP_AGENT_OPENROUTER_MODEL`

## RAG Context

현재 pgvector 수준의 embedding retrieval은 도입하지 않는다. 대신 제출 안정성을 위해 DB 기반 context injection RAG를 구성한다.

Context 구성 요소:

- 제품 기본 정보: 이름, HS Code, 대상국, 설명
- 제품 KB: trust grade, source type, notes
- 인증서: 이름, 만료일, 갱신 주기
- FTO: 대상국, 위험도, 요약
- 바이어 요구사항
- 담당자 등록 출처
- live 공공 API/cache/fallback 결과
- 이전 진단에서 담당자가 승인한 지식화 항목

## Knowledge Loop

진단 실행 후 agent와 Spring 규칙 결과에서 `knowledgeLoopSuggestions`를 만든다.

후보 유형:

- `risk_evidence`: 반복 등장 위험과 근거
- `policy_note`: 담당자가 정책/운영 기준으로 보관하면 좋은 내용
- `source_gap`: 출처가 부족한 영역
- `action_template`: 30/60/90일 실행계획 템플릿
- `compliance_watch`: 인증/리콜/FTO 모니터링 포인트

상태:

- `pending`: 담당자 검토 대기
- `accepted`: 지식화 완료
- `dismissed`: 제외

담당자가 `accepted`를 선택하면 기존 `knowledge_sources`에 출처형 지식으로 승격하고, 다음 진단의 context에 포함한다.

## UI

마이페이지 제품 지식 상세 화면에 `AI 지식화` 탭을 추가한다.

표시 항목:

- 지식화 추천 제목
- 추천 유형
- 판단 근거
- 다음 진단에 주는 효과
- 상태

액션:

- `지식화`: KB 출처로 승격
- `제외`: 다음 추천 목록에서 숨김

## 테스트

- Gemini 실패 시 OpenRouter provider가 호출된다.
- OpenRouter도 실패하면 Spring fallback trace로 끝난다.
- RAG context가 제품 지식/출처/공공데이터를 포함한다.
- 진단 결과에 지식화 후보가 생성된다.
- 담당자가 후보를 지식화하면 `knowledge_sources`에 반영된다.
