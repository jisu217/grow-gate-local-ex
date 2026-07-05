# Public Data License and Usage Matrix

Checked at: 2026-07-04

This is not legal advice. It records only what the project can point to from official provider pages as of the checked date. If an item could not be confirmed from an official page, it is marked `미확인` and the product should show original links instead of redistributing or over-summarizing provider content.

## Provider Matrix

| Provider | 사용 데이터 | 공식 출처 URL | operation API 여부 | API key 필요 여부 | 이용조건 확인 여부 | 저장 가능 여부 | 재가공 가능 여부 | 재배포 가능 여부 | 출처표시 필요 여부 | 제한사항 | 현재 코드 반영 상태 | 남은 리스크 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| KOTRA | 글로벌 공급망 인사이트, 시장/국가별 실행 신호 | https://www.data.go.kr/data/15122648/openapi.do | 예. 공공데이터포털 REST Open API | 예. 활용신청/개발·운영계정 트래픽 구조 | 확인: 데이터셋에 `이용허락범위 제한 없음` 표시 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 미확인. 데이터셋에는 제한 없음만 표시 | operation endpoint 승인 전 catalog URL을 live API로 표시 금지 | `PublicDataProvider.KOTRA_MARKET`와 `public_data_sources.json`에 license/terms/redistribution/storage/reprocessing metadata 추가 | 실제 operation endpoint 문서와 응답 schema는 승인 후 재확인 필요 |
| SafetyKorea / KATS | 제품 안전인증 및 국내외 리콜 정보 | https://www.data.go.kr/data/15116894/openapi.do, https://www.safetykorea.kr/release/openapi | 예. 공공데이터포털은 API 유형 `LINK`, SafetyKorea는 KC인증/리콜 Open API 안내 | 미확인. 공식 페이지는 LINK/API 안내이며 현재 Spring 코드는 key 환경변수를 요구 | 확인: 데이터셋에 `이용허락범위 제한 없음` 표시 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 미확인. 데이터셋에는 제한 없음만 표시 | 인증/리콜 최종 판단으로 표현 금지. 원문 링크와 조회일 유지 | `PublicDataProvider.SAFETY_RECALL`와 `public_data_sources.json`에 metadata 추가. sourceType live/cache/fallback 분리 유지 | 실제 operation별 파라미터, 인증키 필요 여부, 트래픽 정책 확인 필요 |
| KATS 국내리콜정보 | 국내 리콜 제품 상세 현황. 인증번호, 모델명, 제품명, 브랜드명, 리콜사업자명, 제조사명, 위해유형, 등록일 등 | https://www.data.go.kr/data/15040696/fileData.do, https://infuser.odcloud.kr/oas/docs?namespace=15040696/v1 | 예. 파일데이터 자동변환 Open API. 확인한 최신 operation `https://api.odcloud.kr/api/15040696/v1/uddi:f13d1404-717d-4862-871d-b249899ea58f` | 예. 인증키 필수. 인증키 미포함 호출 시 401과 `인증키는 필수 항목 입니다.` 응답 확인 | 확인: 데이터셋에 `이용허락범위 제한 없음` 표시 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 미확인. 데이터셋에는 제한 없음만 표시 | 원문이 1회성 파일데이터이며 포털이 최신 정보는 별도 `제품 안전인증 및 리콜 정보` API 활용을 안내한다. 리콜 여부 최종 확인/인증 적합성/수출 가능성 판단으로 표현 금지 | `scripts/check_public_data_providers.py`와 `public_data_sources.json`에 not-runtime-connected 검증 후보로 추가. 실제 키는 저장 금지 | 최신성 요구가 있으면 SafetyKorea/제품 안전인증 및 리콜 정보 operation API와 비교 필요. 사업자명/제조사명 표시 범위 점검 필요 |
| KATS 국외리콜공표기관 | 해외 제품 리콜 공표기관명. 예: 미국 CPSC, 일본 경제산업성, 호주 ACCC, 독일 BAUA 등 | https://www.data.go.kr/data/15118082/fileData.do, https://infuser.odcloud.kr/oas/docs?namespace=15118082/v1 | 부분 확인. 파일데이터 자동변환 Open API이며 Base URL `api.odcloud.kr/api`와 Swagger URL 확인. 실제 resource path는 Swagger에서 추가 확인 필요 | 예. Open API 활용신청 및 인증키 필요. Encoding/Decoding 키 적용 방식은 호출 조건에 따라 다를 수 있음 | 확인: 데이터셋에 `이용허락범위 제한 없음` 표시 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 미확인. 데이터셋에는 제한 없음만 표시 | 전체 15행의 공표기관 참고 목록이다. 리콜 발생 사실, 인증 적합성, 수출 가능성 판단 근거로 표현 금지 | `scripts/check_public_data_providers.py`와 `public_data_sources.json`에 not-runtime-connected 검증 후보로 추가. 실제 키는 저장 금지 | Swagger resource path, 응답 schema, 실제 호출 가능한 Encoding/Decoding 키 형식 확인 필요 |
| KIPRIS Plus | 특허/상표 공개자료 기반 FTO 사전 위험 신호 | https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300024 | 예. KIPRIS Plus는 BULK와 Open API 제공 | 예. 약관상 Open API 사용 회원은 Authentication Key 획득 필요 | 부분 확인: Terms of use 확인. 재배포 허용 여부는 미확인 | 조건부. 승인 회원 서비스/유료 상품 조건을 따라야 함 | 조건부. 서비스 신청 목적과 정책 범위 내 사용 | 미확인 | 미확인 | 정보 목적이며 오류/누락 가능. FTO 법률 판단, 침해 없음, 실시 가능 표현 금지 | `PublicDataProvider.KIPRIS_FTO`에 conditional/unknown metadata와 FTO usageWarning 추가. S07/report에 변리사 검토 고지 추가 | 실제 상품별 KIPRIS Plus 라이선스/요금/재배포 제한 확인 필요 |
| Bizinfo / 기업마당 | 지원사업 공고, 수출지원 매칭 후보 | https://www.bizinfo.go.kr/web/index.do, https://www.bizinfo.go.kr/biz/bizd/selectBIZD120View.do | 미확인. 현재 코드는 `BIZINFO_API_URL` operation endpoint를 받을 수 있으나 공식 API 문서는 아직 확인 안 됨 | 미확인. 현재 코드는 key 환경변수를 받을 수 있음 | 확인: 기업마당 저작권 정책/이용약관 확인. Open API별 이용허락은 미확인 | 조건부. 사전 협의/허락 또는 적법한 절차 필요 가능성 | 조건부. 무단 변경 금지 조항 존재 | 조건부. 무단 복제/배포 금지 및 출처 표시 요구 가능성 | 예. 허락을 받아 게재하는 경우 출처가 기업마당임을 밝혀야 함 | 직접 수익 목적 이용 금지, 무단 변경/복제/배포 금지 가능성 | `PublicDataProvider.BIZINFO_SUPPORT`와 `public_data_sources.json`에 conditional metadata 추가. 화면에 usageWarning 표시 | 공식 정책정보 개방 API 문서, 공고별 저작권/재배포 조건 확인 필요 |
| KOMSA MTIS | MTIS authStatus 키 상태 확인 | https://mtisopenapi.komsa.or.kr/uat/api/authStatus.do | 부분. authStatus operation endpoint는 확인했으나 진단 runtime connector는 미연결 | 예. `serviceKey` 필요 | 미확인. 공식 이용조건/재배포 조건 문서는 아직 확인 안 됨 | 미확인 | 미확인 | 미확인 | 미확인 | authStatus는 키 상태 확인용으로만 사용. 데이터 원문/재가공/재배포 가능 여부로 해석 금지 | `scripts/check_public_data_providers.py`에 선택 검증 추가. `.env.example.presentation`에 `MTIS_SERVICE_KEY` placeholder 추가. 실제 키는 저장 금지 | 공식 API 명세, 데이터셋 목록, 이용조건, 응답 schema 확인 후 runtime connector 추가 여부 결정 |
| 국토교통부 물류창고업등록정보 | 물류창고업 등록 목록/상세. 창고번호, 업체명, 대표자, 위치, 보관품목, 종업원수, 시설장비 등 | https://www.data.go.kr/data/3048029/openapi.do | 예. REST Open API. 요청주소 `http://apis.data.go.kr/1611000/whsinfoview2/WhsInfoList` | 예. 필수 요청변수 `ServiceKey` | 확인: 공공데이터포털 데이터셋에 `이용허락범위 제한 없음` 표시 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 가능으로 기록. 근거: 데이터셋 이용허락범위 제한 없음 | 미확인. 데이터셋에는 제한 없음만 표시 | 업체명, 대표자, 위치 등 사업자 식별 가능 정보가 포함되므로 원문 링크/조회일/sourceType을 유지하고 개인정보·영업비밀로 오해될 입력과 결합하지 않음 | `scripts/check_public_data_providers.py`에 선택 검증 추가. `.env.example.presentation`에 `MOLIT_WAREHOUSE_API_KEY` placeholder 추가. 진단 runtime scoring에는 미연결 | 실제 운영 연결 전 상세조회 operation, 응답 schema, 화면 표시 범위, 사업자 정보 노출 최소화 정책 확인 필요 |

## Official Source Notes

- 공공데이터포털 정책은 제3자 권리가 포함된 공공데이터는 권리자의 이용허락을 확보해야 하며, 저작물이 포함된 데이터에는 이용허락 범위를 표시해야 한다고 설명한다. 데이터셋별 조건이 우선이다.
- KOTRA 데이터셋은 REST, JSON+XML, 활용신청/트래픽, 이용허락범위 제한 없음으로 확인했다.
- SafetyKorea 데이터셋은 LINK, JSON+XML, SafetyKorea Open API URL, 이용허락범위 제한 없음으로 확인했다.
- KATS 국내리콜정보는 공공데이터포털에서 파일데이터와 자동변환 Open API를 제공하며, JSON/XML, 전체 881행, 무료, 수정일 2025-09-05, 이용허락범위 제한 없음으로 확인했다. 포털 설명상 최신 정보는 별도 제품 안전인증 및 리콜 정보 API 확인이 필요하다.
- KATS 국외리콜공표기관 데이터셋은 파일데이터와 자동변환 Open API를 제공하며, JSON/XML, 전체 15행, 무료, 수시 1회성 데이터, 이용허락범위 제한 없음으로 확인했다. Base URL과 Swagger URL은 확인했지만 실제 operation resource path는 추가 확인이 필요하다.
- KIPRIS Plus 약관은 Open API 사용 회원의 Authentication Key 획득, 서비스 정책 준수, 정보 목적/오류 가능성을 확인했다. 재배포 가능 여부는 확인하지 못했다.
- 기업마당 저작권 정책은 직접 수익 목적 이용 금지, 사전 협의/허락, 출처 표시, 무단 변경/복제/배포 제한 가능성을 확인했다.
- KOMSA MTIS authStatus URL과 서비스키 필요성은 운영 검증 대상으로만 기록했다. 공식 이용조건은 아직 확인하지 못했다.
- 국토교통부 물류창고업등록정보는 공공데이터포털에서 REST, JSON+XML, 무료, 실시간, 자동승인, `ServiceKey` 필수, `이용허락범위 제한 없음`, 목록조회 요청주소를 확인했다. 현재 프로젝트 진단 runtime에는 연결하지 않았다.

## Runtime Handling Rule

- `live`: actual provider operation endpoint call result.
- `cache`: previously collected provider result.
- `fallback`: skipped or failed collection because endpoint/key/schema/network is missing.
- `seed`: synthetic demo data only, not public-data proof.
- `empty`: operation call succeeded but returned no usable records.

When the usage condition is unclear, UI/report text must say: `원문 링크 제공, 요약/재가공 제한 가능성 있음`.
