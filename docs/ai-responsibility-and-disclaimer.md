# AI Responsibility and Disclaimer

Checked at: 2026-07-04

GrowGate uses a deterministic rule-based Gate. LLM or Agent output is advisory only and must not override `finalGate`. API responses and reports must keep `llmInfluencedGate=false` for the final Gate decision.

## Decision Ownership

- Final Gate owner: deterministic rule scoring plus risk and fallback conditions.
- LLM role: report wording, source-bound explanation, trace summary, and candidate risk phrasing.
- Human role: final verification before buyer reply, quote, shipment, certification decision, support application, or FTO decision.
- Expert role: certification experts, customs/trade experts, patent attorneys, or legal professionals review regulated or legal conclusions.

## Required Output Separation

Reports and result screens should separate these blocks:

### 근거 데이터

- Provider:
- sourceType: live/cache/fallback/seed/empty
- 조회일:
- 원문 링크:
- rawCount:
- normalizedCount:
- license/terms status:
- usageWarning:

### 정규화/필터링 결과

- matchedKey:
- metricSummary:
- scoreFormula:
- confidence:
- fallbackUsed:

### AI 보조 해석

- 요약:
- 리스크 후보:
- 확인 필요 사항:

### 사람이 최종 확인해야 하는 항목

- 원문 데이터 최신성
- 인증/리콜/지원사업 신청 가능 여부
- FTO 후보 항목
- 개인정보/영업비밀 포함 여부

## Standard Disclaimer

본 내용은 자동 생성된 의사결정 보조 정보이며, 법률/인증/수출 실행 여부의 최종 판단이 아닙니다. 공공데이터 원문 근거, 정규화/필터링 결과, AI 보조 해석, 사람이 최종 확인해야 하는 항목을 분리해 확인하십시오.

## FTO Disclaimer

본 결과는 공개 특허·상표 데이터와 입력 정보를 기반으로 한 사전 탐색 결과입니다. 특허 침해 여부, 실시 가능성, 권리 유효성에 대한 법률 판단이 아니며, 최종 의사결정 전 변리사 또는 법률 전문가 검토가 필요합니다.

## Public Data Disclaimer

`live`와 `cache`만 공공데이터 수집 proof로 설명할 수 있습니다. `fallback`, `seed`, `empty`, `crawl`, `external_feed`, `heuristic`, `agent`는 공식 operation API live 성공으로 표시하지 않습니다. 이용조건이 미확인인 provider는 원문 링크를 제공하고 요약/재가공/재배포 제한 가능성이 있음을 표시합니다.

## Privacy and Trade Secret Notice

데모 환경에는 주민등록번호, 연락처, 계약서, 도면, 원가, 거래처명, 미공개 인증문서 등 개인정보 또는 영업비밀에 해당할 수 있는 정보를 입력하지 마십시오. 실제 개인정보가 포함될 경우 별도 동의와 처리방침이 필요합니다.

## Logging and Retention Policy

- API key, token, 개인정보, 영업비밀은 로그에 남기지 않는다.
- Public data request parameters must be masked when they include keys or tokens.
- Demo data is synthetic and must be described as `synthetic_demo_not_real_customer`.
- Demo inputs should not be retained long-term unless retention scope, deletion path, and consent are documented.
