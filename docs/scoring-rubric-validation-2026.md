# Scoring Rubric Validation 2026

작성일: 2026-07-01

## 산식 기준

```text
overall = market*0.25 + regulation*0.22 + recall*0.16 + fto*0.20 + support*0.17
```

| 축 | 가중치 | 실무 근거 |
| --- | --- | --- |
| market | 25% | 수출 의사결정의 1차 관문. HS code/대상국 무역 신호와 KOTRA 시장 신호를 우선 반영 |
| regulation | 22% | 인증·시험 누락은 선적 지연과 반송으로 이어지므로 시장성 다음 비중 |
| fto | 20% | 특허/FTO는 판매 금지·분쟁 비용으로 이어지는 고위험 항목 |
| support | 17% | 비용·일정 실행 가능성을 보강하지만, 시장/규제/FTO보다 후순위 |
| recall | 16% | 리콜·안전 이력은 제품군 위험 보정 항목으로 반영 |

## 10개 시나리오 검증표

| # | 제품 | HS | 국가 | 기대 위험 | 기대 점수대 | 검증 포인트 |
| --- | --- | --- | --- | --- | --- | --- |
| 1 | 리튬이온 배터리팩 | 850760 | DE | 운송·배터리 인증 | 58-72 | KCS market, Safety recall, battery rule가 모두 trace에 남아야 함 |
| 2 | IoT 제어 모듈 | 853710 | US | FCC/UL/FTO | 60-76 | 인증 보유 시 regulation 상승, FTO 미등록 시 medium risk |
| 3 | 기능성 화장품 | 330499 | JP | 성분/표시 규제 | 55-70 | buyer requirement와 knowledge source가 support를 보정 |
| 4 | 산업용 유량 센서 | 902610 | VN | 시장/지원사업 | 65-80 | KOTRA/Bizinfo fallback 여부가 sourceType으로 분리 |
| 5 | 친환경 포장재 | 392390 | EU | 포장재 규제 | 58-74 | regulation risk와 knowledgeLoop suggestion 생성 |
| 6 | 의료용 소모품 | 901890 | US | FDA/리콜 | 45-65 | 인증 지식 없음, recall source fallback 시 낮은 confidence |
| 7 | 전기 자전거 배터리 | 850760 | FR | 배터리·안전 | 50-68 | battery HS prefix rule과 public metric contribution 분리 |
| 8 | 스마트 조명 컨트롤러 | 853650 | SG | EMC/FTO | 62-78 | FTO low 등록 시 fto 점수 상승 |
| 9 | 식품 포장 필름 | 392010 | JP | 식품접촉 규제 | 52-68 | 규제 준비도와 buyer requirement 누락 risk 확인 |
| 10 | 산업용 로봇 부품 | 847990 | DE | CE/기계류 | 60-75 | certification expiry와 market signal의 가중 합산 확인 |

## 검증 운영 방식

1. 각 시나리오를 제품으로 등록하고 진단을 생성한다.
2. `evidenceRetrieval.topK`, `sources`, `scoringRubric`, `stageCards.s04~s07`을 저장한다.
3. 전문가가 “위험 수준 방향성”과 “실행 로드맵의 업무 지시 가능성”을 5점 척도로 평가한다.
4. 점수가 기대 범위를 벗어나면 해당 dimension contribution만 조정하고 sourceType trace는 유지한다.

## 발표 시 주의 문장

- “현재 산식은 통계 학습 모델이 아니라 수출 실무 checklist를 점수화한 제출용 rubric입니다.”
- “정답 점수 예측이 아니라 위험 우선순위와 실행 순서를 안정적으로 정렬하는 것이 목적입니다.”
- “label 기반 XGBoost는 학습 데이터가 확보된 뒤 교체 가능한 후속 모듈입니다.”
