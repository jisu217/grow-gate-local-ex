# Judge Defense 90-Point Evidence Guide

GROW-GATE는 "AI가 수출 성공률을 예측하는 서비스"가 아니라, 지역 제조기업이 바이어 견적 전 놓치기 쉬운 공공데이터·제품지식·인증·FTO 확인 항목을 Gate와 업무 보드로 바꾸는 상담 보조 서비스다.

## 90-Point Ready Gates

| Gate | 확인 방법 | 실패 시 발표 표현 |
| --- | --- | --- |
| E2E | `scripts/smoke-prod.sh`: signup/login/product/diagnosis/polling/sources/report | 시연 경로 수정 전 제출 금지 |
| Live data | `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true`에서 live success >= 1 | "실시간 공공데이터" 표현 금지 |
| Mock isolation | `VITE_APP_MODE=presentation`, `VITE_ENABLE_MOCK=false`, risk gate 통과 | mock route는 데모 전용으로 설명 |
| Score trace | S10 source table과 `/api/diagnosis/{id}/sources` | 공공데이터 점수 반영 주장 축소 |
| AI wording | `scripts/check-risk-gates.sh` 금지 표현 grep | 자율 AI/예측/RAG 표현 삭제 |
| Responsibility | Confidence panel, HOLD/전문가 검토 badge | 법적 확정 판단 표현 금지 |

## Expected Questions

| 질문 | 답변 |
| --- | --- |
| 산업부 공공데이터를 실제로 썼습니까? | `BIZINFO_SUPPORT`, `KOTRA_MARKET`, optional `MOTIE_WEB_CRAWL`을 산업부/산하기관 성격으로 분리하고, live/cache/fallback/crawl sourceType을 저장한다. key/operation endpoint가 없으면 fallback으로 숨기지 않는다. |
| 관세청 데이터의 어떤 필드가 시장성 점수에 들어갑니까? | `importAmountUsd`, `exportAmountUsd`, `yoyGrowthPct`, `recordCount`를 normalized metric으로 파싱하고 confidence-adjusted contribution을 market score에 더한다. |
| 공공데이터가 실패하면 점수가 올라갑니까? | 아니다. fallback source는 positive contribution을 만들 수 없고 confidence panel의 uncertainty와 HOLD 판단 쪽으로만 작용한다. |
| S04~S07 화면이 mock입니까? | 아니다. backend `stageCards.s04~s07`이 없으면 프론트는 "데이터 없음/source 미수집" empty state를 보여주고 mock 숫자로 채우지 않는다. |
| XGBoost나 RAG를 구현했습니까? | 메인 제출 앱은 XGBoost/대규모 RAG를 주장하지 않는다. 현재는 source-traced rule scoring과 담당자 승인형 self-loop wiki, source-bound LLM 보조 설명 구조다. |
| Agent가 자율 판단합니까? | 아니다. Spring `AgentLoop`는 plan/toolCall/observation/verifier/retry/fallback trace를 남기는 workflow 구조이고, Gate score는 규칙 기반이다. |
| 법적 FTO 판단입니까? | 아니다. FTO/인증/리콜은 견적 전 1차 체크이며 전문가 확인 필요 action item으로 전환한다. |
| 발표장 인터넷이 느리면 어떻게 합니까? | smoke script가 live 요구 여부를 명확히 분리한다. live 증거가 없으면 실시간 연동 표현을 낮추고 cache/fallback source trace와 보수적 Gate를 보여준다. |

## Demo Order

1. `scripts/smoke-prod.sh` log에서 auth, product, diagnosis, polling, sources, report 통과 확인
2. 제품 저장 후 진단 생성 화면에서 snapshot input 확인
3. 결과 상단 Gate decision과 confidence panel 확인
4. S04~S07 backend stageCards 확인
5. S10 source table에서 provider, sourceType, metricSummary, contribution 확인
6. action board에서 견적 전/출하 전 담당자 업무 확인

## Hard No

다음 표현은 발표/README/UI에 쓰지 않는다.

- "AI가 수출 성공 가능성을 예측"
- "XGBoost 예측"
- "대규모 RAG 기반 자동 의사결정"
- "자율 AI Agent가 판단"
- "FTO 문제 없음"
- "공공데이터 완전 분석"
