# Codex Prompt 03 — 지역산업 프로필 하드코딩 제거

목표:
`DiagnosisAsyncProcessor.buildRegionalIndustryProfile`의 문자열 룰을 `regional_industry_profiles` 테이블/JSON dataset 기반으로 대체한다.

작업:
1. `06_schema_migrations/2026-07-03_agent_ml_regional_schema.sql`와 `2026-07-03_seed_regional_and_registry.sql`를 migration/init에 반영한다.
2. `RegionalIndustryProfile` Entity, Repository, Service를 `backend/BE-02/src/main/java/com/growgate/regional`에 추가한다.
3. `DiagnosisAsyncProcessor`에서 region/industry/hsCode/productName으로 service 조회 후 profile을 구성한다.
4. UI S01 또는 S10에 `public_source_name`, `source_period`, `refresh_cycle_hours`, `synthetic` 값을 표시한다.
5. synthetic profile인 경우 “공식 지역산업 공공데이터로 교체 필요” warning을 표시한다.

수용 조건:
- 기존 하드코딩 support org list가 service/dataset으로 이동한다.
- 지역/업종이 없는 경우 controlled fallback과 missingInputs가 표시된다.
- 테스트: 경남/배터리, 부산/조선기자재, 대구/디지털헬스 3개 케이스가 기대 support org를 반환한다.
