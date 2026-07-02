# Public Data Field Mapping And Score Contribution

이 문서는 발표장에서 "어떤 공공데이터 필드가 점수에 들어갑니까?" 질문에 답하기 위한 구현 기준표다. 실제 live API key나 operation endpoint가 없으면 record는 `fallback`으로 저장되고 positive contribution을 만들 수 없다.

## Source Type Policy

| sourceType | 의미 | 점수 기여 |
| --- | --- | --- |
| `live` | 승인된 operation endpoint에서 새로 수집한 공공 API record | metric 기반 기여 가능 |
| `cache` | 동일 key의 유효한 기존 record 재사용 | 기존 confidence로 제한 기여 가능 |
| `fallback` | key/endpoint/네트워크/파싱 실패 또는 catalog page 감지 | positive contribution 금지 |
| `crawl` | allowlist 공개 웹 페이지 보조 수집 | 단독 의사결정 금지, 보조 confidence |
| `heuristic` | 로컬 rule engine | 공공데이터로 표시 금지 |
| `user_input` | 담당자 입력/제품 지식 | 입력 근거로만 표시 |
| `agent` | workflow/LLM 보조 trace | 점수 산출 주체 아님 |

## Provider Mapping

| Provider | Agency type | usedFor | Key metrics in `normalizedPayload` | Score use |
| --- | --- | --- | --- | --- |
| `KCS_TRADE` | `OTHER_PUBLIC` | `market` | `importAmountUsd`, `exportAmountUsd`, `yoyGrowthPct`, `recordCount` | export/import amount와 growth가 높을수록 market contribution 증가 |
| `KOTRA_MARKET` | `MOTIE_AFFILIATED` | `market` | `marketSignalCount`, `countryMentions`, `recordCount` | target country/market signal이 확인되면 market/support 실행 confidence 보강 |
| `SAFETY_RECALL` | `OTHER_PUBLIC` | `recall` | `recallCount`, `highRiskCount`, `recordCount` | recall/highRisk count가 높을수록 recall contribution 하락 |
| `KIPRIS_FTO` | `OTHER_PUBLIC` | `fto` | `patentCount`, `claimsCount`, `productMentions`, `recordCount` | patent/product mention이 많을수록 FTO 검토 필요 contribution으로 보수화 |
| `BIZINFO_SUPPORT` | `MOTIE_AFFILIATED` | `support` | `programCount`, `deadlineWithin30d`, `matchedKeywordCount` | 관련 지원사업과 마감 여유가 있으면 support contribution 증가 |
| `MOTIE_WEB_CRAWL` | `MOTIE_AFFILIATED` 또는 allowlist 기관 | `market/regulation/support` | `pageCount`, `keywordHits`, `policyMentions`, `masked` | API live 대체가 아니라 보조 source trace로만 사용 |

## Score Contribution Ledger

진단 결과의 source entry는 아래 필드를 가진다.

| Field | 설명 |
| --- | --- |
| `sourceId` | source trace table에서 식별 가능한 provider/run 기반 id |
| `provider` | `KCS_TRADE`, `KOTRA_MARKET` 등 실제 provider 또는 `RULE_ENGINE` |
| `sourceType` | `live/cache/fallback/crawl/heuristic/user_input/agent` |
| `usedFor` | `market/regulation/recall/fto/support` |
| `metricSummary` | 점수에 쓰인 핵심 metric 요약 |
| `rawScoreContribution` | connector가 계산한 원점 기여도 |
| `confidence` | provider/parser confidence |
| `scoreContribution` | confidence와 fallback 정책을 반영한 최종 기여도 |
| `decisionImpact` | 화면에 보여줄 사람이 읽는 영향 설명 |

계산 원칙:

- fallback source는 positive contribution을 만들 수 없다.

```text
adjustedContribution = fallback ? 0 : round(rawContribution * confidence)
dimensionScore = clamp(baseRuleScore + sum(adjustedContribution))
fallbackCount > 0 or lowCoverage => confidencePanel.uncertaintyReasons 추가
```

## Presentation Checks

- `scripts/smoke-prod.sh`는 `publicData.successCount`와 `fallbackCount`를 출력한다.
- `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true`이면 live success가 1건 미만일 때 실패한다.
- S10 Sources 화면에서 `live`, `cache`, `fallback`, `crawl`, `heuristic` badge가 구분되어야 한다.
- `fallback`과 `heuristic`은 "공공데이터 연결됨"으로 표시하면 안 된다.
