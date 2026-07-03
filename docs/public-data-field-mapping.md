# Public Data Field Mapping And Score Contribution

이 문서는 발표장에서 "어떤 공공데이터 필드가 점수에 들어갑니까?" 질문에 답하기 위한 구현 기준표다. 실제 live API key나 operation endpoint가 없으면 record는 `fallback`으로 저장되고 positive contribution을 만들 수 없다.

## Source Type Policy

| sourceType | 의미 | 점수 기여 |
| --- | --- | --- |
| `live` | 승인된 operation endpoint에서 새로 수집한 공공 API record | metric 기반 기여 가능 |
| `cache` | 동일 key의 유효한 기존 record 재사용 | 기존 confidence로 제한 기여 가능 |
| `fallback` | key/endpoint/네트워크/파싱 실패 또는 catalog page 감지 | positive contribution 금지 |
| `crawl` | allowlist 공개 웹 페이지 보조 수집 | 단독 의사결정 금지, 보조 confidence |
| `external_feed` | allowlist RSS/Atom/JSON 기반 수출 뉴스·연구소 보고서 feed | 최대 +3점, 공식 API 대체 금지 |
| `seed` | 개발/시연 초기 데이터 | live/cache 공공데이터로 해석 금지 |
| `heuristic` | 로컬 rule engine | 공공데이터로 표시 금지 |
| `user_input` | 담당자 입력/제품 지식 | 입력 근거로만 표시 |
| `agent` | workflow/LLM 보조 trace | 점수 산출 주체 아님 |

## Provider Mapping

| Provider | Agency type | usedFor | Key metrics in `normalizedPayload` | Score use |
| --- | --- | --- | --- | --- |
| `KCS_TRADE` | `OTHER_PUBLIC` | `market` | `importAmountUsd`, `exportAmountUsd`, `yoyGrowthPct`, `recordCount` | export/import amount와 growth가 높을수록 market contribution 증가 |
| `KOTRA_MARKET` | `MOTIE_AFFILIATED` | `market` | `marketArticleCount`, `countryMentions`, `productMentions`, `yoyGrowthPct`, `recordCount` | target country/product article과 growth가 확인되면 market confidence 보강 |
| `SAFETY_RECALL` | `OTHER_PUBLIC` | `recall` | `recallCount`, `highRiskCount`, `recordCount` | recall/highRisk count가 높을수록 recall contribution 하락 |
| `KIPRIS_FTO` | `OTHER_PUBLIC` | `fto` | `patentCount`, `claimsCount`, `productMentions`, `recordCount` | patent/product mention이 많을수록 FTO 검토 필요 contribution으로 보수화 |
| `BIZINFO_SUPPORT` | `MOTIE_AFFILIATED` | `support` | `supportProgramCount`, `openProgramSignalCount`, `exportKeywordCount`, `recordCount` | 관련 지원사업과 모집/수출 키워드가 있으면 support contribution 증가 |
| `MOTIE_WEB_CRAWL` | `MOTIE_AFFILIATED` 또는 allowlist 기관 | `market/regulation/support` | `pageCount`, `keywordHits`, `policyMentions`, `masked` | API live 대체가 아니라 보조 source trace로만 사용 |
| `EXPORT_EVIDENCE_FEED` | `EXTERNAL_EVIDENCE` | `market` | `documentCount`, `recentDocumentCount`, `targetCountryMentions`, `productMentions`, `exportKeywordHits` | 뉴스/기사/연구소 보고서 최신성 보조. 공식 공공 API score 증거로 말하지 않음 |

## Provider Fixture And Regression Tests

아래 fixture는 `backend/BE-02/src/test/resources/publicdata/`에 있고, `ProviderSchemaNormalizationTest`에서 실제 parser 산출물을 검증한다.

| Fixture | 검증 목적 |
| --- | --- |
| `kotra-market-sample-live-response.json` | KOTRA market article, country/product mention, growth field mapping |
| `safety-recall-sample-live-response.json` | Safety recall/high-risk count parsing and negative contribution |
| `kipris-fto-sample-live-response.json` | KIPRIS patent count, claim count, product mention parsing |
| `bizinfo-support-sample-live-response.json` | 기업마당 지원사업, 모집/수출 keyword count parsing |
| `export-evidence-feed-sample.xml` | RSS/Atom feed item, date, export keyword parsing |

## Score Contribution Ledger

진단 결과의 source entry는 아래 필드를 가진다.

| Field | 설명 |
| --- | --- |
| `sourceId` | source trace table에서 식별 가능한 provider/run 기반 id |
| `provider` | `KCS_TRADE`, `KOTRA_MARKET` 등 실제 provider 또는 `RULE_ENGINE` |
| `sourceType` | `live/cache/fallback/crawl/external_feed/seed/heuristic/user_input/agent` |
| `usedFor` | `market/regulation/recall/fto/support` |
| `metricSummary` | 점수에 쓰인 핵심 metric 요약 |
| `operationEndpoint` | connector가 catalog/application page가 아니라 실제 operation endpoint를 호출했다는 evidence |
| `apiKeyConfigured` | 인증키 기반 API 호출 설정이 있었는지에 대한 evidence. 값은 키 원문이 아니라 boolean만 저장한다. |
| `endpointType` | `operation`, `catalog`, `application`, `portal` 등 endpoint 분류 |
| `rawScoreContribution` | connector가 계산한 원점 기여도 |
| `confidence` | provider/parser confidence |
| `scoreContribution` | confidence와 fallback 정책을 반영한 최종 기여도 |
| `decisionImpact` | 화면에 보여줄 사람이 읽는 영향 설명 |

계산 원칙:

- fallback source는 positive contribution을 만들 수 없다. `PublicDataCollectionService.guardScoreContribution()`와 `PublicDataCollectionServiceTest.collectForDiagnosisClampsFallbackPositiveContributionBeforePersisting()`가 저장 단계에서 보장한다.

```text
storedRawContribution = fallback ? min(rawContribution, 0) : rawContribution
adjustedContribution = fallback ? min(storedRawContribution, -1) - 2 : round(rawContribution * confidence)
dimensionScore = clamp(baseRuleScore + sum(adjustedContribution))
fallbackCount > 0 or lowCoverage => confidencePanel.uncertaintyReasons 추가
```

## Presentation Checks

- `scripts/smoke-prod.sh`는 `publicData.successCount`와 `fallbackCount`를 출력한다.
- `SMOKE_REQUIRE_LIVE_PUBLIC_DATA=true`이면 live success가 1건 미만일 때 실패한다.
- `/api/public-data/health`는 현재 사용자/회사 scope에서 `motieRelatedLiveOrCacheCount`, `motieVerifiedOperationLiveCount`, `motieRelatedScoreContribution`, `motieLiveEvidenceReady`를 반환한다.
- 산업부 계열 live 증거는 `sourceType=live/cache`, `provider.motieRelated=true`, `collectionEvidence.operationEndpoint=true`인 record만 verified operation evidence로 계산한다.
- 발표장에서 "실제 operation API 증거"로 말하려면 `sourceType=live/cache`, `operationEndpoint=true`, `apiKeyConfigured=true`, `collectionRunId`, `fetchedAt/retrievedAt`가 함께 있어야 한다.
- `scripts/presentation-public-data-proof.sh`는 `operationEndpointLiveCount`, `apiKeyConfiguredLiveCount`, `officialOperationApiEvidenceCount`를 산출한다.
- S10 Sources 화면에서 `live`, `cache`, `fallback`, `crawl`, `heuristic` badge가 구분되어야 한다.
- Source Trust Bar는 `lastCollectedAt`, `sourcePeriod`, `providerHealth`, `liveSuccessRate`를 첫 화면에 표시해야 한다.
- S10 Sources 화면은 raw metric -> normalized metric -> raw contribution -> confidence -> adjusted contribution -> final score delta waterfall을 보여야 한다.
- `fallback`과 `heuristic`은 "공공데이터 연결됨"으로 표시하면 안 된다.

## Regional Industry And Buyer Request Trace

진단 결과는 `regionalIndustryProfile`과 `buyerRequestTrace`를 포함한다.

| Field | 의미 |
| --- | --- |
| `regionalIndustryProfile.region` | 사용자가 입력한 지역. 미입력 시 `지역 미입력`으로 남기고 contribution을 낮춘다. |
| `regionalIndustryProfile.leadingIndustry` | 지역/업종/HS code deterministic matching으로 산출한 지역 주력산업 후보 |
| `regionalIndustryProfile.supportOrgs` | 지역 테크노파크, 지방 KOTRA 지원단, 지자체 지원기관 후보 |
| `regionalIndustryProfile.scoreContribution` | support Gate에 더해지는 지역 산업 매칭 contribution |
| `buyerRequestTrace.impacts` | 바이어 문의 키워드가 regulation/recall/fto/support 중 어느 판단 축에 연결됐는지 |

이 로직은 live 공공 API를 대체하지 않는다. 지역지원사업 API가 연결되면 해당 record는 별도 `live/cache/fallback` source로 추가하고, 현재 deterministic matching은 `sourceType=heuristic` 또는 `user_input`으로 표시한다.
