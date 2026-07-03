# 방어 가능한 제출 표현 정리

작성일: 2026-07-01

## 표현 원칙

심사/발표에서는 구현보다 앞선 표현을 쓰지 않는다. 현재 제출 서버가 방어할 수 있는 범위는 “공공데이터 provider별 수집 상태 추적”, “numeric signal 파싱”, “confidence-adjusted rule scoring”, “top-k 근거 검색”, “agent trace 기반 계획-도구-검증-재시도 기록”이다.

| 위험 표현 | 제출용 표현 | 방어 근거 |
| --- | --- | --- |
| 실시간 공공데이터 기반 AI 진단 | live/cache/fallback 상태가 구분되는 공공데이터 수집 근거 기반 준비도 진단 | sourceType, provider, collectedAt, fallbackReason, confidence를 화면과 API에 노출 |
| 관세청/KOTRA/KIPRIS 데이터를 완전 분석해 예측 | 관세청/KOTRA/KIPRIS 응답에서 파싱 가능한 numeric signal을 점수 산식에 반영 | `PublicDataSignal.metrics`, `scoreContribution`, provider별 normalizedPayload |
| 공공데이터 기반 자동 의사결정 | 담당자 검토를 돕는 근거 기반 실행 우선순위 제안 | 결과는 decision이 아니라 readiness score, risk flag, roadmap task로 표시 |
| 5종 공공데이터 API 완전 연동 | 5개 provider의 operation endpoint, cache, fallback 상태를 구분해 수집 | disabled/fallback 상태를 “연결됨”으로 표시하지 않고 sourceType badge로 분리 |
| 대규모 문서 검색 기반 성과 확률 산출 | DB 지식과 공공데이터 record의 BM25-lite top-k 근거 검색 및 규칙 기반 준비도 추정 | pgvector 기반 대규모 검색/성과 확률 산출은 후속 과제로 분리하고 메인 UI에서 주장하지 않음 |

## 발표 시 한 문장

“GrowGate는 공공데이터가 실제로 수집된 경우와 fallback/heuristic인 경우를 명확히 나누고, 수집 record에서 뽑은 metric과 담당자 제품 지식을 근거로 수출 준비도와 다음 실행 우선순위를 제안하는 서비스입니다.”
