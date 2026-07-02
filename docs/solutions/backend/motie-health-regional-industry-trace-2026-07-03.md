# MOTIE Health And Regional Industry Trace - 2026-07-03

## Context

심사 질문은 두 가지로 압축된다.

- 산업부 또는 산하기관 live 공공데이터가 실제로 몇 건 들어왔고 점수에 몇 점 기여했는가.
- GrowGate가 지역 제조기업 서비스라는 코드 근거가 있는가.

## Guidance

- fake live seed를 만들지 않는다. live 증거는 `sourceType=live/cache`, `provider.motieRelated=true`, `collectionEvidence.operationEndpoint=true`인 record만 verified로 센다.
- `/api/public-data/health`를 발표 방어용 canonical endpoint로 사용한다.
- 진단 입력은 `region`, `industry`, `buyerRequest`를 받는다.
- `regionalIndustryProfile`은 지역/업종/HS code로 주력산업과 지원기관 후보를 매칭하고 support Gate contribution으로 반영한다.
- `buyerRequestTrace`는 바이어 문의 키워드를 regulation/recall/fto/support 판단 축에 연결한다.

## Why This Matters

MOTIE-related record 비율과 지역산업 trace가 없으면 서비스가 단순 수출 체크리스트처럼 보인다. Health endpoint와 trace JSON을 두면 심사위원이 "몇 건/몇 점/어느 지역/어느 요구사항"을 물었을 때 코드와 화면으로 답할 수 있다.

## When To Apply

- 공공데이터 공모전에서 특정 부처/산하기관 데이터 활용 비중을 증명해야 할 때
- 지역 산업단지/지자체/테크노파크 지원사업과 제품 진단을 연결해야 할 때
- 바이어 문의 기반 서비스라고 설명하면서 실제 판단 trace가 약할 때
