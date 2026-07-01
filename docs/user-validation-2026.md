# User Validation 2026

## Validation Design

| Persona | Scenario | Baseline Method | GrowGate Method | Expected Evidence |
| --- | --- | --- | --- | --- |
| Regional export officer | Battery pack to Germany | Manual HS search, certification checklist, support notice search | Product diagnosis with KCS/KOTRA/Safety/KIPRIS/Bizinfo source trace | Time saved, missing evidence list |
| SME quality manager | Cosmetics to Japan | Spreadsheet checklist and consultant email | Regulation/recall/FTO/support score review | Risk item correction before submission |
| Startup founder | IoT controller to US | Web search and fragmented notes | Diagnosis report + knowledge loop acceptance | Reusable policy note and source record |

## Product Scenarios

| # | Product | HS Code | Target | Main Question |
| --- | --- | --- | --- | --- |
| 1 | 리튬이온 배터리팩 | 850760 | DE | EU battery/safety readiness |
| 2 | IoT 제어 모듈 | 853710 | US | FCC/UL/FTO readiness |
| 3 | 기능성 화장품 | 330499 | JP | ingredient/regulation readiness |
| 4 | 산업용 센서 | 902610 | VN | market/support fit |
| 5 | 친환경 포장재 | 392390 | EU | regulation and recall risk |

## Measurement Sheet

| Metric | Baseline | GrowGate Target |
| --- | --- | --- |
| Initial evidence gathering time | 2-4 hours | 20-40 minutes |
| Source trace completeness | Manual citation | live/cache/fallback/heuristic sourceType |
| Rework count before expert review | 2+ cycles | 1 cycle target |
| Missing document visibility | 담당자 기억 의존 | riskItems + knowledgeLoop suggestions |

## Expert Review Questions

1. 점수 산식의 5개 축이 실제 수출 준비도 검토 순서와 맞는가?
2. fallback/heuristic 출처가 공공데이터와 명확히 구분되는가?
3. 30/60/90일 실행 항목이 담당자 업무 지시로 충분한가?
4. 지식화 후보가 다음 진단에 재사용될 가치가 있는가?

