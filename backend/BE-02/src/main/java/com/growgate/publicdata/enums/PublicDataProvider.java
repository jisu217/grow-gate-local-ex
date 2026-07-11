package com.growgate.publicdata.enums;

public enum PublicDataProvider {
    KCS_TRADE("관세청", "기획재정부/관세청", "OTHER_GOV", "market", 24,
            "수출입무역통계",
            "https://www.data.go.kr/en/data/15102108/openapi.do",
            true, true,
            "공공데이터포털 개별 데이터셋 이용허락범위 확인 필요",
            "https://www.data.go.kr/ugs/selectPortalPolicyView.do",
            "unknown", "unknown", "unknown",
            "unknown",
            "공식 이용조건 확인 전 원문 링크 중심으로 표시하고 재배포하지 않습니다.",
            "2026-07-04"),
    KCS_TARIFF_LOGISTICS("관세청", "기획재정부/관세청", "OTHER_GOV", "market", 8760,
            "관세청 HS부호·품목번호별 관세율표·항공/해상 운송비용",
            "https://www.customs.go.kr/",
            false, false,
            "공공데이터포털/관세청 파일별 이용조건 확인 필요",
            "https://www.data.go.kr/",
            "unknown", "unknown", "unknown",
            "unknown",
            "사용자 제공 파일을 정규화한 cache reference입니다. HS 설명, 관세율, 운송비 지표를 참고 신호로만 쓰며 법정 관세 확정, 견적 운송비 산정, 통관 가능 판단으로 표현하지 않습니다.",
            "2026-07-05"),
    EXIM_EXCHANGE_RATE("한국수출입은행", "기획재정부/한국수출입은행", "OTHER_GOV", "market", 1,
            "한국수출입은행 환율 정보",
            "https://www.data.go.kr/data/3068846/openapi.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/3068846/openapi.do",
            "yes", "yes", "yes",
            "unknown",
            "환율 참고 신호입니다. 견적 환율, 결제 환율, 환차손익, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    BOK_KEY_STATISTICS("한국은행", "한국은행", "OTHER_GOV", "market", 24,
            "한국은행 ECOS 100대 통계지표",
            "https://ecos.bok.or.kr/api/#/DevGuide/DevSpeciflcation/OA-1010",
            true, true,
            "한국은행 경제통계시스템 API 이용조건 확인 필요",
            "https://ecos.bok.or.kr/api/",
            "unknown", "unknown", "unknown",
            "unknown",
            "거시경제 참고 신호입니다. 특정 품목 수요 예측, 가격 전망, 수출 가능성, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KOTRA_MARKET("KOTRA", "산업통상자원부 산하", "MOTIE_AFFILIATED", "market", 24,
            "대한무역투자진흥공사 해외시장·전시회 정보",
            "https://infuser.odcloud.kr/oas/docs?namespace=15003367/v1",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15122648/openapi.do",
            "yes", "yes", "yes",
            "unknown",
            "출처와 sourceType을 함께 표시하고, operation endpoint/key 증거가 없으면 live 공공 API로 표시하지 않습니다.",
            "2026-07-04"),
    KOTRA_OVERSEAS_EXHIBITIONS("대한무역투자진흥공사", "산업통상자원부 산하", "MOTIE_AFFILIATED", "support", 8760,
            "대한무역투자진흥공사 해외전시회 정보",
            "https://www.data.go.kr/data/15003367/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15003367/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "해외전시회 참가·시장 탐색 참고자료입니다. 참가 가능성, 바이어 확보, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KIAT_RND_OUTCOMES("한국산업기술진흥원", "산업통상자원부 산하", "MOTIE_AFFILIATED", "support", 8760,
            "한국산업기술진흥원_연구개발 성과",
            "https://www.data.go.kr/data/15144951/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15144951/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "연구개발 성과 발생 여부 통계 참고자료입니다. 특정 제품의 기술성, 특허권리성, 논문 성과, 사업화 가능성, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KIAT_TECH_BANK_PATENT("한국산업기술진흥원", "산업통상자원부 산하", "MOTIE_AFFILIATED", "fto", 24,
            "한국산업기술진흥원_기술은행 등록기술 특허 조회 서비스_GW",
            "https://apis.data.go.kr/B552536/tech_3/techpatent",
            true, true,
            "공공데이터포털 개별 API 이용조건 확인 필요",
            "https://www.data.go.kr/ugs/selectPortalPolicyView.do",
            "unknown", "unknown", "unknown",
            "unknown",
            "기술은행 등록기술 특허 조회 참고자료입니다. FTO 법률 판단, 특허 침해 여부, 권리 유효성, 사업화 가능성, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    MOTIE_EXPORT_PREARRANGE("산업통상부", "산업통상자원부", "MOTIE", "market", 1,
            "산업통상부_수출예정정보서비스",
            "https://www.data.go.kr/data/15001810/openapi.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15001810/openapi.do",
            "yes", "yes", "yes",
            "unknown",
            "구매확인서 기반 수출 예정 통계 참고자료입니다. 수출 계약 성사, 특정 기업 실적, 수요 예측, 생산계획, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    MOTIE_DEALING_BUSINESS_INFO("산업통상부", "산업통상자원부", "MOTIE", "market", 1,
            "산업통상부_거래업체정보서비스",
            "https://www.data.go.kr/data/15001808/openapi.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15001808/openapi.do",
            "yes", "yes", "yes",
            "unknown",
            "HS 6단위 거래 현황 참고자료입니다. 특정 업체 신뢰도, 거래 가능성, 수요 예측, 계약 성사, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    MOTIE_TRADE_REMEDY_OCCURRENCE("산업통상부", "산업통상자원부", "MOTIE", "regulation", 8760,
            "산업통상부_무역구제제도 발생현황",
            "https://www.data.go.kr/data/3039967/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/3039967/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "연도별 반덤핑·상계관세·세이프가드 발생 현황 aggregate 참고자료입니다. 특정 품목 제소 가능성, 법률 판단, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    MOTIE_TRADE_REMEDY_FTA_LIST("산업통상부", "산업통상자원부", "MOTIE", "market", 8760,
            "산업통상부_무역구제_자유무역협정 목록자료",
            "https://www.data.go.kr/data/15117950/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15117950/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "자유무역협정 목록 참고자료입니다. 관세율 확정, 원산지 판정, 협정 적용 가능성, 법률 판단, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KSURE_OVERSEAS_BRANCHES("한국무역보험공사", "산업통상자원부 산하", "MOTIE_AFFILIATED", "support", 8760,
            "한국무역보험공사_해외지사 현황",
            "https://www.data.go.kr/data/15044855/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15044855/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "해외지사 위치 참고자료입니다. 무역보험 이용 가능성, 지사 담당 가능성, 금융 지원 승인, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE("한국무역보험공사", "산업통상자원부 산하", "MOTIE_AFFILIATED", "market", 8760,
            "한국무역보험공사_77개국 업종별 연체율",
            "https://www.data.go.kr/data/15104444/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15104444/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "국가·업종별 과거 연체율 참고자료입니다. 바이어 신용도, 개별 거래 리스크, 보험 인수 가능성, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    SAFETY_RECALL("산업통상부 국가기술표준원", "산업통상자원부", "MOTIE", "recall", 24,
            "산업통상부_국가기술표준원_제품안전_국내리콜정보",
            "https://www.safetykorea.kr/openapi/api/recall/recallList.json",
            true, true,
            "제품안전정보센터 Open API 이용조건 확인 필요",
            "https://www.safetykorea.kr/release/openapi",
            "yes", "yes", "yes",
            "unknown",
            "국내리콜정보 원문과 수집 시각을 유지합니다. 제품별 최신 리콜 없음, 안전인증 적합성, 안전성, 수출 실행 판단, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KATS_PRODUCT_SAFETY_HAZARD_INFO("산업통상부 국가기술표준원", "산업통상자원부", "MOTIE", "recall", 8760,
            "산업통상부 국가기술표준원_제품안전_위해정보",
            "https://www.data.go.kr/data/15064255/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15064255/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "제품 위해정보 참고자료입니다. 제품별 최신 위해/리콜 없음, 안전인증 적합성, 안전성, 수출 실행 판단, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KATS_DOMESTIC_RECALL_REPORT("산업통상부 국가기술표준원", "산업통상자원부", "MOTIE", "recall", 8760,
            "산업통상부_국가기술표준원_제품안전_국내리콜 보고서",
            "https://www.safetykorea.kr/openapi/api/recall/recallList.json",
            true, true,
            "제품안전정보센터 Open API 이용조건 확인 필요",
            "https://www.safetykorea.kr/release/openapi",
            "yes", "yes", "yes",
            "unknown",
            "국내리콜 보고서 원문 텍스트 참고자료입니다. 제품별 리콜 존재/부재, 안전인증 적합성, 안전성, 수출 실행 판단, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KATS_CERTIFIED_PRODUCT_INFO("산업통상부 국가기술표준원", "산업통상자원부", "MOTIE", "regulation", 8760,
            "산업통상부_국가기술표준원_제품안전_인증제품 정보",
            "https://www.safetykorea.kr/openapi/api/cert/certificationList.json",
            true, true,
            "제품안전정보센터 Open API 이용조건 확인 필요",
            "https://www.safetykorea.kr/release/openapi",
            "yes", "yes", "yes",
            "unknown",
            "국내 인증제품 참고자료입니다. EU 규제 적합성, 해외 인증 보유, 인증 유효성, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KATS_FOREIGN_RECALL("제품안전정보센터", "산업통상자원부", "MOTIE", "recall", 24,
            "제품안전정보센터_국외리콜정보",
            "https://www.safetykorea.kr/openapi/api/recall/fRecallList.json",
            true, true,
            "제품안전정보센터 Open API 이용조건 확인 필요",
            "https://www.safetykorea.kr/",
            "unknown", "unknown", "unknown",
            "unknown",
            "국외 리콜 참고자료입니다. 특정 제품 최신 리콜 없음, 안전성, 인증 적합성, 수출 실행, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-11"),
    KOREA_LAW_STATUTES("법제처", "법제처", "OTHER_GOV", "regulation", 24,
            "국가법령정보 현행법령 목록",
            "https://www.law.go.kr/DRF/lawSearch.do",
            true, true,
            "국가법령정보센터 OPEN API 이용조건 확인 필요",
            "https://open.law.go.kr/",
            "unknown", "unknown", "unknown",
            "unknown",
            "국내 법령·인증 근거 설명 보강용입니다. EU 규제 대체, 법률 자문, 적합성 확정, 최종 Gate 판단을 수행하지 않습니다.",
            "2026-07-05"),
    CONSUMER_RECALL_SANITATION("소비자24", "공정거래위원회/소비자24", "OTHER_GOV", "recall", 24,
            "소비자24 리콜정보_위생용품",
            "https://www.consumer.go.kr/user/ftc/consumer/openApiSvcUser/120/selectOpenApiSvcUserDetail.do",
            true, true,
            "소비자24 OpenAPI 이용조건 확인 필요",
            "https://www.consumer.go.kr/",
            "unknown", "unknown", "unknown",
            "unknown",
            "위생용품 리콜 참고자료입니다. 최신 리콜 없음, 안전성, 인증 적합성, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    CONSUMER_RECALL_CHEMICAL("소비자24", "공정거래위원회/소비자24", "OTHER_GOV", "recall", 24,
            "소비자24 리콜정보_생활화학제품",
            "https://www.consumer.go.kr/user/ftc/consumer/openApiSvcUser/120/selectOpenApiSvcUserDetail.do",
            true, true,
            "소비자24 OpenAPI 이용조건 확인 필요",
            "https://www.consumer.go.kr/",
            "unknown", "unknown", "unknown",
            "unknown",
            "생활화학제품 리콜 참고자료입니다. 최신 리콜 없음, 안전성, 인증 적합성, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    CONSUMER_RECALL_OVERSEAS("소비자24", "공정거래위원회/소비자24", "OTHER_GOV", "recall", 24,
            "소비자24 리콜정보_해외리콜",
            "https://www.consumer.go.kr/user/ftc/consumer/openApiSvcUser/120/selectOpenApiSvcUserDetail.do",
            true, true,
            "소비자24 OpenAPI 이용조건 확인 필요",
            "https://www.consumer.go.kr/",
            "unknown", "unknown", "unknown",
            "unknown",
            "해외리콜 참고자료입니다. 특정 국가 최신 리콜 없음, 안전성, 인증 적합성, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KGS_HYDROGEN_SANDBOX_REGIONS("한국가스안전공사", "산업통상자원부 산하", "MOTIE_AFFILIATED", "support", 8760,
            "한국가스안전공사_수소분야 규제샌드박스 실증지역별 통계",
            "https://www.data.go.kr/data/15139606/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15139606/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "수소 분야 규제샌드박스 실증지역별 지정 현황 통계입니다. 수소 신사업 참고 신호로만 표시하고 규제 승인, 특구 참여 가능성, 안전 적합성, 최종 사업 실행 판단으로 확정하지 않습니다.",
            "2026-07-05"),
    KICOX_OPERATION_RATE("한국산업단지공단", "산업통상자원부 산하", "MOTIE_AFFILIATED", "market", 2160,
            "한국산업단지공단_국가산업단지 산업동향정보_가동률",
            "https://www.data.go.kr/data/15085889/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15085889/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "국가산업단지 가동률 통계 참고자료입니다. 특정 기업 생산능력, 수요 예측, 입주 가능성, 공급 안정성, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY("한국산업단지공단", "산업통상자원부 산하", "MOTIE_AFFILIATED", "market", 720,
            "한국산업단지공단_공장등록현황통계_시도별 업종별 현황",
            "https://www.data.go.kr/data/15085508/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15085508/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "시도별 업종별 공장등록 현황 참고자료입니다. 특정 기업 생산능력, 입주 가능성, 지역 공급 안정성, 수요 예측, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    MOLIT_WAREHOUSE_REGISTRATION("국토교통부", "국토교통부 물류시설정보과", "OTHER_GOV", "support", 24,
            "국토교통부_물류창고업등록정보",
            "https://apis.data.go.kr/1611000/whsinfoview2",
            true, true,
            "공공데이터포털 개별 데이터셋 이용허락범위 확인 필요",
            "https://www.data.go.kr/ugs/selectPortalPolicyView.do",
            "unknown", "unknown", "unknown",
            "unknown",
            "물류창고업 등록 현황 참고자료입니다. 창고 사용 가능성, 계약 가능성, 물류비, 인허가 적합성, 최종 Gate 판단을 확정하지 않습니다.",
            "2026-07-05"),
    KHNP_SAFETY_HEALTH_CERTIFICATION("한국수력원자력(주)", "산업통상자원부 산하", "MOTIE_AFFILIATED", "support", 8760,
            "한국수력원자력(주)_안전보건 인증현황 정보",
            "https://www.data.go.kr/data/15101214/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15101214/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "한국수력원자력 사업소의 안전보건경영시스템 인증 현황 참고자료입니다. 제품 안전인증, 수출 적합성, 거래 가능성, 규제 준수 판정, 최종 Gate 판단 근거로 확정하지 않습니다.",
            "2026-07-05"),
    MOTIE_IMPORT_REGULATION("산업통상부", "산업통상자원부", "MOTIE", "regulation", 8760,
            "산업통상부_수입규제현황",
            "https://www.data.go.kr/data/3039436/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/3039436/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "CSV 원본을 우선 사용하고, 차기 등록 예정일 이후에는 공공데이터포털 자동변환 API 증거가 있을 때만 live로 표시합니다.",
            "2026-07-05"),
    MOTIE_MPE_TRADE_STATISTICS("산업통상부", "산업통상자원부", "MOTIE", "market", 8760,
            "산업통상부_소재부품장비 무역통계 수출입 총괄",
            "https://www.data.go.kr/data/15099627/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15099627/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "소재부품장비 품목/지역별 수출입 통계 참고자료입니다. 시장 보조 신호로만 쓰며 수요 예측, 가격 전망, 최종 사업 실행 판단으로 확정하지 않습니다.",
            "2026-07-05"),
    MOTIE_MPE_PRODUCTION_INDEX("산업통상부", "산업통상자원부", "MOTIE", "market", 8760,
            "산업통상부_소재부품장비 생산지수통계(생산)",
            "https://www.data.go.kr/data/15099628/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15099628/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "소재부품장비 품목별 생산지수 참고자료입니다. 공급역량, 생산 전망, 최종 사업 실행 판단으로 확정하지 않고 시장 보조 신호로만 표시합니다.",
            "2026-07-05"),
    MOTIE_MPE_INVENTORY_INDEX("산업통상부", "산업통상자원부", "MOTIE", "market", 8760,
            "산업통상부_소재부품장비 생산지수통계(재고)",
            "https://www.data.go.kr/data/15099629/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15099629/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "소재부품장비 품목별 재고지수 참고자료입니다. 재고 부담, 공급역량, 생산 전망, 최종 사업 실행 판단으로 확정하지 않고 시장 보조 신호로만 표시합니다.",
            "2026-07-05"),
    MOTIE_MPE_SHIPMENT_INDEX("산업통상부", "산업통상자원부", "MOTIE", "market", 8760,
            "산업통상부_소재부품장비 생산지수통계(출하)",
            "https://www.data.go.kr/data/15099630/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15099630/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "소재부품장비 품목별 출하지수 참고자료입니다. 출하 흐름, 수요, 공급역량, 최종 사업 실행 판단으로 확정하지 않고 시장 보조 신호로만 표시합니다.",
            "2026-07-05"),
    TRADE_SECURITY_HSK_LINKAGE("무역안보관리원", "산업통상자원부 산하", "MOTIE_AFFILIATED", "regulation", 8760,
            "무역안보관리원_HSK 연계표 정보",
            "https://www.data.go.kr/data/15034135/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15034135/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "HSK와 전략물자 통제번호 후보의 연계표입니다. 전략물자 해당 여부, 허가 필요성, 법률 판단을 확정하지 않고 담당자 확인 후보로만 표시합니다.",
            "2026-07-05"),
    TRADE_SECURITY_ITEM_KEYWORD("무역안보관리원", "산업통상자원부 산하", "MOTIE_AFFILIATED", "regulation", 8760,
            "무역안보관리원_전략물자 품목키워드 및 개정정보",
            "https://www.data.go.kr/data/15042113/fileData.do",
            true, true,
            "공공데이터포털 이용허락범위 제한 없음",
            "https://www.data.go.kr/data/15042113/fileData.do",
            "yes", "yes", "yes",
            "unknown",
            "전략물자 품목 키워드 참고자료입니다. 전략물자 해당 여부, 허가 필요성, 법률 판단을 확정하지 않고 담당자 확인 후보로만 표시합니다.",
            "2026-07-05"),
    KIPRIS_FTO("특허청/KIPRIS", "특허청", "OTHER_GOV", "fto", 72,
            "KIPRIS Plus Open API",
            "http://plus.kipris.or.kr/kipo-api/kipi/patUtiModInfoSearchSevice/getWordSearch",
            true, true,
            "KIPRIS Plus Terms of use 확인 필요",
            "https://plus.kipris.or.kr/portal/main/contents.do?menuNo=200031",
            "conditional", "conditional", "unknown",
            "unknown",
            "특허/상표 공개자료 기반 사전 위험 신호로만 표시하고, FTO 법률 판단 또는 침해 판단으로 표현하지 않습니다. 공식 이용조건 확인 전 대량 원문 재배포를 하지 않습니다.",
            "2026-07-05"),
    BIZINFO_SUPPORT("기업마당/수출지원", "중소벤처기업부 등", "OTHER_GOV", "support", 24,
            "기업마당 지원사업 공고",
            "https://www.bizinfo.go.kr/web/index.do",
            true, true,
            "기업마당 저작권 정책/이용약관 확인 필요",
            "https://www.bizinfo.go.kr/biz/bizd/selectBIZD120View.do",
            "conditional", "conditional", "conditional",
            "yes",
            "원문 링크 제공을 우선하고, 공식 이용조건 확인 전 지원사업 콘텐츠의 재가공/재배포를 제한합니다.",
            "2026-07-04"),
    MOTIE_WEB_CRAWL("산업통상자원부 공개 웹", "산업통상자원부", "MOTIE", "regulation", 24,
            "산업통상자원부 공개 웹 보조근거",
            "https://www.motie.go.kr/",
            false, false,
            "사이트별 이용조건 확인 필요",
            "",
            "unknown", "unknown", "unknown",
            "unknown",
            "공식 operation API proof가 아닌 crawl 보조근거입니다. 원문 링크 확인 전 재배포하지 않습니다.",
            "2026-07-04"),
    EXPORT_EVIDENCE_FEED("수출 뉴스·보고서 feed", "공개 RSS/API", "EXTERNAL_EVIDENCE", "market", 12,
            "수출 뉴스·보고서 feed",
            "",
            false, false,
            "feed 제공자별 이용조건 확인 필요",
            "",
            "unknown", "unknown", "unknown",
            "unknown",
            "공식 공공 API proof가 아닌 external_feed 보조근거입니다.",
            "2026-07-04");

    private final String agency;
    private final String ministry;
    private final String dataFamily;
    private final String usedFor;
    private final int refreshCycleHours;
    private final String sourceName;
    private final String officialUrl;
    private final boolean operationEndpoint;
    private final boolean apiKeyRequired;
    private final String licenseName;
    private final String termsUrl;
    private final String redistributionAllowed;
    private final String storageAllowed;
    private final String reprocessingAllowed;
    private final String attributionRequired;
    private final String usageWarning;
    private final String lastVerifiedAt;

    PublicDataProvider(String agency, String ministry, String dataFamily, String usedFor, int refreshCycleHours,
                       String sourceName, String officialUrl, boolean operationEndpoint, boolean apiKeyRequired,
                       String licenseName, String termsUrl, String redistributionAllowed, String storageAllowed,
                       String reprocessingAllowed, String attributionRequired, String usageWarning,
                       String lastVerifiedAt) {
        this.agency = agency;
        this.ministry = ministry;
        this.dataFamily = dataFamily;
        this.usedFor = usedFor;
        this.refreshCycleHours = refreshCycleHours;
        this.sourceName = sourceName;
        this.officialUrl = officialUrl;
        this.operationEndpoint = operationEndpoint;
        this.apiKeyRequired = apiKeyRequired;
        this.licenseName = licenseName;
        this.termsUrl = termsUrl;
        this.redistributionAllowed = redistributionAllowed;
        this.storageAllowed = storageAllowed;
        this.reprocessingAllowed = reprocessingAllowed;
        this.attributionRequired = attributionRequired;
        this.usageWarning = usageWarning;
        this.lastVerifiedAt = lastVerifiedAt;
    }

    public String agency() {
        return agency;
    }

    public String ministry() {
        return ministry;
    }

    public String dataFamily() {
        return dataFamily;
    }

    public String usedFor() {
        return usedFor;
    }

    public int refreshCycleHours() {
        return refreshCycleHours;
    }

    public String sourceName() {
        return sourceName;
    }

    public String officialUrl() {
        return officialUrl;
    }

    public boolean operationEndpoint() {
        return operationEndpoint;
    }

    public boolean apiKeyRequired() {
        return apiKeyRequired;
    }

    public String licenseName() {
        return licenseName;
    }

    public String termsUrl() {
        return termsUrl;
    }

    public String redistributionAllowed() {
        return redistributionAllowed;
    }

    public String storageAllowed() {
        return storageAllowed;
    }

    public String reprocessingAllowed() {
        return reprocessingAllowed;
    }

    public String attributionRequired() {
        return attributionRequired;
    }

    public String usageWarning() {
        return usageWarning;
    }

    public String lastVerifiedAt() {
        return lastVerifiedAt;
    }

    public boolean motieRelated() {
        return dataFamily.startsWith("MOTIE");
    }
}
