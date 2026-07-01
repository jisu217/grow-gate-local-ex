const dataSources = [
  {
    id: "kcs_trade_gw",
    name: "관세청 수출입무역통계 GW",
    provider: "Korea Customs Service",
    category: "무역통계",
    table: "trade_signal",
    screens: "S04, S09",
    status: "활용 중",
    priority: "Core",
    sourceType: "Public API",
    originUrl: "https://www.data.go.kr/en/data/15102108/openapi.do",
    docsUrl: "https://www.data.go.kr/en/data/15102108/openapi.do",
    endpoint: "http://apis.data.go.kr/1220000/Newtrade/getNewtradeList",
    auth: "data.go.kr serviceKey",
    format: "XML",
    collection: "월/기간 파라미터로 수출입 신고 기반 통계를 조회하고 HS·국가 단위로 정규화",
    reliability: 86,
    insight: "HS 850760과 대상국 기준 시장성 점수의 핵심 근거입니다. 월 단위 업데이트라 실시간 수요 급변은 보조 데이터가 필요합니다.",
    usage: ["시장성 점수와 수출기회 카드의 수입 증가율 근거", "Germany/EU 기회 점수 91의 seed 기준값", "리포트 출력 시 evidence_source로 추적"],
    codeRefs: ["backend/public_data_pipeline.py: trade_signal 정규화", "backend/agents.py: MarketScoringAgent", "frontend/app.js: 시장성 카드 렌더링"],
    dataShape: ["country_code: DE", "hs_code: 850760", "trade_value_usd: 12800000", "import_growth_yoy: 14.7", "opportunity_score: 91"],
    stats: { records: "1 seed", updateCycle: "monthly", targetTable: "trade_signal", screens: "S04, S09" }
  },
  {
    id: "kotra_supply_chain",
    name: "KOTRA 글로벌 공급망 인사이트",
    provider: "KOTRA",
    category: "시장·공급망",
    table: "country_profile",
    screens: "S04, S08, S09",
    status: "활용 중",
    priority: "Core",
    sourceType: "Portal/API",
    originUrl: "https://dream.kotra.or.kr/",
    docsUrl: "https://www.data.go.kr/",
    endpoint: "KOTRA 해외시장뉴스·국가정보 검색 또는 공공데이터포털 KOTRA API",
    auth: "portal search / data.go.kr serviceKey",
    format: "HTML, JSON, XML",
    collection: "국가·산업·키워드별 시장 설명을 수집해 국가 프로필과 규제 메모로 요약",
    reliability: 78,
    insight: "EU 배터리 규제와 바이어 요구를 로드맵 메모로 전환합니다. 정량 수치보다 정책·시장 설명력에 강합니다.",
    usage: ["공급망·정책 압력을 국가 프로필로 반영", "바이어가 선호하는 인증 문서 패키지 설명", "30·60·90일 로드맵의 시장 메모 근거"],
    codeRefs: ["backend/public_data_pipeline.py: country_profile 정규화", "backend/data_connectors.py: public data facade", "docs/architecture.md: 데이터 파이프라인 설명"],
    dataShape: ["country_code: DE", "region: EU", "market_note: EU 배터리 규제 증가", "regulatory_pressure: 0.92", "buyer_note: CE/UN38.3 선호"],
    stats: { records: "1 seed", updateCycle: "ad-hoc", targetTable: "country_profile", screens: "S04, S08, S09" }
  },
  {
    id: "safety_korea_cert_recall",
    name: "Safety Korea 제품 안전인증 및 리콜 정보",
    provider: "KATS Safety Korea",
    category: "인증·리콜",
    table: "recall_case",
    screens: "S05, S06, S08",
    status: "활용 중",
    priority: "Core",
    sourceType: "Public API",
    originUrl: "https://www.safetykorea.kr/release/openapi2",
    docsUrl: "https://www.safetykorea.kr/release/openapi2",
    endpoint: "제품안전정보센터 Open API 인터페이스 설계서 기준",
    auth: "email approval key",
    format: "XML/JSON by interface spec",
    collection: "인증정보·리콜정보를 품목명, 모델명, 위해유형 기준으로 조회해 리스크 사례화",
    reliability: 88,
    insight: "유사 리콜을 시험 전 체크리스트로 바꾸는 차별화 근거입니다. 해외 리콜 데이터와 결합하면 설득력이 더 커집니다.",
    usage: ["배터리팩 유사 리콜 위험 Copilot의 핵심 사례", "낙하·진동·충격·충방전 시험 체크리스트 생성", "규제·인증 패스포트와 실행 보드의 안전 리스크 근거"],
    codeRefs: ["backend/public_data_pipeline.py: recall_case 정규화", "backend/ml_models.py: 리콜 유사도 및 위험도 계산", "frontend/app.js: 리콜/FMEA 카드 렌더링"],
    dataShape: ["product: 전동공구 배터리팩", "category: battery", "hazard: 내부 단락 및 온도상승", "severity: 0.88", "recommended_tests: 4개"],
    stats: { records: "1 seed", updateCycle: "near-real-time", targetTable: "recall_case", screens: "S05, S06, S08, S09" }
  },
  {
    id: "kipris_plus_patent",
    name: "KIPRIS Plus Open API",
    provider: "KIPRIS Plus",
    category: "특허·기술",
    table: "patent_signal",
    screens: "S07, S09",
    status: "활용 중",
    priority: "Core",
    sourceType: "OpenAPI/Bulk",
    originUrl: "https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300026",
    docsUrl: "https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300026",
    endpoint: "Data Service > OpenAPI",
    auth: "KIPRIS Plus account/order",
    format: "API response or bulk file",
    collection: "IPC·키워드·출원인 기준으로 특허 밀집도와 최근 증가율을 생성",
    reliability: 80,
    insight: "BMS, 보호회로, 열관리 키워드의 FTO 1차 위험도를 설명합니다. 법률 의견이 아니라 선행 위험 탐색으로 표현해야 안전합니다.",
    usage: ["BMS·보호회로·열관리 특허 밀집도 산출", "FTO 위험지도에서 1차 위험 키워드 제시", "지원사업·로드맵에서 선행기술 검토 태스크 생성"],
    codeRefs: ["backend/public_data_pipeline.py: patent_signal 정규화", "backend/ml_models.py: FTO 위험 산정", "backend/agents.py: FTORiskAgent"],
    dataShape: ["keyword: battery management system", "ipc: H02J/H01M", "filings: 9630", "recent_growth: 0.22", "risk: 0.81"],
    stats: { records: "1 seed", updateCycle: "daily", targetTable: "patent_signal", screens: "S07, S08, S09" }
  },
  {
    id: "export_voucher_support",
    name: "수출바우처·해외규격인증·지역TP 공고",
    provider: "MOTIE/MSS/KOTRA/Regional TP",
    category: "지원사업",
    table: "support_program",
    screens: "S07, S08",
    status: "활용 중",
    priority: "Core",
    sourceType: "Portal/API",
    originUrl: "https://www.exportvoucher.com/",
    docsUrl: "https://www.data.go.kr/",
    endpoint: "기관별 공고 검색/공공데이터포털 지원사업 API 후보",
    auth: "portal search / data.go.kr serviceKey",
    format: "HTML, JSON, XML",
    collection: "기업 지역, 인증 갭, 마감일, 지원 범위를 태그화해 지원사업 적합도 계산",
    reliability: 74,
    insight: "기업 지역과 인증 갭을 지원사업 우선순위로 바꿉니다. 공고 마감일 자동 동기화가 다음 보강 포인트입니다.",
    usage: ["인증 갭과 지역 정보를 지원사업 적합도 점수로 변환", "해외규격인증획득지원사업을 우선 추천", "30일 로드맵의 신청 가능성 확인 태스크 생성"],
    codeRefs: ["backend/public_data_pipeline.py: support_program 정규화", "backend/agents.py: SupportMatchingAgent", "frontend/app.js: 지원사업 매칭 카드"],
    dataShape: ["name: 해외규격인증획득지원사업", "agency: 중기부/유관기관", "benefit: 인증 비용 일부 지원", "deadline: 공고별 상이", "fit_tags: CE/IEC/SME"],
    stats: { records: "1 seed", updateCycle: "weekly", targetTable: "support_program", screens: "S07, S08, S09" }
  },
  {
    id: "trade_navi_tariff_regulation",
    name: "TradeNAVI 관세·규제·비관세장벽",
    provider: "KITA TradeNAVI",
    category: "관세·비관세",
    table: "tariff_regulation",
    screens: "S04, S05, S08",
    status: "연계 우선",
    priority: "High",
    sourceType: "Portal",
    originUrl: "https://www.kita.net/tradeNavi/tariffRegulation/main.do",
    docsUrl: "https://www.kita.net/tradeNavi/tariffRegulation/main.do",
    endpoint: "관세조회, TBT, SPS, 해외수입요건, 통관거부사례 화면",
    auth: "site access",
    format: "HTML",
    collection: "HSK/대상국 기준 관세율, TBT, SPS, 수입요건을 수집해 규제 여권 보강",
    reliability: 82,
    insight: "2026년 기준 EU·미국 등 주요국 관세 업데이트를 빠르게 확인할 수 있어 관세/비관세 근거를 보강합니다.",
    usage: ["FTA·MFN 관세 비교", "TBT/SPS 이슈를 인증 태스크로 변환", "통관거부 사례를 사전 체크리스트로 반영"],
    codeRefs: ["backend/public_data_pipeline.py: tariff_regulation 후보", "backend/agents.py: RegulationPassportAgent", "frontend/app.js: 규제 여권 카드"],
    dataShape: ["country: EU", "hs_code: 850760", "tariff_year: 2026", "barrier_type: TBT", "requirement: battery documentation"],
    stats: { records: "candidate", updateCycle: "monthly", targetTable: "tariff_regulation", screens: "S04, S05, S08" }
  },
  {
    id: "kosis_region_indicators",
    name: "KOSIS 지역 제조·수출 경제지표",
    provider: "KOSIS",
    category: "지역·거시",
    table: "regional_indicator",
    screens: "S01, S08, S09",
    status: "연계 우선",
    priority: "High",
    sourceType: "Public API",
    originUrl: "https://kosis.kr/serviceInfo/openAPIGuide.do",
    docsUrl: "https://kosis.kr/serviceInfo/openAPIGuide.do",
    endpoint: "KOSIS 공유서비스 OpenAPI",
    auth: "KOSIS/API key",
    format: "JSON, SDMX, XML, XLS",
    collection: "지역·산업 통계를 호출해 경남 제조기업 맥락과 정책효과 KPI 기준값 생성",
    reliability: 84,
    insight: "지역 제조업과 수출입물가지수, e-지방지표를 연결하면 정책성과와 현장성 설명이 강해집니다.",
    usage: ["지역기업 문제정의 근거", "정책효과 KPI 기준선", "심사 발표의 지역 파급효과 수치화"],
    codeRefs: ["backend/public_data_pipeline.py: regional_indicator 후보", "backend/agents.py: PolicyImpactAgent", "frontend/team.js: 역할자 평가 근거"],
    dataShape: ["region: 경남", "industry: 제조업", "indicator: export_price_index", "period: 2026M04", "value: 104.3"],
    stats: { records: "candidate", updateCycle: "monthly/quarterly", targetTable: "regional_indicator", screens: "S01, S08, S09" }
  },
  {
    id: "un_comtrade_global_trade",
    name: "UN Comtrade 글로벌 HS 무역통계",
    provider: "United Nations",
    category: "무역통계",
    table: "global_trade_flow",
    screens: "S04, S09",
    status: "수집 후보",
    priority: "High",
    sourceType: "Open API",
    originUrl: "https://comtradeplus.un.org/",
    docsUrl: "https://comtradedeveloper.un.org/apis",
    endpoint: "UN Comtrade API developer portal",
    auth: "subscription/API key",
    format: "JSON, CSV",
    collection: "HS6·보고국·상대국별 수입/수출 흐름을 조회해 한국 관세청 데이터와 교차검증",
    reliability: 83,
    insight: "한국 관세청 데이터의 상대국 관점 검증과 글로벌 수요 비교에 적합합니다.",
    usage: ["국가별 수입 수요 비교", "한국 데이터와 mirror 통계 검증", "시장성 점수의 글로벌 기준선"],
    codeRefs: ["backend/data_connectors.py: comtrade connector 후보", "backend/ml_models.py: demand baseline", "frontend/app.js: 시장성 비교 차트"],
    dataShape: ["reporter: DEU", "partner: KOR", "cmdCode: 850760", "flow: import", "primaryValue: 12800000"],
    stats: { records: "candidate", updateCycle: "monthly/annual", targetTable: "global_trade_flow", screens: "S04, S09" }
  },
  {
    id: "eurostat_comext",
    name: "Eurostat Comext EU 상품무역",
    provider: "Eurostat",
    category: "무역통계",
    table: "eu_trade_flow",
    screens: "S04, S09",
    status: "수집 후보",
    priority: "High",
    sourceType: "Open API",
    originUrl: "https://ec.europa.eu/eurostat/web/user-guides/data-browser/api-data-access/api-getting-started/comext-database",
    docsUrl: "https://ec.europa.eu/eurostat/web/user-guides/data-browser/api-data-access/api-getting-started/comext-database",
    endpoint: "https://ec.europa.eu/eurostat/api/comext/dissemination/sdmx/2.1/",
    auth: "none/API rate policy",
    format: "SDMX",
    collection: "EU 회원국별 HS 상세 수입통계를 가져와 Germany/EU 타깃 시장을 세분화",
    reliability: 85,
    insight: "EU 내부 시장별 수요와 수입 구조를 볼 수 있어 Germany 단일국가 판단을 EU 전체로 확장합니다.",
    usage: ["EU 국가별 수입 성장률", "독일 외 후보국 도출", "시장성 점수 민감도 분석"],
    codeRefs: ["backend/data_connectors.py: eurostat connector 후보", "backend/ml_models.py: EU market split", "frontend/data-catalog.html: Comext 명세"],
    dataShape: ["geo: DE", "partner: KR", "product: 850760", "flow: imports", "unit: EUR"],
    stats: { records: "candidate", updateCycle: "monthly", targetTable: "eu_trade_flow", screens: "S04, S09" }
  },
  {
    id: "eu_access2markets",
    name: "EU Access2Markets 관세·원산지·요건",
    provider: "European Commission",
    category: "관세·비관세",
    table: "eu_market_requirement",
    screens: "S05, S08, S09",
    status: "수집 후보",
    priority: "High",
    sourceType: "Portal/Dataset",
    originUrl: "https://data.europa.eu/data/datasets/market-access-database?locale=en",
    docsUrl: "https://data.europa.eu/data/datasets/market-access-database?locale=en",
    endpoint: "Access2Markets portal dataset",
    auth: "site access",
    format: "HTML/dataset",
    collection: "HS·원산지·목적국 기준으로 관세, 원산지 규칙, 제품요건을 추출",
    reliability: 82,
    insight: "EU 수출 준비에서 관세뿐 아니라 원산지와 제품요건을 한 번에 설명하는 핵심 보조 근거입니다.",
    usage: ["원산지 증빙 태스크", "관세 감면 가능성 체크", "제품요건 누락 리스크 확인"],
    codeRefs: ["backend/agents.py: RegulationPassportAgent", "backend/public_data_pipeline.py: eu_market_requirement 후보", "frontend/app.js: 인증 여권"],
    dataShape: ["destination: EU", "origin: KR", "hs_code: 850760", "rule_of_origin: PSR", "requirement: product compliance"],
    stats: { records: "candidate", updateCycle: "ad-hoc", targetTable: "eu_market_requirement", screens: "S05, S08, S09" }
  },
  {
    id: "eu_safety_gate",
    name: "EU Safety Gate 위험제품 알림",
    provider: "European Commission",
    category: "인증·리콜",
    table: "foreign_recall_case",
    screens: "S06, S08, S09",
    status: "수집 후보",
    priority: "High",
    sourceType: "Portal",
    originUrl: "https://ec.europa.eu/safety-gate/",
    docsUrl: "https://ec.europa.eu/safety-gate/",
    endpoint: "Safety Gate alert search",
    auth: "site access",
    format: "HTML/search result",
    collection: "제품군·위해유형·국가별 EU 위험제품 알림을 수집해 국내 리콜과 결합",
    reliability: 79,
    insight: "EU 비식품 위험제품 알림은 배터리팩 안전성 설득력을 해외 시장 맥락으로 확장합니다.",
    usage: ["해외 리콜 근거", "시험 전 위험유형 보강", "발표 차별화 사례"],
    codeRefs: ["backend/ml_models.py: recall similarity 후보", "backend/agents.py: RecallRiskAgent", "frontend/app.js: 리콜 위험 카드"],
    dataShape: ["country: EU", "category: electrical appliances", "risk: fire", "measure: recall", "product: battery pack"],
    stats: { records: "candidate", updateCycle: "weekly", targetTable: "foreign_recall_case", screens: "S06, S08, S09" }
  },
  {
    id: "cpsc_recalls",
    name: "미국 CPSC Recalls API",
    provider: "U.S. CPSC",
    category: "인증·리콜",
    table: "foreign_recall_case",
    screens: "S06, S08, S09",
    status: "수집 후보",
    priority: "Medium",
    sourceType: "Open API",
    originUrl: "https://www.cpsc.gov/Recalls/CPSC-Recalls-Application-Program-Interface-API-Information?language=en",
    docsUrl: "https://www.cpsc.gov/Recalls/CPSC-Recalls-Application-Program-Interface-API-Information?language=en",
    endpoint: "CPSC Recalls API",
    auth: "none",
    format: "JSON",
    collection: "제품명·위해유형·조치일을 수집해 미국 시장 진출 시 리콜 선례 분석",
    reliability: 80,
    insight: "미국 진출 확장 시 CPSC 리콜 사례를 사전에 연결하면 다국가 안전성 검토가 가능해집니다.",
    usage: ["미국 리콜 선례", "위해유형 빈도 분석", "북미 확장 로드맵"],
    codeRefs: ["backend/data_connectors.py: cpsc connector 후보", "backend/ml_models.py: hazard clustering", "frontend/data-catalog.html: recall chart"],
    dataShape: ["recallDate: 2026-01-10", "product: lithium battery", "hazard: fire", "remedy: refund", "country: US"],
    stats: { records: "candidate", updateCycle: "near-real-time", targetTable: "foreign_recall_case", screens: "S06, S08, S09" }
  },
  {
    id: "world_bank_indicators",
    name: "World Bank 거시·시장 지표 API",
    provider: "World Bank",
    category: "지역·거시",
    table: "macro_indicator",
    screens: "S04, S09",
    status: "수집 후보",
    priority: "Medium",
    sourceType: "Open API",
    originUrl: "https://datahelpdesk.worldbank.org/knowledgebase/articles/889392",
    docsUrl: "https://datahelpdesk.worldbank.org/knowledgebase/articles/889392",
    endpoint: "https://api.worldbank.org/v2/country/{country}/indicator/{indicator}",
    auth: "none",
    format: "JSON, XML",
    collection: "GDP, 인구, 물류·무역 관련 지표를 국가별 시장성 보정값으로 수집",
    reliability: 77,
    insight: "무역량만으로 설명하기 어려운 시장규모와 성장 배경을 보정합니다.",
    usage: ["시장규모 보정", "국가 리스크 배경값", "발표용 거시 지표"],
    codeRefs: ["backend/data_connectors.py: worldbank connector 후보", "backend/ml_models.py: macro adjustment", "frontend/app.js: 시장성 설명"],
    dataShape: ["country: DE", "indicator: NY.GDP.MKTP.CD", "date: 2025", "value: 4.5e12", "source: WDI"],
    stats: { records: "candidate", updateCycle: "annual", targetTable: "macro_indicator", screens: "S04, S09" }
  },
  {
    id: "wto_itip_goods",
    name: "WTO I-TIP Goods 비관세조치",
    provider: "WTO",
    category: "관세·비관세",
    table: "non_tariff_measure",
    screens: "S05, S08, S09",
    status: "수집 후보",
    priority: "Medium",
    sourceType: "Dataset/API",
    originUrl: "https://www.wto.org/english/res_e/statis_e/itip_e.htm",
    docsUrl: "https://data.wto.org/es/dataset/ext_itip_1",
    endpoint: "WTO data portal I-TIP Goods datasets",
    auth: "site/data portal access",
    format: "dataset/API",
    collection: "SPS, TBT, AD, CVD 등 비관세조치 유형을 HS·국가 기준으로 매핑",
    reliability: 78,
    insight: "기술규제와 무역구제 조치를 국가별로 비교해 규제 리스크 점수를 보강할 수 있습니다.",
    usage: ["비관세 리스크 점수", "TBT/SPS 근거", "정책 담당자 관점 보강"],
    codeRefs: ["backend/public_data_pipeline.py: non_tariff_measure 후보", "backend/agents.py: TradeBarrierAgent", "frontend/team.js: 데이터 갭 대시보드"],
    dataShape: ["member: EU", "measure_type: TBT", "hs_code: 850760", "in_force_date: 2026-01-01", "source_doc: notification"],
    stats: { records: "candidate", updateCycle: "ad-hoc", targetTable: "non_tariff_measure", screens: "S05, S08, S09" }
  },
  {
    id: "law_open_data",
    name: "국가법령정보 공동활용 API",
    provider: "국가법령정보센터",
    category: "법령·표준",
    table: "legal_requirement",
    screens: "S05, S08",
    status: "수집 후보",
    priority: "Medium",
    sourceType: "Open API",
    originUrl: "https://open.law.go.kr/LSO/openApi/guideList.do",
    docsUrl: "https://open.law.go.kr/LSO/openApi/guideList.do",
    endpoint: "국가법령정보 공동활용 Open API",
    auth: "API key",
    format: "XML/HTML",
    collection: "전기용품·생활용품 안전관리법 등 국내 법령 근거를 인증 여권에 링크",
    reliability: 76,
    insight: "국내 인증·안전 의무의 법적 근거를 화면에서 바로 추적할 수 있게 만듭니다.",
    usage: ["법령 근거 링크", "인증 체크리스트 설명", "내수/수출 병행 리스크"],
    codeRefs: ["backend/data_connectors.py: law connector 후보", "backend/agents.py: ComplianceAgent", "frontend/app.js: 규제 근거"],
    dataShape: ["law_id: 009994", "article: 제5조", "topic: safety certification", "effective_date: 2026-01-01", "url: open.law.go.kr"],
    stats: { records: "candidate", updateCycle: "ad-hoc", targetTable: "legal_requirement", screens: "S05, S08" }
  },
  {
    id: "standard_certification",
    name: "e-나라표준인증·국가기술표준원 기준",
    provider: "KATS/e-나라표준인증",
    category: "법령·표준",
    table: "standard_requirement",
    screens: "S05, S08, S09",
    status: "수집 후보",
    priority: "High",
    sourceType: "Portal",
    originUrl: "https://www.standard.go.kr/",
    docsUrl: "https://www.kats.go.kr/",
    endpoint: "표준·인증 검색",
    auth: "site access",
    format: "HTML/PDF",
    collection: "KC, KS, 전기용품 안전기준과 시험 항목을 제품군 기준으로 매핑",
    reliability: 79,
    insight: "시험기관과 대화할 때 필요한 규격 버전과 시험 항목을 구체화합니다.",
    usage: ["시험 항목 명세", "인증 준비 문서", "규격 버전 체크"],
    codeRefs: ["backend/agents.py: CertificationRoadmapAgent", "backend/public_data_pipeline.py: standard_requirement 후보", "frontend/app.js: 규격 체크리스트"],
    dataShape: ["standard_id: KC 62133-2", "product: lithium battery", "test: vibration", "sample_count: TBD", "version: latest"],
    stats: { records: "candidate", updateCycle: "ad-hoc", targetTable: "standard_requirement", screens: "S05, S08, S09" }
  },
  {
    id: "oecd_global_recalls",
    name: "OECD GlobalRecalls",
    provider: "OECD",
    category: "인증·리콜",
    table: "foreign_recall_case",
    screens: "S06, S09",
    status: "수집 후보",
    priority: "Medium",
    sourceType: "Portal",
    originUrl: "https://globalrecalls.oecd.org/",
    docsUrl: "https://globalrecalls.oecd.org/",
    endpoint: "GlobalRecalls search",
    auth: "site access",
    format: "HTML/search result",
    collection: "국가별 리콜 알림을 제품군·위해유형 기준으로 보조 수집",
    reliability: 74,
    insight: "여러 국가 리콜 출처를 한 번에 탐색하는 보조 레이어로 적합합니다.",
    usage: ["해외 리콜 후보 탐색", "EU/US 외 국가 비교", "발표용 글로벌 위험 지도"],
    codeRefs: ["backend/data_connectors.py: global recalls 후보", "backend/ml_models.py: hazard similarity", "frontend/data-catalog.html: coverage chart"],
    dataShape: ["jurisdiction: OECD", "product: battery pack", "hazard: fire", "measure: recall", "source_country: multiple"],
    stats: { records: "candidate", updateCycle: "weekly", targetTable: "foreign_recall_case", screens: "S06, S09" }
  },
  {
    id: "eximbank_exchange_rate",
    name: "한국수출입은행 환율정보 API",
    provider: "Korea Eximbank",
    category: "금융·비용",
    table: "fx_rate",
    screens: "S04, S08, S09",
    status: "수집 후보",
    priority: "Medium",
    sourceType: "Public API",
    originUrl: "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON",
    docsUrl: "https://www.data.go.kr/",
    endpoint: "https://www.koreaexim.go.kr/site/program/financial/exchangeJSON",
    auth: "authkey",
    format: "JSON",
    collection: "USD/EUR/KRW 환율을 수집해 인증비용, 수출가격, 지원금 환산에 반영",
    reliability: 76,
    insight: "CEO 관점에서 비용·가격·지원금 규모를 같은 통화로 설명할 수 있게 만듭니다.",
    usage: ["인증비용 환산", "수출가격 민감도", "지원사업 금액 비교"],
    codeRefs: ["backend/data_connectors.py: fx connector 후보", "backend/ml_models.py: cost normalization", "frontend/app.js: 비용 카드"],
    dataShape: ["cur_unit: EUR", "deal_bas_r: 1480.2", "date: 20260527", "base: KRW", "use: certification_cost"],
    stats: { records: "candidate", updateCycle: "daily", targetTable: "fx_rate", screens: "S04, S08, S09" }
  }
];

const missingData = [
  { title: "시험기관 견적·리드타임", impact: "로드맵이 추정값이 아니라 실행 일정으로 보입니다.", priority: "High", owner: "인증" },
  { title: "지역기업 인터뷰 3건", impact: "현장 문제 정의와 UX 타당성을 보강합니다.", priority: "High", owner: "기획" },
  { title: "지원사업 마감 캘린더", impact: "추천 기능이 실제 신청 행동으로 이어집니다.", priority: "Medium", owner: "운영" },
  { title: "시험규격 버전·샘플 조건", impact: "인증 시험기관 관점에서 실행 가능성이 올라갑니다.", priority: "Medium", owner: "인증" },
  { title: "심사 기준표 매핑", impact: "공모전 점수 상승 행동을 더 정확히 계산할 수 있습니다.", priority: "Medium", owner: "발표" }
];

const stakeholderScores = [
  { role: "공모전 심사위원", group: "평가", score: 86, confidence: "중상", evidence: "공공데이터 출처와 화면별 활용 흐름이 명확합니다.", concern: "실제 사용자 검증 수치가 아직 약합니다.", improvement: "기업 인터뷰와 전후 비교 지표를 추가하세요." },
  { role: "박사급 실무자", group: "전문", score: 82, confidence: "중", evidence: "규제·리콜·특허·지원사업을 제품 단위로 묶은 구조가 타당합니다.", concern: "추정 모델 설명과 검증 데이터가 제한적입니다.", improvement: "모델 입력 특성과 fallback 기준을 한 장으로 설명하세요." },
  { role: "수출지원 업무 담당자", group: "실무", score: 88, confidence: "중상", evidence: "30·60·90일 산출물 중심 로드맵이 업무 방식과 맞습니다.", concern: "기관 상담 연결과 문서 첨부 흐름이 빠져 있습니다.", improvement: "상담 요청 버튼과 필요 서류 체크리스트를 보강하세요." },
  { role: "중소 CEO", group: "경영", score: 84, confidence: "중", evidence: "무엇을 먼저 해야 하는지 비용·일정 관점으로 보입니다.", concern: "예상 비용과 지원금 규모가 더 필요합니다.", improvement: "시험·인증 예상 비용 범위를 카드로 추가하세요." },
  { role: "대기업 CEO", group: "경영", score: 76, confidence: "중", evidence: "공급망·규제 리스크 통합 관점은 의미가 있습니다.", concern: "엔터프라이즈 권한, 감사, 다국가 운영 관점은 부족합니다.", improvement: "확장 아키텍처와 감사 로그 로드맵을 별도로 제시하세요." },
  { role: "장관·정책 의사결정자", group: "정책", score: 85, confidence: "중", evidence: "지역 제조기업 수출 병목을 공공데이터 활용 사례로 잘 보여줍니다.", concern: "정책 효과 지표가 정량화되지 않았습니다.", improvement: "지역별 인증 리드타임 단축 KPI를 추가하세요." },
  { role: "학부생", group: "학습", score: 90, confidence: "상", evidence: "데이터 흐름과 화면 구조가 직관적이라 발표 이해가 쉽습니다.", concern: "전문 용어가 많아 초반 설명이 필요합니다.", improvement: "첫 화면에 용어 도움말을 넣으세요." },
  { role: "대학원생", group: "연구", score: 80, confidence: "중", evidence: "문제 정의와 에이전트 분해가 연구 주제로 확장 가능합니다.", concern: "실험 설계와 비교군이 부족합니다.", improvement: "기존 서비스 대비 의사결정 시간 단축 실험을 설계하세요." },
  { role: "수출업자", group: "사용자", score: 87, confidence: "중상", evidence: "HS Code, 인증, 물류, 지원사업이 한 화면에서 연결됩니다.", concern: "국가별 바이어 발굴 데이터가 더 있으면 좋습니다.", improvement: "KOTRA 무역관 상담 연결을 액션으로 추가하세요." },
  { role: "수입업자", group: "사용자", score: 73, confidence: "중", evidence: "공급업체의 인증 준비도를 확인하는 용도는 있습니다.", concern: "수입자 관점의 통관·책임소재 정보는 부족합니다.", improvement: "수입자 체크리스트와 책임 문서 항목을 추가하세요." },
  { role: "인증 시험기관", group: "전문", score: 79, confidence: "중", evidence: "시험 항목과 준비 문서를 먼저 정리하는 흐름이 좋습니다.", concern: "시험 규격 버전과 샘플 조건이 더 구체적이어야 합니다.", improvement: "규격 버전, 샘플 수량, 전처리 조건 필드를 추가하세요." },
  { role: "투자자", group: "사업", score: 78, confidence: "중", evidence: "공공데이터 기반 B2B SaaS 확장 가능성이 보입니다.", concern: "반복 사용과 과금 지점이 아직 약합니다.", improvement: "기관 관리자 대시보드와 기업별 이력 저장 로드맵을 제시하세요." },
  { role: "지역 지자체 담당자", group: "정책", score: 89, confidence: "중상", evidence: "지역 제조기업 지원사업과 실제 수출 준비 업무가 직접 연결됩니다.", concern: "지역별 산업군 템플릿이 더 있으면 도입 설득이 쉽습니다.", improvement: "배터리 외 기계·소재·생활용품 데모 케이스를 추가하세요." }
];

const awardFactors = [
  { label: "공공데이터 활용성", value: 88, type: "positive", text: "5개 출처가 화면별 기능과 연결되어 있습니다." },
  { label: "문제 적합성", value: 86, type: "positive", text: "지역 제조기업의 수출 규제·인증 병목을 직접 겨냥합니다." },
  { label: "구현 완성도", value: 82, type: "positive", text: "정적 프론트와 FastAPI 백엔드가 함께 준비되어 있습니다." },
  { label: "실증 근거", value: 58, type: "risk", text: "실제 기업 인터뷰, 테스트 로그, 효과 지표가 더 필요합니다." },
  { label: "발표 차별성", value: 78, type: "positive", text: "AI 에이전트와 30·60·90일 실행 로드맵이 강한 스토리입니다." }
];

const actionSimulations = [
  { id: "interview", title: "지역기업 인터뷰 3건 추가", effect: "도움", delta: 7, type: "positive", effort: "반나절", owner: "기획", reason: "심사에서 가장 약한 실증 근거를 직접 보강합니다. 문제 정의가 실제 현장 기반이라는 증거가 생깁니다." },
  { id: "foreign-recall", title: "EU/US 리콜 데이터 연결", effect: "도움", delta: 5, type: "positive", effort: "1일", owner: "데이터", reason: "국내 Safety Korea 근거를 해외 시장 리스크와 연결해 수출 서비스로서의 신뢰도를 높입니다." },
  { id: "more-animation", title: "화려한 애니메이션 추가", effect: "감점 가능", delta: -3, type: "negative", effort: "반나절", owner: "프론트", reason: "운영형 B2B 대시보드에서는 장식보다 근거와 실행성이 중요합니다. 발표 집중도를 분산시킬 수 있습니다." },
  { id: "cost-range", title: "시험·인증 비용 범위 추가", effect: "도움", delta: 4, type: "positive", effort: "반나절", owner: "인증", reason: "중소 CEO와 실무자가 바로 판단할 수 있는 비용 감각을 제공합니다." },
  { id: "llm-copy", title: "모든 설명을 AI 문장으로 길게 확장", effect: "감점 가능", delta: -4, type: "negative", effort: "반나절", owner: "발표", reason: "심사위원은 긴 설명보다 근거, 점수, 행동을 빠르게 봅니다. 장황한 문장은 완성도를 낮춰 보이게 합니다." },
  { id: "judge-map", title: "심사 기준표와 기능 매핑", effect: "도움", delta: 6, type: "positive", effort: "2시간", owner: "PM", reason: "어떤 기능이 어떤 평가 항목 점수를 올리는지 명확해져 발표 전략과 우선순위가 정리됩니다." }
];

const codeGate = {
  reviewFocus: [
    { title: "데이터 근거 표현", text: "공공데이터 출처, 정규화 테이블, 화면 연결이 과장 없이 보이는지 확인합니다." },
    { title: "Git 동작 안전성", text: "정적 페이지 버튼이 실제 커밋·철회를 실행하는 것처럼 보이지 않는지 확인합니다." },
    { title: "반응형 가독성", text: "역할자 점수 카드와 체크리스트가 모바일에서 겹치지 않는지 확인합니다." }
  ],
  reviewChecklist: [
    "CSP를 깨는 inline script가 없는가",
    "수상 가능성이 확정값처럼 표현되지 않는가",
    "공공데이터 출처와 활용 화면이 연결되는가",
    "코드 게이트 버튼이 UI 상태 표시임을 명확히 말하는가",
    "모바일 폭에서 긴 한국어 문장이 카드 밖으로 넘치지 않는가",
    "발표에 불필요한 장식보다 판단 정보가 우선되는가"
  ],
  preCommitChecklist: [
    "team.html 직접 열기 확인",
    "code-gate.html 직접 열기 확인",
    "모든 액션 카드 클릭 확인",
    "코드 게이트 버튼과 체크박스 동작 확인",
    "미완성 표식과 오해 소지 문구 검색",
    "변경 파일 목록과 커밋 메시지 확인"
  ],
  commitCommands: [
    "git status --short",
    "git diff -- grow_gate_local_ex_demo/frontend",
    "git add grow_gate_local_ex_demo/frontend/team.html grow_gate_local_ex_demo/frontend/code-gate.html grow_gate_local_ex_demo/frontend/team.js grow_gate_local_ex_demo/frontend/styles.css",
    "git commit -m \"feat: add team operations dashboards\""
  ],
  rollbackCommands: [
    "git log --oneline -5",
    "git revert <commit-sha>",
    "git status --short"
  ],
  rollbackVersions: [
    { name: "설계 승인 전", risk: "낮음", use: "대시보드 방향 자체를 바꿀 때 사용합니다." },
    { name: "팀 운영실 추가 전", risk: "중간", use: "전략 대시보드만 문제가 있을 때 사용합니다." },
    { name: "최종 커밋 전", risk: "낮음", use: "시각 QA 후 마지막 수정만 되돌릴 때 사용합니다." }
  ]
};

const gateState = {
  reviewRequested: false,
  reviewCompleted: false,
  preCommitChecked: false,
  commitReady: false,
  rollbackOpen: false
};

let activeSourceId = null;
let sourcePage = 1;
const sourcePageSize = 6;

function qs(selector) {
  return document.querySelector(selector);
}

function escapeHtml(value) {
  return String(value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function renderDataShapeList(items) {
  return `
    <div class="data-shape-list">
      ${items.map((item) => {
        const [rawKey, ...rawValue] = item.split(":");
        const key = rawValue.length ? rawKey.trim() : "field";
        const value = rawValue.length ? rawValue.join(":").trim() : item;
        return `
          <div>
            <span>${escapeHtml(key)}</span>
            <strong>${escapeHtml(value)}</strong>
          </div>
        `;
      }).join("")}
    </div>
  `;
}

function renderTeamDashboard() {
  renderCatalogMetrics();
  renderDataSources();
  renderMissingData();
  renderStakeholderScores();
  renderAwardFactors();
  renderActionSimulator();
  closeDataSourceDetail();
}

function renderCatalogMetrics() {
  const countTarget = qs("#catalogSourceCount");
  const statusTarget = qs("#catalogStatusSummary");
  if (countTarget) countTarget.textContent = dataSources.length;
  if (statusTarget) {
    const active = dataSources.filter((source) => source.status === "활용 중").length;
    const high = dataSources.filter((source) => source.priority === "High").length;
    statusTarget.textContent = `${active}개 활용 중, ${high}개 우선 연계 후보까지 추적`;
  }
}

function renderDataSources() {
  const target = qs("#teamDataSources");
  if (!target) return;
  const totalPages = Math.ceil(dataSources.length / sourcePageSize);
  sourcePage = Math.min(Math.max(sourcePage, 1), totalPages);
  const start = (sourcePage - 1) * sourcePageSize;
  const pageSources = dataSources.slice(start, start + sourcePageSize);
  target.innerHTML = pageSources.map((source) => `
    <article class="source-row" role="button" tabindex="0" data-source-id="${escapeHtml(source.id)}" aria-label="${escapeHtml(source.name)} 상세 보기">
      <div>
        <span class="source-status">${escapeHtml(source.status)} · ${escapeHtml(source.category)}</span>
        <h3>${escapeHtml(source.name)}</h3>
        <p>${escapeHtml(source.insight)}</p>
      </div>
      <dl>
        <dt>제공</dt><dd>${escapeHtml(source.provider)}</dd>
        <dt>테이블</dt><dd>${escapeHtml(source.table)}</dd>
        <dt>형태</dt><dd>${escapeHtml(source.format)}</dd>
      </dl>
      <div class="mini-score" style="--value:${source.reliability}%"><b>${source.reliability}</b><span>신뢰</span></div>
    </article>
  `).join("");
  renderDataSourcePager(totalPages, start, pageSources.length);
  target.querySelectorAll("[data-source-id]").forEach((row) => {
    row.addEventListener("click", () => selectDataSource(row.dataset.sourceId));
    row.addEventListener("keydown", (event) => {
      if (event.key === "Enter" || event.key === " ") {
        event.preventDefault();
        selectDataSource(row.dataset.sourceId);
      }
    });
  });
}

function renderDataSourcePager(totalPages, start, count) {
  const target = qs("#sourcePager");
  if (!target) return;
  target.innerHTML = `
    <span>${start + 1}-${start + count} / ${dataSources.length}</span>
    <div>
      ${Array.from({ length: totalPages }, (_, index) => `
        <button class="${sourcePage === index + 1 ? "active" : ""}" type="button" data-source-page="${index + 1}">
          ${index + 1}
        </button>
      `).join("")}
    </div>
  `;
  target.querySelectorAll("[data-source-page]").forEach((button) => {
    button.addEventListener("click", () => {
      sourcePage = Number(button.dataset.sourcePage);
      closeDataSourceDetail();
      renderDataSources();
    });
  });
}

function closeDataSourceDetail() {
  activeSourceId = null;
  qs(".source-workbench")?.classList.remove("detail-open");
  qs("#teamDataSources")?.querySelectorAll("[data-source-id]").forEach((row) => {
    row.classList.remove("selected");
  });
  const target = qs("#sourceDetail");
  if (!target) return;
  target.hidden = true;
  target.innerHTML = "";
}

function selectDataSource(id) {
  const source = dataSources.find((item) => item.id === id) || dataSources[0];
  const target = qs("#sourceDetail");
  if (!target) return;
  activeSourceId = source.id;
  target.hidden = false;
  qs(".source-workbench")?.classList.add("detail-open");
  qs("#teamDataSources")?.querySelectorAll("[data-source-id]").forEach((row) => {
    row.classList.toggle("selected", row.dataset.sourceId === source.id);
  });
  target.innerHTML = `
    <div class="panel-heading horizontal">
      <div>
        <p class="eyebrow">SOURCE DETAIL</p>
        <h2>${escapeHtml(source.name)}</h2>
      </div>
      <div class="source-detail-actions">
        <span class="badge source-table-badge">${escapeHtml(source.table)}</span>
        <a class="source-spec-link" href="data-catalog.html#${escapeHtml(source.id)}">명세서 보기</a>
        <button class="source-detail-close" type="button">목록만 보기</button>
      </div>
    </div>
    <div class="source-detail-summary">
      <article><span>신뢰도</span><strong class="score-value">${source.reliability}</strong><p>seed와 공식 출처 매핑 기준</p></article>
      <article><span>정규화</span><strong class="chip-value">${escapeHtml(source.stats.targetTable)}</strong><p>Agent 입력 계약</p></article>
      <article><span>연결 화면</span><strong class="chip-value">${escapeHtml(source.stats.screens)}</strong><p>근거 표시 위치</p></article>
    </div>
    <div class="source-detail-grid">
      <article class="source-detail-card">
        <h3>활용 내역 요약</h3>
        <ul>${source.usage.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>
      </article>
      <article class="source-detail-card">
        <h3>실제 코드 연결</h3>
        <ul>${source.codeRefs.map((item) => `<li><code>${escapeHtml(item)}</code></li>`).join("")}</ul>
      </article>
      <article class="source-detail-card">
        <h3>데이터 형태</h3>
        ${renderDataShapeList(source.dataShape)}
      </article>
      <article class="source-detail-card">
        <h3>간단 통계</h3>
        <dl>
          <dt>레코드</dt><dd>${escapeHtml(source.stats.records)}</dd>
          <dt>갱신</dt><dd>${escapeHtml(source.stats.updateCycle)}</dd>
          <dt>테이블</dt><dd>${escapeHtml(source.stats.targetTable)}</dd>
          <dt>화면</dt><dd>${escapeHtml(source.stats.screens)}</dd>
        </dl>
      </article>
      <article class="source-detail-card">
        <h3>수집 명세</h3>
        <dl>
          <dt>출처</dt><dd><a href="${escapeHtml(source.originUrl)}" target="_blank" rel="noreferrer">${escapeHtml(source.provider)}</a></dd>
          <dt>엔드포인트</dt><dd><code>${escapeHtml(source.endpoint)}</code></dd>
          <dt>인증</dt><dd>${escapeHtml(source.auth)}</dd>
          <dt>수집</dt><dd>${escapeHtml(source.collection)}</dd>
        </dl>
      </article>
    </div>
  `;
  qs(".source-detail-close")?.addEventListener("click", () => {
    const rowToFocus = qs(`[data-source-id="${activeSourceId}"]`);
    closeDataSourceDetail();
    rowToFocus?.focus();
  });
}

function renderMissingData() {
  const target = qs("#missingDataList");
  if (!target) return;
  target.innerHTML = missingData.map((item) => `
    <article>
      <div>
        <strong>${escapeHtml(item.title)}</strong>
        <p>${escapeHtml(item.impact)}</p>
      </div>
      <span class="priority ${item.priority.toLowerCase()}">${escapeHtml(item.priority)} · ${escapeHtml(item.owner)}</span>
    </article>
  `).join("");
}

function renderStakeholderScores() {
  const target = qs("#stakeholderScores");
  if (!target) return;
  target.innerHTML = stakeholderScores.map((item) => `
    <article class="role-card">
      <div class="role-card-top">
        <span>${escapeHtml(item.group)}</span>
        <strong>${item.score}</strong>
      </div>
      <h3>${escapeHtml(item.role)}</h3>
      <div class="score-track" aria-hidden="true"><i style="width:${item.score}%"></i></div>
      <p><b>근거</b>${escapeHtml(item.evidence)}</p>
      <p><b>부족</b>${escapeHtml(item.concern)}</p>
      <p><b>보강</b>${escapeHtml(item.improvement)}</p>
      <small>신뢰도 ${escapeHtml(item.confidence)}</small>
    </article>
  `).join("");
}

function renderAwardFactors() {
  const target = qs("#awardFactors");
  if (!target) return;
  target.innerHTML = awardFactors.map((factor) => `
    <article class="factor-row ${escapeHtml(factor.type)}">
      <div>
        <strong>${escapeHtml(factor.label)}</strong>
        <p>${escapeHtml(factor.text)}</p>
      </div>
      <span>${factor.value}</span>
    </article>
  `).join("");
}

function renderActionSimulator() {
  const target = qs("#actionSimulator");
  if (!target) return;
  target.innerHTML = actionSimulations.map((action) => `
    <button class="action-card ${escapeHtml(action.type)}" type="button" data-action-id="${escapeHtml(action.id)}">
      <span>${escapeHtml(action.effect)}</span>
      <strong>${action.delta > 0 ? "+" : ""}${action.delta}</strong>
      <p>${escapeHtml(action.title)}</p>
    </button>
  `).join("");
  target.querySelectorAll("[data-action-id]").forEach((button) => {
    button.addEventListener("click", () => selectAction(button.dataset.actionId));
  });
  selectAction(actionSimulations[0].id);
}

function selectAction(id) {
  const action = actionSimulations.find((item) => item.id === id) || actionSimulations[0];
  const target = qs("#selectedActionInsight");
  if (!target) return;
  qs("#actionSimulator")?.querySelectorAll(".action-card").forEach((button) => {
    button.classList.toggle("selected", button.dataset.actionId === action.id);
  });
  target.className = `insight-box ${action.type}`;
  target.innerHTML = `
    <span>${escapeHtml(action.effect)}</span>
    <strong>${action.delta > 0 ? "+" : ""}${action.delta}점 예상</strong>
    <h3>${escapeHtml(action.title)}</h3>
    <p>${escapeHtml(action.reason)}</p>
    <dl>
      <dt>노력</dt><dd>${escapeHtml(action.effort)}</dd>
      <dt>담당</dt><dd>${escapeHtml(action.owner)}</dd>
    </dl>
  `;
}

function renderCodeGate() {
  renderReviewFocus();
  renderReviewChecklist();
  renderPreCommitChecklist();
  renderCommitGuidance("review");
  renderRollbackVersions();
  updateCodeGateStatus();
}

function renderReviewFocus() {
  const target = qs("#reviewFocus");
  if (!target) return;
  target.innerHTML = codeGate.reviewFocus.map((item) => `
    <article>
      <h3>${escapeHtml(item.title)}</h3>
      <p>${escapeHtml(item.text)}</p>
    </article>
  `).join("");
}

function renderChecklist(targetId, items) {
  const target = qs(targetId);
  if (!target) return;
  target.innerHTML = items.map((item, index) => `
    <label class="gate-check">
      <input type="checkbox" data-check-index="${index}" />
      <span>${escapeHtml(item)}</span>
    </label>
  `).join("");
  target.querySelectorAll("input").forEach((input) => {
    input.addEventListener("change", updateCodeGateStatus);
  });
}

function renderReviewChecklist() {
  renderChecklist("#reviewChecklist", codeGate.reviewChecklist);
}

function renderPreCommitChecklist() {
  renderChecklist("#preCommitChecklist", codeGate.preCommitChecklist);
}

function renderCommitGuidance(mode) {
  const target = qs("#commitGuidance");
  if (!target) return;
  const commands = mode === "rollback" ? codeGate.rollbackCommands : codeGate.commitCommands;
  const title = mode === "rollback" ? "철회 가이드" : "최종 커밋 명령 가이드";
  const note = mode === "rollback"
    ? "정적 페이지는 Git 철회를 실행하지 않습니다. 필요한 경우 터미널에서 안전한 revert를 사용하세요."
    : "아래 명령은 브라우저에서 실행되지 않는 안내입니다. 실제 실행은 터미널에서 진행하세요.";
  target.innerHTML = `
    <h3>${title}</h3>
    <p>${note}</p>
    <pre><code>${escapeHtml(commands.join("\n"))}</code></pre>
  `;
}

function renderRollbackVersions() {
  const target = qs("#rollbackVersions");
  if (!target) return;
  target.innerHTML = codeGate.rollbackVersions.map((version) => `
    <article>
      <span>위험 ${escapeHtml(version.risk)}</span>
      <h3>${escapeHtml(version.name)}</h3>
      <p>${escapeHtml(version.use)}</p>
    </article>
  `).join("");
}

function setGateState(key, value) {
  gateState[key] = value;
  if (key === "commitReady") renderCommitGuidance("commit");
  if (key === "rollbackOpen") renderCommitGuidance("rollback");
  updateCodeGateStatus();
}

function updateCodeGateStatus() {
  const target = qs("#codeGateStatus");
  if (!target) return;
  const reviewChecked = checkedCount("#reviewChecklist");
  const preCommitChecked = checkedCount("#preCommitChecklist");
  gateState.preCommitChecked = preCommitChecked === codeGate.preCommitChecklist.length;
  target.innerHTML = `
    <article class="${gateState.reviewRequested ? "done" : ""}">
      <strong>${gateState.reviewRequested ? "요청됨" : "대기"}</strong>
      <span>리뷰 요청</span>
    </article>
    <article class="${gateState.reviewCompleted ? "done" : ""}">
      <strong>${reviewChecked}/${codeGate.reviewChecklist.length}</strong>
      <span>리뷰 체크</span>
    </article>
    <article class="${gateState.preCommitChecked ? "done" : ""}">
      <strong>${preCommitChecked}/${codeGate.preCommitChecklist.length}</strong>
      <span>커밋 전 확인</span>
    </article>
    <article class="${gateState.commitReady ? "done" : ""}">
      <strong>${gateState.commitReady ? "준비" : "검토"}</strong>
      <span>최종 커밋</span>
    </article>
  `;
}

function checkedCount(selector) {
  const target = qs(selector);
  if (!target) return 0;
  return target.querySelectorAll("input:checked").length;
}

function bindCodeGateButtons() {
  document.querySelectorAll("[data-gate-action]").forEach((button) => {
    button.addEventListener("click", () => {
      const action = button.dataset.gateAction;
      if (action === "review-requested") setGateState("reviewRequested", true);
      if (action === "review-completed") setGateState("reviewCompleted", true);
      if (action === "commit-ready") setGateState("commitReady", true);
      if (action === "rollback-open") setGateState("rollbackOpen", true);
    });
  });
  document.querySelectorAll("[data-git-action]").forEach((button) => {
    button.addEventListener("click", () => runGitAction(button.dataset.gitAction));
  });
}

async function runGitAction(action) {
  const panel = qs("#gitBackendPanel");
  if (!panel) return;
  const remoteUrl = qs("#gitRemoteUrl")?.value || "https://github.com/jiyongKim615/grow_gate_local_ex.git";
  const message = qs("#gitCommitMessage")?.value || "feat: add team operations dashboards";
  panel.className = "git-backend-panel loading";
  panel.innerHTML = `<strong>실행 중</strong><p>${escapeHtml(action)} 요청을 백엔드로 보내는 중입니다.</p>`;
  try {
    const response = await fetch(buildGitUrl(action), buildGitOptions(action, remoteUrl, message));
    const payload = await response.json();
    renderGitResult(action, response.ok, payload);
  } catch (error) {
    panel.className = "git-backend-panel error";
    panel.innerHTML = `
      <strong>백엔드 연결 실패</strong>
      <p>FastAPI 서버가 실행 중인지 확인하세요. 기본 주소는 <code>http://127.0.0.1:8000</code> 입니다.</p>
      <pre><code>${escapeHtml(error.message)}</code></pre>
    `;
  }
}

function buildGitUrl(action) {
  if (action === "status") return "http://127.0.0.1:8000/git/status";
  if (action === "init") return "http://127.0.0.1:8000/git/init";
  if (action === "commit") return "http://127.0.0.1:8000/git/commit";
  if (action === "push") return "http://127.0.0.1:8000/git/push?branch=main";
  return "http://127.0.0.1:8000/git/status";
}

function buildGitOptions(action, remoteUrl, message) {
  const token = qs("#gitActionToken")?.value || "";
  const headers = { "Accept": "application/json" };
  if (token) headers["X-GrowGate-Git-Token"] = token;
  if (action === "status") return { method: "GET", headers: { "Accept": "application/json" } };
  if (action === "init") {
    return {
      method: "POST",
      headers: { ...headers, "Content-Type": "application/json" },
      body: JSON.stringify({ remote_url: remoteUrl, branch: "main" })
    };
  }
  if (action === "commit") {
    return {
      method: "POST",
      headers: { ...headers, "Content-Type": "application/json" },
      body: JSON.stringify({ message })
    };
  }
  return { method: "POST", headers };
}

function renderGitResult(action, httpOk, payload) {
  const panel = qs("#gitBackendPanel");
  if (!panel) return;
  const ok = Boolean(httpOk && payload.ok !== false);
  const steps = Array.isArray(payload.steps)
    ? payload.steps
    : [payload.status, payload.remote, payload].filter(Boolean);
  const statusText = ok ? "완료" : "확인 필요";
  const summary = payload.is_repo
    ? `브랜치 ${payload.branch || "unknown"} · ${payload.project_root || ""}`
    : (payload.hint || payload.detail || "백엔드 Git 요청 결과입니다.");
  const gitOps = payload.git_ops
    ? `<p class="git-ops-state">${payload.git_ops.enabled && payload.git_ops.token_configured ? "Git 실행 가능" : "Git 쓰기 잠김"} · ${escapeHtml(payload.git_ops.hint)}</p>`
    : "";
  panel.className = `git-backend-panel ${ok ? "success" : "error"}`;
  panel.innerHTML = `
    <strong>${escapeHtml(statusText)} · ${escapeHtml(action)}</strong>
    <p>${escapeHtml(summary)}</p>
    ${gitOps}
    <div class="git-step-list">
      ${steps.map(renderGitStep).join("")}
    </div>
  `;
}

function renderGitStep(step) {
  const command = Array.isArray(step.command) ? step.command.join(" ") : "";
  const output = [step.stdout, step.stderr].filter(Boolean).join("\n");
  return `
    <article class="${step.ok ? "success" : "error"}">
      <span>${step.ok ? "OK" : `ERR ${step.returncode || ""}`}</span>
      <code>${escapeHtml(command || step.action || "git")}</code>
      ${output ? `<pre><code>${escapeHtml(output)}</code></pre>` : ""}
    </article>
  `;
}

function renderDataCatalogPage() {
  renderCatalogOverview();
  renderCatalogCharts();
  renderCatalogList();
  const initialId = window.location.hash ? window.location.hash.slice(1) : dataSources[0].id;
  selectCatalogSpec(initialId);
}

function renderCatalogOverview() {
  const target = qs("#catalogOverview");
  if (!target) return;
  const active = dataSources.filter((source) => source.status === "활용 중").length;
  const high = dataSources.filter((source) => source.priority === "High").length;
  const api = dataSources.filter((source) => source.sourceType.toLowerCase().includes("api")).length;
  target.innerHTML = `
    <article><span>전체 후보</span><strong>${dataSources.length}</strong><p>수출 준비도 판단에 연결 가능한 데이터 소스</p></article>
    <article><span>활용 중</span><strong>${active}</strong><p>현재 데모 seed 또는 화면 근거에 반영</p></article>
    <article><span>우선 연계</span><strong>${high}</strong><p>공모전 설득력 상승 폭이 큰 출처</p></article>
    <article><span>API형</span><strong>${api}</strong><p>백엔드 커넥터로 자동화하기 쉬운 출처</p></article>
  `;
}

function renderCatalogCharts() {
  renderBarChart("#catalogCategoryChart", countBy(dataSources, "category"), "카테고리별 소스 수");
  renderBarChart("#catalogStatusChart", countBy(dataSources, "status"), "수집 상태별 분포");
  renderReliabilityChart();
}

function countBy(items, key) {
  return items.reduce((acc, item) => {
    acc[item[key]] = (acc[item[key]] || 0) + 1;
    return acc;
  }, {});
}

function renderBarChart(selector, counts, title) {
  const target = qs(selector);
  if (!target) return;
  const max = Math.max(...Object.values(counts));
  target.innerHTML = `
    <h3>${escapeHtml(title)}</h3>
    <div class="catalog-bars">
      ${Object.entries(counts).map(([label, value]) => `
        <div class="catalog-bar-row">
          <span>${escapeHtml(label)}</span>
          <div><i style="width:${Math.max(8, Math.round((value / max) * 100))}%"></i></div>
          <b>${value}</b>
        </div>
      `).join("")}
    </div>
  `;
}

function renderReliabilityChart() {
  const target = qs("#catalogReliabilityChart");
  if (!target) return;
  const buckets = {
    "80+": dataSources.filter((source) => source.reliability >= 80).length,
    "70-79": dataSources.filter((source) => source.reliability >= 70 && source.reliability < 80).length,
    "후보": dataSources.filter((source) => source.status === "수집 후보").length
  };
  renderBarChart("#catalogReliabilityChart", buckets, "신뢰도·후보 분포");
}

function renderCatalogList() {
  const target = qs("#catalogSpecList");
  if (!target) return;
  target.innerHTML = dataSources.map((source) => `
    <button type="button" data-catalog-id="${escapeHtml(source.id)}">
      <span>${escapeHtml(source.category)} · ${escapeHtml(source.status)}</span>
      <strong>${escapeHtml(source.name)}</strong>
      <small>${escapeHtml(source.provider)} / ${escapeHtml(source.format)}</small>
    </button>
  `).join("");
  target.querySelectorAll("[data-catalog-id]").forEach((button) => {
    button.addEventListener("click", () => selectCatalogSpec(button.dataset.catalogId));
  });
}

function selectCatalogSpec(id) {
  const source = dataSources.find((item) => item.id === id) || dataSources[0];
  const target = qs("#catalogSpecDetail");
  if (!target) return;
  qs("#catalogSpecList")?.querySelectorAll("[data-catalog-id]").forEach((button) => {
    button.classList.toggle("selected", button.dataset.catalogId === source.id);
  });
  history.replaceState(null, "", `#${source.id}`);
  target.innerHTML = `
    <div class="panel-heading horizontal">
      <div>
        <p class="eyebrow">SOURCE SPEC</p>
        <h2>${escapeHtml(source.name)}</h2>
      </div>
      <span class="badge">${escapeHtml(source.status)}</span>
    </div>
    <div class="spec-grid">
      <article>
        <span>원래 사이트</span>
        <a href="${escapeHtml(source.originUrl)}" target="_blank" rel="noreferrer">${escapeHtml(source.originUrl)}</a>
      </article>
      <article>
        <span>API·명세서</span>
        <a href="${escapeHtml(source.docsUrl)}" target="_blank" rel="noreferrer">${escapeHtml(source.docsUrl)}</a>
      </article>
      <article>
        <span>엔드포인트</span>
        <code>${escapeHtml(source.endpoint)}</code>
      </article>
      <article>
        <span>인증·포맷</span>
        <p>${escapeHtml(source.auth)} · ${escapeHtml(source.format)}</p>
      </article>
    </div>
    <section class="spec-narrative">
      <h3>수집 방식</h3>
      <p>${escapeHtml(source.collection)}</p>
      <h3>서비스 활용</h3>
      <ul>${source.usage.map((item) => `<li>${escapeHtml(item)}</li>`).join("")}</ul>
      <h3>데이터 형태</h3>
      ${renderDataShapeList(source.dataShape)}
    </section>
  `;
}

document.addEventListener("DOMContentLoaded", () => {
  const dashboard = document.body.dataset.dashboard;
  if (dashboard === "team") renderTeamDashboard();
  if (dashboard === "data-catalog") renderDataCatalogPage();
  if (dashboard === "code-gate") {
    renderCodeGate();
    bindCodeGateButtons();
  }
});
