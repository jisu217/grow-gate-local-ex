---
synthetic_data: true
created_at: 2026-07-03
---

# GROW-GATE 합성 데이터 거버넌스 정책

## 핵심 원칙
이 패키지의 기업·매출·문서·바이어·진단 결과는 모두 합성 데이터입니다. 심사·시연·테스트·초기 ML 벤치마크 목적으로만 사용하고, 실제 고객 계약 또는 live 공공데이터 성공 기록으로 표시하지 않습니다.

## 화면 표시 원칙
- `sourceType=seed` 또는 `synthetic=true`를 유지합니다.
- synthetic record를 `live`, `cache`, `apiKeyConfigured=true`, `operationEndpoint=true`로 바꾸지 않습니다.
- 발표자료에는 “실제 고객 데이터”가 아니라 “실제 업무 흐름을 모사한 합성 PoC 데이터”로 표시합니다.

## 실제 고객 검증 전환
실제 고객 검증은 인터뷰 동의서, 익명화된 사용 로그, 담당자 피드백, 견적 전후 소요시간 비교표로 별도 확보해야 합니다.
