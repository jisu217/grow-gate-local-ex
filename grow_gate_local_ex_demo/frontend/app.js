const DATA = {
  "metadata": {
    "service": "GROW-GATE Local EX",
    "version": "0.1.0-demo",
    "description": "시연용 로컬 데이터셋입니다. 실제 서비스에서는 산업부·무역·해외시장·시험인증·제품안전·특허·지원사업 공공데이터 API로 대체합니다.",
    "last_updated": "2026-05-15"
  },
  "markets": [
    {
      "code": "DE",
      "name": "Germany / EU",
      "region": "EU",
      "main_port": "Hamburg",
      "import_growth_yoy": 14.7,
      "tariff_mfn": 0.0,
      "logistics_days_air": "2-3",
      "logistics_days_sea": "28-35",
      "route_label": "EU Customs & SPC Check",
      "demand_tags": [
        "battery",
        "electronics",
        "industrial",
        "safety"
      ],
      "regulation_pressure": 0.92,
      "market_opportunity": 0.91
    },
    {
      "code": "US",
      "name": "United States",
      "region": "North America",
      "main_port": "Los Angeles",
      "import_growth_yoy": 11.3,
      "tariff_mfn": 2.7,
      "logistics_days_air": "2-4",
      "logistics_days_sea": "15-22",
      "route_label": "US CBP Clearance",
      "demand_tags": [
        "battery",
        "electronics",
        "consumer",
        "industrial"
      ],
      "regulation_pressure": 0.78,
      "market_opportunity": 0.88
    },
    {
      "code": "VN",
      "name": "Vietnam",
      "region": "ASEAN",
      "main_port": "Ho Chi Minh",
      "import_growth_yoy": 9.1,
      "tariff_mfn": 3.2,
      "logistics_days_air": "1-2",
      "logistics_days_sea": "5-7",
      "route_label": "ASEAN Customs & BTB Check",
      "demand_tags": [
        "electronics",
        "industrial",
        "component"
      ],
      "regulation_pressure": 0.55,
      "market_opportunity": 0.79
    },
    {
      "code": "TH",
      "name": "Thailand",
      "region": "ASEAN",
      "main_port": "Laem Chabang",
      "import_growth_yoy": 7.4,
      "tariff_mfn": 3.5,
      "logistics_days_air": "2-3",
      "logistics_days_sea": "6-8",
      "route_label": "ASEAN Customs",
      "demand_tags": [
        "battery",
        "consumer",
        "industrial"
      ],
      "regulation_pressure": 0.52,
      "market_opportunity": 0.72
    },
    {
      "code": "SG",
      "name": "Singapore",
      "region": "ASEAN",
      "main_port": "Singapore",
      "import_growth_yoy": 6.8,
      "tariff_mfn": 0.0,
      "logistics_days_air": "2-3",
      "logistics_days_sea": "7-10",
      "route_label": "ASEAN Hub",
      "demand_tags": [
        "electronics",
        "digital",
        "component"
      ],
      "regulation_pressure": 0.58,
      "market_opportunity": 0.74
    },
    {
      "code": "ID",
      "name": "Indonesia",
      "region": "ASEAN",
      "main_port": "Tanjung Priok",
      "import_growth_yoy": 8.6,
      "tariff_mfn": 4.0,
      "logistics_days_air": "2-4",
      "logistics_days_sea": "8-12",
      "route_label": "ASEAN Import Check",
      "demand_tags": [
        "consumer",
        "industrial",
        "battery"
      ],
      "regulation_pressure": 0.5,
      "market_opportunity": 0.71
    },
    {
      "code": "CN",
      "name": "China",
      "region": "East Asia",
      "main_port": "Shanghai",
      "import_growth_yoy": 5.2,
      "tariff_mfn": 4.5,
      "logistics_days_air": "1-2",
      "logistics_days_sea": "3-5",
      "route_label": "CCC / China Customs",
      "demand_tags": [
        "electronics",
        "battery",
        "industrial"
      ],
      "regulation_pressure": 0.72,
      "market_opportunity": 0.68
    }
  ],
  "regulations": [
    {
      "id": "CE-LVD-EMC",
      "title": "CE / LVD / EMC 적합성 검토",
      "scope": "EU 전기전자 제품",
      "triggers": {
        "regions": [
          "EU"
        ],
        "features": [
          "electronics",
          "digital"
        ],
        "hs_prefixes": [
          "84",
          "85",
          "90"
        ]
      },
      "priority": "high",
      "typical_evidence": [
        "제품 사양서",
        "회로도",
        "시험성적서",
        "DoC",
        "사용설명서"
      ],
      "action": "대상 지침을 분류하고 시험기관 견적·일정을 먼저 확보합니다."
    },
    {
      "id": "UN38.3",
      "title": "UN38.3 리튬전지 운송시험",
      "scope": "리튬이온 셀·팩·장착 제품 국제 운송",
      "triggers": {
        "regions": [
          "EU",
          "North America",
          "ASEAN",
          "East Asia"
        ],
        "features": [
          "battery"
        ],
        "hs_prefixes": [
          "8507"
        ]
      },
      "priority": "critical",
      "typical_evidence": [
        "셀/팩 정보",
        "UN38.3 시험성적서",
        "MSDS",
        "운송 라벨",
        "포장 사양"
      ],
      "action": "항공·해상 운송 전 UN38.3, MSDS, 포장·라벨 자료를 확정합니다."
    },
    {
      "id": "IEC62133",
      "title": "IEC 62133 배터리 안전시험",
      "scope": "휴대기기용 리튬이온 배터리 안전",
      "triggers": {
        "regions": [
          "EU",
          "North America",
          "ASEAN"
        ],
        "features": [
          "battery"
        ],
        "hs_prefixes": [
          "8507"
        ]
      },
      "priority": "high",
      "typical_evidence": [
        "셀 인증서",
        "팩 회로 보호 설계",
        "BMS 사양",
        "충방전 시험 데이터"
      ],
      "action": "셀 인증서와 팩 보호회로 자료를 수집하고 갭 시험 항목을 산정합니다."
    },
    {
      "id": "EU-BATTERY-PASSPORT",
      "title": "EU 배터리 여권 준비도",
      "scope": "배터리팩, ESS, 전기차·산업용 배터리 등",
      "triggers": {
        "regions": [
          "EU"
        ],
        "features": [
          "battery"
        ],
        "hs_prefixes": [
          "8507"
        ]
      },
      "priority": "high",
      "typical_evidence": [
        "배터리 모델·용량",
        "셀 공급망",
        "재활용 원료",
        "탄소발자국",
        "QR 식별체계"
      ],
      "action": "여권 데이터 항목을 공급망 단위로 나누고 누락 데이터를 표준 필드로 관리합니다."
    },
    {
      "id": "GPSR",
      "title": "EU 일반제품안전규정(GPSR) 체크",
      "scope": "EU 소비재·온라인 판매 제품",
      "triggers": {
        "regions": [
          "EU"
        ],
        "features": [
          "consumer",
          "ecommerce"
        ],
        "hs_prefixes": [
          "84",
          "85",
          "95"
        ]
      },
      "priority": "medium",
      "typical_evidence": [
        "EU 책임 경제운영자",
        "위험성 평가",
        "사용자 경고문",
        "추적성 정보",
        "라벨"
      ],
      "action": "책임자·추적성·위험성 평가·라벨 표시 항목을 상품페이지와 함께 점검합니다."
    },
    {
      "id": "CBAM",
      "title": "CBAM 탄소국경조정 대상 여부",
      "scope": "철강·알루미늄·시멘트·비료·수소·전력 등 일부 품목",
      "triggers": {
        "regions": [
          "EU"
        ],
        "features": [
          "carbon_material"
        ],
        "hs_prefixes": [
          "72",
          "73",
          "76",
          "25",
          "28",
          "31"
        ]
      },
      "priority": "medium",
      "typical_evidence": [
        "HS Code",
        "원산지",
        "내재배출량",
        "공정정보",
        "공급사 데이터"
      ],
      "action": "CBAM 해당 HS 여부를 먼저 검토하고 공급사 배출량 자료를 수집합니다."
    },
    {
      "id": "CRA",
      "title": "EU 사이버보안 규제(CRA) 준비도",
      "scope": "디지털 요소가 있는 하드웨어·소프트웨어 제품",
      "triggers": {
        "regions": [
          "EU"
        ],
        "features": [
          "digital",
          "iot",
          "software"
        ],
        "hs_prefixes": [
          "84",
          "85"
        ]
      },
      "priority": "medium",
      "typical_evidence": [
        "SBOM",
        "취약점 대응 정책",
        "보안 업데이트 기간",
        "사고 보고 프로세스"
      ],
      "action": "SBOM, 취약점 모니터링, 보안 업데이트 정책을 인증 준비 문서에 포함합니다."
    },
    {
      "id": "UL-FCC",
      "title": "미국 UL / FCC 후보 검토",
      "scope": "미국 전기전자·무선 제품",
      "triggers": {
        "regions": [
          "North America"
        ],
        "features": [
          "electronics",
          "digital",
          "iot"
        ],
        "hs_prefixes": [
          "84",
          "85",
          "90"
        ]
      },
      "priority": "high",
      "typical_evidence": [
        "전기안전 시험성적서",
        "무선 모듈 정보",
        "사용설명서",
        "제품 라벨"
      ],
      "action": "UL 안전성 여부와 FCC 무선·전자파 요건을 제품 기능별로 분리해 검토합니다."
    },
    {
      "id": "ASEAN-SAFETY",
      "title": "ASEAN 전기안전·통관 적합성 체크",
      "scope": "동남아 전기전자·소비재 수출",
      "triggers": {
        "regions": [
          "ASEAN"
        ],
        "features": [
          "electronics",
          "consumer"
        ],
        "hs_prefixes": [
          "84",
          "85"
        ]
      },
      "priority": "medium",
      "typical_evidence": [
        "인보이스",
        "원산지증명",
        "시험성적서",
        "라벨",
        "현지 수입자 정보"
      ],
      "action": "국가별 수입자 등록·라벨·통관 서류 요건을 먼저 확인합니다."
    }
  ],
  "recalls": [
    {
      "id": "R-001",
      "product": "리튬이온 보조배터리",
      "category": "battery",
      "keywords": [
        "리튬",
        "배터리",
        "충전",
        "과열",
        "화재",
        "pack",
        "cell"
      ],
      "hazard": "과충전 보호회로 미흡에 따른 과열·발화 위험",
      "severity": 0.92,
      "recommended_tests": [
        "과충전 시험",
        "단락 시험",
        "온도상승 시험",
        "BMS 보호회로 검증"
      ],
      "design_actions": [
        "셀 공급사 인증서 검증",
        "PCM/BMS 이중 보호",
        "충전 전류 제한값 문서화"
      ]
    },
    {
      "id": "R-002",
      "product": "전동공구 배터리팩",
      "category": "battery",
      "keywords": [
        "전동공구",
        "배터리팩",
        "충격",
        "낙하",
        "과열",
        "charger"
      ],
      "hazard": "낙하 충격 후 내부 단락 및 충전 중 온도상승 위험",
      "severity": 0.88,
      "recommended_tests": [
        "낙하 시험",
        "진동 시험",
        "충격 시험",
        "충방전 내구 시험"
      ],
      "design_actions": [
        "셀 홀더 강화",
        "절연 시트 추가",
        "충격 후 보호회로 동작 확인"
      ]
    },
    {
      "id": "R-003",
      "product": "휴대용 전자기기 충전기",
      "category": "electronics",
      "keywords": [
        "충전기",
        "어댑터",
        "절연",
        "감전",
        "과열",
        "전자파"
      ],
      "hazard": "절연거리 부족 및 부품 정격 미달에 따른 감전·화재 위험",
      "severity": 0.74,
      "recommended_tests": [
        "절연내력 시험",
        "누설전류 시험",
        "온도상승 시험",
        "EMC 시험"
      ],
      "design_actions": [
        "절연거리 재검토",
        "퓨즈·서미스터 정격 확인",
        "인쇄회로 패턴 간격 확대"
      ]
    },
    {
      "id": "R-004",
      "product": "IoT 센서 장치",
      "category": "iot",
      "keywords": [
        "iot",
        "센서",
        "무선",
        "펌웨어",
        "취약점",
        "업데이트"
      ],
      "hazard": "기본 비밀번호·취약 펌웨어로 인한 원격 침해 가능성",
      "severity": 0.67,
      "recommended_tests": [
        "기본계정 점검",
        "펌웨어 무결성 검증",
        "통신 암호화 시험"
      ],
      "design_actions": [
        "초기 비밀번호 정책",
        "서명된 업데이트",
        "취약점 접수·패치 프로세스"
      ]
    },
    {
      "id": "R-005",
      "product": "생활용품 플라스틱 부품",
      "category": "consumer",
      "keywords": [
        "플라스틱",
        "생활용품",
        "파손",
        "날카로운",
        "어린이"
      ],
      "hazard": "파손 시 날카로운 모서리 발생 및 삼킴 위험",
      "severity": 0.54,
      "recommended_tests": [
        "인장 시험",
        "낙하 시험",
        "모서리 안전성",
        "소형부품 시험"
      ],
      "design_actions": [
        "모서리 라운딩",
        "재질 취성 개선",
        "경고문·연령 표시"
      ]
    }
  ],
  "patents": [
    {
      "keyword": "lithium battery pack",
      "korean_keyword": "리튬 배터리팩",
      "ipc": [
        "H01M",
        "B60L"
      ],
      "filings": 12840,
      "recent_growth": 0.19,
      "risk": 0.78,
      "avoidance_hints": [
        "BMS 알고리즘 청구항 회피 검토",
        "셀 밸런싱 구조 차별화",
        "방열 구조 선행문헌 조사"
      ]
    },
    {
      "keyword": "battery management system",
      "korean_keyword": "배터리 관리 시스템",
      "ipc": [
        "H02J",
        "H01M"
      ],
      "filings": 9630,
      "recent_growth": 0.22,
      "risk": 0.81,
      "avoidance_hints": [
        "충전 제어 로직 비교",
        "SOC 추정 알고리즘 검토",
        "펌웨어 업데이트 방식 차별화"
      ]
    },
    {
      "keyword": "portable power station",
      "korean_keyword": "휴대용 전원장치",
      "ipc": [
        "H02J",
        "H01M"
      ],
      "filings": 3210,
      "recent_growth": 0.28,
      "risk": 0.64,
      "avoidance_hints": [
        "인버터·배터리 결합 구조 검토",
        "열관리 구조 비교",
        "사용자 인터페이스 디자인권 확인"
      ]
    },
    {
      "keyword": "industrial sensor gateway",
      "korean_keyword": "산업용 센서 게이트웨이",
      "ipc": [
        "G05B",
        "H04L"
      ],
      "filings": 4200,
      "recent_growth": 0.17,
      "risk": 0.59,
      "avoidance_hints": [
        "통신 프로토콜 특허 확인",
        "데이터 수집·전송 구조 회피",
        "보안 업데이트 청구항 검토"
      ]
    }
  ],
  "support_programs": [
    {
      "id": "P-001",
      "name": "수출바우처 맞춤형 해외진출 지원",
      "fit_tags": [
        "export",
        "marketing",
        "certification",
        "regional_sme"
      ],
      "benefit": "해외마케팅, 인증, 컨설팅, 디자인 등 패키지 지원",
      "priority_rule": "수출 초기·성장 단계 기업 우선 검토",
      "roadmap_stage": "30days"
    },
    {
      "id": "P-002",
      "name": "해외규격인증획득지원사업",
      "fit_tags": [
        "certification",
        "CE",
        "UL",
        "IEC",
        "SME"
      ],
      "benefit": "해외 인증 시험·컨설팅 비용 일부 지원",
      "priority_rule": "인증 미보유 또는 신규 국가 진출 기업에 적합",
      "roadmap_stage": "30days"
    },
    {
      "id": "P-003",
      "name": "KOTRA 해외시장조사·지사화 연계",
      "fit_tags": [
        "market",
        "buyer",
        "export"
      ],
      "benefit": "해외시장 조사, 바이어 발굴, 현지 네트워크 연계",
      "priority_rule": "대상국과 품목이 확정된 기업에 적합",
      "roadmap_stage": "60days"
    },
    {
      "id": "P-004",
      "name": "테크노파크 제품고도화·시험분석 지원",
      "fit_tags": [
        "regional_sme",
        "testing",
        "prototype",
        "quality"
      ],
      "benefit": "시제품 개선, 시험분석, 전문가 컨설팅 지원",
      "priority_rule": "지역 주력산업·산단 입주기업에 적합",
      "roadmap_stage": "30days"
    },
    {
      "id": "P-005",
      "name": "중소기업 기술보호·특허 전략 컨설팅",
      "fit_tags": [
        "patent",
        "FTO",
        "technology"
      ],
      "benefit": "선행기술 조사, 회피설계, 권리화 전략 상담",
      "priority_rule": "기술 기반 제조기업·해외 FTO 위험이 있는 기업에 적합",
      "roadmap_stage": "60days"
    },
    {
      "id": "P-006",
      "name": "스마트공장·품질혁신 연계 지원",
      "fit_tags": [
        "quality",
        "traceability",
        "manufacturing"
      ],
      "benefit": "품질 데이터 수집, 추적성, 공정 개선 지원",
      "priority_rule": "리콜 위험·품질 데이터 보완이 필요한 제조기업에 적합",
      "roadmap_stage": "90days"
    }
  ],
  "regions": [
    {
      "name": "경남",
      "industry_focus": [
        "기계",
        "전기전자",
        "항공",
        "조선",
        "배터리부품"
      ],
      "weight": 1.06
    },
    {
      "name": "대구",
      "industry_focus": [
        "로봇",
        "전기차부품",
        "섬유",
        "의료기기"
      ],
      "weight": 1.03
    },
    {
      "name": "울산",
      "industry_focus": [
        "자동차",
        "화학",
        "에너지",
        "배터리"
      ],
      "weight": 1.05
    },
    {
      "name": "충북",
      "industry_focus": [
        "이차전지",
        "바이오",
        "반도체",
        "화장품"
      ],
      "weight": 1.04
    },
    {
      "name": "전북",
      "industry_focus": [
        "농기계",
        "상용차",
        "탄소소재",
        "식품"
      ],
      "weight": 1.02
    },
    {
      "name": "부산",
      "industry_focus": [
        "물류",
        "기계부품",
        "해양",
        "소비재"
      ],
      "weight": 1.04
    }
  ]
};

const API_BASE = "http://127.0.0.1:8000";
const PUBLIC_DATA_SOURCE_FALLBACK = [
  {
    source_id: "kcs_trade_gw",
    screen_ids: ["S02", "S04", "S09"],
    connector: "trade",
    name: "관세청_수출입무역통계 GW",
    provider: "Korea Customs Service",
    official_url: "https://www.data.go.kr/en/data/15102108/openapi.do",
    target_table: "trade_signal",
    update_cycle: "monthly",
    use_in_product: "HS Code와 대상국 기준 수출입 추세, 관세/시장성 신호 산출",
  },
  {
    source_id: "kotra_supply_chain",
    screen_ids: ["S02", "S04", "S08", "S09"],
    connector: "country",
    name: "대한무역투자진흥공사_글로벌 공급망 인사이트",
    provider: "KOTRA",
    official_url: "https://www.data.go.kr/data/15122648/openapi.do",
    target_table: "country_profile",
    update_cycle: "ad-hoc",
    use_in_product: "대상국 공급망 이슈, 정책대응, 산업전망을 시장성 근거와 로드맵 메모에 반영",
  },
  {
    source_id: "safety_korea_cert_recall",
    screen_ids: ["S02", "S05", "S06", "S08", "S09"],
    connector: "cert_recall",
    name: "국가기술표준원_제품 안전인증 및 리콜 정보",
    provider: "KATS Safety Korea",
    official_url: "https://www.data.go.kr/data/15116894/openapi.do",
    target_table: "recall_case",
    update_cycle: "near-real-time",
    use_in_product: "국내외 인증·리콜 사례를 인증 후보와 리콜 위험 Copilot에 반영",
  },
  {
    source_id: "kipris_plus_patent",
    screen_ids: ["S02", "S07", "S08", "S09"],
    connector: "patent",
    name: "특허청 KIPRIS Plus Open API",
    provider: "KIPRIS Plus",
    official_url: "https://plus.kipris.or.kr/eng/main/contents.do?menuNo=300024",
    target_table: "patent_signal",
    update_cycle: "daily",
    use_in_product: "BMS, 보호회로, 열관리 키워드의 출원 밀집도와 FTO 1차 위험지도 산출",
  },
  {
    source_id: "export_voucher_support",
    screen_ids: ["S02", "S07", "S08", "S09"],
    connector: "support",
    name: "수출바우처·해외규격인증·지역TP 지원사업 공고",
    provider: "MOTIE/MSS/KOTRA/Regional TP",
    official_url: "https://www.exportvoucher.com/",
    target_table: "support_program",
    update_cycle: "weekly",
    use_in_product: "기업 지역, 인증 갭, 시험 필요도, FTO 위험을 지원사업 신청 우선순위로 변환",
  },
];
let lastResult = null;

const $ = (selector) => document.querySelector(selector);
const $$ = (selector) => Array.from(document.querySelectorAll(selector));

function clamp(value, low = 0, high = 100) {
  return Math.max(low, Math.min(high, value));
}

function normalizeHs(hsCode) {
  return String(hsCode || "").replace(/\D/g, "");
}

function tokenize(text) {
  const matches = String(text || "").toLowerCase().match(/[a-z0-9가-힣]+/g);
  return matches ? matches : [];
}

function tokenSimilarity(a, b) {
  const aSet = new Set(tokenize(a));
  const bSet = new Set(tokenize(b));
  if (!aSet.size || !bSet.size) return 0;
  const union = new Set([...aSet, ...bSet]);
  let intersection = 0;
  aSet.forEach((token) => { if (bSet.has(token)) intersection += 1; });
  let bonus = 0;
  const aLower = String(a || "").toLowerCase();
  const bLower = String(b || "").toLowerCase();
  aSet.forEach((token) => { if (token.length >= 3 && bLower.includes(token)) bonus += 0.04; });
  bSet.forEach((token) => { if (token.length >= 3 && aLower.includes(token)) bonus += 0.04; });
  return Math.min(1, intersection / union.size + bonus);
}

function matchTags(features, tags) {
  const f = new Set((features || []).map((x) => x.toLowerCase()));
  const t = new Set((tags || []).map((x) => x.toLowerCase()));
  if (!f.size || !t.size) return 0;
  const union = new Set([...f, ...t]);
  let hit = 0;
  f.forEach((x) => { if (t.has(x)) hit += 1; });
  return hit / union.size;
}

function hsPrefixMatches(hsCode, prefixes) {
  const hs = normalizeHs(hsCode);
  return (prefixes || []).some((prefix) => hs.startsWith(prefix));
}

function findTargetMarket(targetMarket) {
  const query = String(targetMarket || "").toLowerCase();
  return DATA.markets.find((market) =>
    market.name.toLowerCase().includes(query) ||
    query.includes(market.name.toLowerCase()) ||
    market.code.toLowerCase() === query ||
    market.region.toLowerCase() === query
  ) || DATA.markets[0];
}

function findRegionProfile(regionName) {
  return DATA.regions.find((region) => region.name === regionName) || { name: regionName, industry_focus: [], weight: 1 };
}

function scoreMarket(market, features, regionWeight, preferredName) {
  const growth = clamp(Number(market.import_growth_yoy || 0) * 3.2, 0, 45);
  const tariff = clamp(20 - Number(market.tariff_mfn || 0) * 2.2, 0, 20);
  const demand = matchTags(features, market.demand_tags) * 25;
  const base = Number(market.market_opportunity || 0.5) * 25;
  const regionBonus = preferredName && market.name.toLowerCase().includes(preferredName.toLowerCase()) ? 8 : 0;
  const score = Math.round(clamp((growth + tariff + demand + base + regionBonus) * regionWeight, 1, 99));
  const reason = `수입 증가율 ${market.import_growth_yoy}%, MFN 관세 ${market.tariff_mfn}%, 제품 태그 적합도 ${matchTags(features, market.demand_tags).toFixed(2)}를 반영했습니다.`;
  return [score, reason];
}

function selectRegulations(regulations, targetRegion, hsCode, features) {
  const featureSet = new Set((features || []).map((x) => x.toLowerCase()));
  const priorityWeight = { critical: 100, high: 75, medium: 50, low: 25 };
  return regulations
    .map((item) => {
      const trigger = item.triggers || {};
      const regionHit = (trigger.regions || []).includes(targetRegion) || (trigger.regions || []).includes("ALL");
      const triggerFeatures = new Set((trigger.features || []).map((x) => x.toLowerCase()));
      let featureHitCount = 0;
      featureSet.forEach((x) => { if (triggerFeatures.has(x)) featureHitCount += 1; });
      const hsHit = hsPrefixMatches(hsCode, trigger.hs_prefixes || []);
      if (!(regionHit && (featureHitCount > 0 || hsHit))) return null;
      const parts = [];
      if (regionHit) parts.push(`대상 권역 ${targetRegion} 일치`);
      if (featureHitCount) parts.push(`제품 특성 ${featureHitCount}개 일치`);
      if (hsHit) parts.push("HS Code 접두어 일치");
      return {
        ...item,
        matched_reason: parts.join(", "),
        _score: (priorityWeight[item.priority] || 50) + featureHitCount * 12 + (hsHit ? 18 : 0),
      };
    })
    .filter(Boolean)
    .sort((a, b) => b._score - a._score)
    .slice(0, 8)
    .map(({ _score, ...item }) => item);
}

function scoreRecallRisks(recalls, productName, features) {
  const query = `${productName} ${features.join(" ")}`;
  return recalls
    .map((recall) => {
      const text = `${recall.product} ${recall.hazard} ${(recall.keywords || []).join(" ")} ${recall.category}`;
      const similarity = tokenSimilarity(query, text);
      const featureOverlap = matchTags(features, [recall.category]);
      const riskScore = Math.min(1, similarity * 0.62 + featureOverlap * 0.28 + Number(recall.severity || 0) * 0.1);
      const risk_level = riskScore >= 0.68 ? "HIGH" : riskScore >= 0.38 ? "MEDIUM" : "LOW";
      return { ...recall, similarity: round(similarity, 3), risk_score: round(riskScore, 3), risk_level };
    })
    .sort((a, b) => b.risk_score - a.risk_score)
    .slice(0, 3);
}

function scorePatentRisks(patents, productName, features) {
  const query = `${productName} ${features.join(" ")}`;
  return patents
    .map((patent) => {
      const text = `${patent.keyword} ${patent.korean_keyword} ${(patent.ipc || []).join(" ")}`;
      const similarity = tokenSimilarity(query, text);
      const filingsNorm = Math.min(1, Math.log10(Math.max(1, patent.filings || 1)) / 5);
      const score = Math.min(1, similarity * 0.45 + Number(patent.risk || 0) * 0.32 + Number(patent.recent_growth || 0) * 0.13 + filingsNorm * 0.1);
      const risk_level = score >= 0.68 ? "HIGH" : score >= 0.42 ? "MEDIUM" : "LOW";
      return { ...patent, similarity: round(similarity, 3), risk_score: round(score, 3), risk: round(score, 3), risk_level };
    })
    .sort((a, b) => b.risk_score - a.risk_score)
    .slice(0, 3);
}

function scoreSupportPrograms(programs, features, regulations, recalls, patents) {
  const needs = new Set([...features, "export", "regional_sme"]);
  if (regulations.length) needs.add("certification");
  if (recalls.some((item) => ["MEDIUM", "HIGH"].includes(item.risk_level))) { needs.add("testing"); needs.add("quality"); }
  if (patents.some((item) => ["MEDIUM", "HIGH"].includes(item.risk_level))) { needs.add("patent"); needs.add("FTO"); needs.add("technology"); }
  const lowerNeeds = new Set([...needs].map((x) => x.toLowerCase()));
  return programs
    .map((program) => {
      const tags = (program.fit_tags || []).map((x) => x.toLowerCase());
      const overlap = tags.filter((tag) => lowerNeeds.has(tag)).length;
      const score = Math.round(clamp(45 + overlap * 13 + (program.roadmap_stage === "30days" ? 6 : 0), 30, 99));
      return {
        ...program,
        fit_score: score,
        reason: `현재 갭 태그(${[...needs].sort().join(", ")})와 지원사업 태그 ${overlap}개가 일치합니다.`,
      };
    })
    .sort((a, b) => b.fit_score - a.fit_score)
    .slice(0, 5);
}

function round(value, digits = 0) {
  const factor = Math.pow(10, digits);
  return Math.round(value * factor) / factor;
}

function scoreReadiness(marketScores, regulations, recalls, patents, supports, request) {
  const market = Math.round(clamp(marketScores.slice(0, 3).reduce((a, b) => a + b, 0) / Math.max(1, Math.min(3, marketScores.length)), 0, 100));
  const certCoverage = Math.min(100, (request.certifications || []).length * 15 + (request.has_test_report ? 20 : 0));
  const doc = Math.round(clamp(20 + (request.has_bom ? 25 : 0) + (request.has_test_report ? 25 : 0) + (request.has_label_draft ? 20 : 0), 0, 100));
  const support = Math.round(clamp(supports.slice(0, 3).reduce((acc, p) => acc + (p.fit_score || 0), 0) / Math.max(1, Math.min(3, supports.length)), 0, 100));
  const criticalRegs = regulations.filter((r) => r.priority === "critical").length;
  const highRegs = regulations.filter((r) => r.priority === "high").length;
  const regPenalty = criticalRegs * 11 + highRegs * 6 + Math.max(0, regulations.length - (request.certifications || []).length) * 2;
  const regulation = Math.round(clamp(100 - regPenalty + certCoverage * 0.25, 0, 100));
  const recallPenalty = recalls.reduce((acc, r) => acc + (r.risk_level === "HIGH" ? 28 : r.risk_level === "MEDIUM" ? 13 : 4), 0);
  const recall = Math.round(clamp(100 - recallPenalty, 0, 100));
  const patentPenalty = patents.reduce((acc, p) => acc + (p.risk_level === "HIGH" ? 24 : p.risk_level === "MEDIUM" ? 11 : 3), 0);
  const patent = Math.round(clamp(100 - patentPenalty, 0, 100));
  const score = Math.round(clamp(market * 0.24 + regulation * 0.24 + recall * 0.14 + patent * 0.12 + support * 0.12 + doc * 0.14, 0, 100));
  return [score, {
    "시장성": market,
    "규제·인증": regulation,
    "리콜·품질": recall,
    "특허·FTO": patent,
    "지원사업 적합도": support,
    "문서 준비도": doc,
  }];
}

function buildLocalMlPredictions(request, markets, regulations, recalls, patents, supports, breakdown) {
  const market = Number(breakdown["시장성"] || 0);
  const regulation = Number(breakdown["규제·인증"] || 0);
  const recallRisk = 100 - Number(breakdown["리콜·품질"] || 0);
  const patentRisk = 100 - Number(breakdown["특허·FTO"] || 0);
  const support = Number(breakdown["지원사업 적합도"] || 0);
  const docs = Number(breakdown["문서 준비도"] || 0);
  const critical = regulations.filter((item) => item.priority === "critical").length;
  const eu = /EU|Germany/i.test(request.target_market) ? 1 : 0;
  const battery = (request.features || []).includes("battery") ? 1 : 0;
  const success = clamp(34 + market * 0.34 + support * 0.16 + docs * 0.2 + regulation * 0.12 - recallRisk * 0.12 - patentRisk * 0.08 - critical * 4 - eu * 2);
  const lead = clamp(24 + (100 - regulation) * 0.42 + critical * 14 + battery * 8 + eu * 7 - docs * 0.16, 21, 140);
  const supportProb = clamp(30 + support * 0.48 + market * 0.12 + (100 - regulation) * 0.08 + battery * 4);
  const reserve = clamp(5 + recallRisk * 0.07 + patentRisk * 0.08 + (100 - regulation) * 0.08 + critical * 2.2 + battery * 1.6, 4, 32);
  const baseImportance = [
    { feature: "시장성 점수", importance: 0.24, direction: "positive" },
    { feature: "규제 압력", importance: 0.22, direction: "negative" },
    { feature: "문서 준비도", importance: 0.18, direction: "positive" },
    { feature: "지원사업 적합도", importance: 0.15, direction: "positive" },
    { feature: "리콜 위험도", importance: 0.12, direction: "negative" },
  ];
  return [
    {
      model_name: "XGBoost Export Success Classifier",
      model_type: "XGBClassifier",
      target: "90일 파일럿 수출 실행 가능성",
      value: round(success, 1),
      unit: "%",
      confidence: 0.82,
      interpretation: "시장성, 규제압력, 문서 준비도를 종합해 90일 내 파일럿 수출 패키지 완성 가능성을 추정합니다.",
      feature_importance: baseImportance,
    },
    {
      model_name: "XGBoost Certification Lead-time Regressor",
      model_type: "XGBRegressor",
      target: "인증·시험 리드타임 추정",
      value: Math.round(lead),
      unit: "days",
      confidence: 0.78,
      interpretation: "긴급 규제 수, 배터리 여부, 문서 준비도를 기준으로 시험기관 상담부터 보완 완료까지의 기간을 추정합니다.",
      feature_importance: baseImportance,
    },
    {
      model_name: "XGBoost Support Fit Classifier",
      model_type: "XGBClassifier",
      target: "지원사업 선정 적합 확률",
      value: round(supportProb, 1),
      unit: "%",
      confidence: 0.8,
      interpretation: "인증 갭, 지역 제조기업 태그, 지원사업 매칭 점수로 신청 우선순위를 예측합니다.",
      feature_importance: baseImportance,
    },
    {
      model_name: "XGBoost Risk Reserve Regressor",
      model_type: "XGBRegressor",
      target: "시험·보완 예비비율 추정",
      value: round(reserve, 1),
      unit: "%",
      confidence: 0.74,
      interpretation: "리콜·FTO·규제 위험을 반영해 시험 실패, 라벨 보완, 회피설계 상담에 필요한 예비비율을 추정합니다.",
      feature_importance: baseImportance,
    },
  ];
}

function readinessLevel(score) {
  if (score >= 78) return "HIGH";
  if (score >= 55) return "MEDIUM";
  return "LOW";
}

function buildRoadmap(request, regulations, recalls, patents, supports) {
  const criticalRegs = regulations.filter((r) => ["critical", "high"].includes(r.priority));
  const regTitles = (criticalRegs.length ? criticalRegs : regulations).slice(0, 3).map((r) => r.title);
  const tests = [...new Set(recalls.flatMap((r) => r.recommended_tests || []))];
  const supportNames = supports.filter((p) => p.roadmap_stage === "30days").slice(0, 2).map((p) => p.name);
  const patentHints = [...new Set(patents.flatMap((p) => p.avoidance_hints || []))];
  return [
    {
      day_bucket: "30일",
      title: "제품·규제 갭 진단과 지원사업 선신청",
      tasks: [
        `${request.product_name} 사양서, BOM, HS Code 근거자료를 한 폴더로 정리`,
        `우선 인증 후보: ${regTitles.join(", ") || "대상국 규제 확인"}`,
        `시험 전 체크리스트: ${tests.slice(0, 4).join(", ") || "시험항목 확인"}`,
        `추천 지원사업 신청 검토: ${(supportNames.length ? supportNames : [supports[0]?.name || "지역 수출지원사업"]).join(", ")}`,
      ],
      output: "수출 준비도 진단서 v1, 인증·시험 견적요청서, 지원사업 신청 초안",
      owner: "품질·인증 담당자 / 해외영업 담당자",
    },
    {
      day_bucket: "60일",
      title: "시험·인증 실행과 바이어 검증",
      tasks: [
        "시험기관 상담 후 필수 시험·선택 시험·샘플 수량을 확정",
        "대상국 라벨, 사용설명서, DoC/기술문서 초안을 작성",
        "KOTRA·현지 네트워크로 바이어 요구 인증과 포장 조건을 확인",
        `특허/FTO 1차 검토: ${patentHints.slice(0, 3).join(", ") || "선행기술 키워드 확장"}`,
      ],
      output: "시험 신청서, 기술문서 패키지 초안, 바이어 검증 메모, FTO 키워드 맵",
      owner: "인증 담당자 / 기술개발 담당자",
    },
    {
      day_bucket: "90일",
      title: "시장 테스트와 스케일 수출 준비",
      tasks: [
        "시험 결과 보완사항을 설계·라벨·포장에 반영",
        "리콜 위험 기반 FMEA와 출하 전 품질 체크리스트를 운영",
        "수출단가, 물류 리드타임, 통관 서류, A/S 정책을 견적서에 반영",
        "성공 가능성이 높은 1개 국가부터 파일럿 주문·샘플 발송을 진행",
      ],
      output: "수출 실행 패키지, FMEA, 파일럿 바이어 제안서, 2차 지원사업 계획",
      owner: "대표이사 / 해외영업 / 품질책임자",
    },
  ];
}

function analyzeLocal(request) {
  const target = findTargetMarket(request.target_market);
  const regionProfile = findRegionProfile(request.region);
  const markets = DATA.markets.map((market) => {
    const [score, reason] = scoreMarket(market, request.features, Number(regionProfile.weight || 1), target.name);
    return {
      code: market.code,
      name: market.name,
      region: market.region,
      main_port: market.main_port,
      import_growth_yoy: market.import_growth_yoy,
      tariff_mfn: market.tariff_mfn,
      logistics_days_air: market.logistics_days_air,
      logistics_days_sea: market.logistics_days_sea,
      route_label: market.route_label,
      opportunity_score: score,
      reason,
    };
  }).sort((a, b) => b.opportunity_score - a.opportunity_score).slice(0, 5);
  const regulations = selectRegulations(DATA.regulations, target.region, request.hs_code, request.features);
  const recalls = scoreRecallRisks(DATA.recalls, request.product_name, request.features);
  const patents = scorePatentRisks(DATA.patents, request.product_name, request.features);
  const support_programs = scoreSupportPrograms(DATA.support_programs, request.features, regulations, recalls, patents);
  const roadmap = buildRoadmap(request, regulations, recalls, patents, support_programs);
  const [readiness_score, score_breakdown] = scoreReadiness(markets.map((m) => m.opportunity_score), regulations, recalls, patents, support_programs, request);
  const ml_predictions = buildLocalMlPredictions(request, markets, regulations, recalls, patents, support_programs, score_breakdown);
  const topMarket = markets[0]?.name || request.target_market;
  const topReg = regulations[0]?.title || "대상국 규제 확인 필요";
  const topRecall = recalls[0]?.hazard || "유사 리콜 위험 낮음";
  const topSupport = support_programs[0]?.name || "지원사업 확인 필요";
  const topPatent = patents[0]?.korean_keyword || "선행기술";
  const ai_summary = `${request.region} 소재 ${request.industry} 기업의 '${request.product_name}'은 ${topMarket} 진출 기준 수출 준비도 ${readiness_score}/100으로 산정되었습니다. 우선순위는 ${topReg} 검토, 리콜 위험(${topRecall})에 대한 시험 전 점검, ${topPatent} 관련 FTO 키워드 검토, 그리고 ${topSupport} 신청입니다. 30일 내 제품자료·시험견적·지원사업 신청을 끝내고, 60일 내 인증 실행과 바이어 요구사항 검증, 90일 내 파일럿 수출 패키지 완성을 권장합니다.`;
  return {
    request,
    readiness_score,
    readiness_level: readinessLevel(readiness_score),
    score_breakdown,
    markets,
    regulations,
    recalls: recalls.map((item) => {
      const { risk_score, keywords, category, ...rest } = item;
      return rest;
    }),
    patents: patents.map((item) => {
      const { risk_score, similarity, ...rest } = item;
      return rest;
    }),
    support_programs,
    roadmap,
    ml_predictions,
    ai_summary,
    guardrails: [
      "본 결과는 공공데이터와 데모 규칙 기반의 1차 진단이며, 최종 인증 가능 여부는 시험기관·관세사·변리사 확인이 필요합니다.",
      "HS Code와 제품 사양이 바뀌면 대상 규제와 지원사업 우선순위가 달라질 수 있습니다.",
      "실서비스에서는 데이터 호출 시점, 출처 URL, 문서 버전, 불확실성 점수를 함께 표시해야 합니다.",
    ],
    raw_signals: { agent_plan: ["market", "regulation", "recall", "patent", "support", "roadmap", "report"], target_region: target.region, region_profile: regionProfile },
  };
}

function collectForm() {
  const features = $$("fieldset input[type='checkbox']:checked")
    .map((input) => input.value)
    .filter((value) => value && !["on"].includes(value));
  return {
    company_name: $("#companyName").value.trim(),
    region: $("#region").value,
    industry: $("#industry").value,
    product_name: $("#productName").value.trim(),
    hs_code: $("#hsCode").value.trim(),
    target_market: $("#targetMarket").value,
    features,
    annual_export_usd: 0,
    certifications: $("#hasTestReport").checked ? ["KC", "UN38.3"] : [],
    has_bom: $("#hasBom").checked,
    has_test_report: $("#hasTestReport").checked,
    has_label_draft: $("#hasLabelDraft").checked,
    notes: null,
  };
}

async function runAnalysis(event) {
  if (event) event.preventDefault();
  const request = collectForm();
  const useBackend = $("#useBackend").checked;
  setBusy(true);
  try {
    if (useBackend) {
      const response = await fetch(`${API_BASE}/analyze`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(request),
      });
      if (!response.ok) throw new Error(`API ${response.status}`);
      lastResult = await response.json();
      showToast("FastAPI AI Agent 분석 완료");
    } else {
      await wait(240);
      lastResult = analyzeLocal(request);
      showToast("로컬 JS AI Agent 분석 완료");
    }
  } catch (error) {
    console.warn(error);
    lastResult = analyzeLocal(request);
    $("#useBackend").checked = false;
    updateBackendState();
    showToast("백엔드 연결 실패 · 로컬 분석으로 전환");
  } finally {
    render(lastResult);
    setBusy(false);
  }
}

function wait(ms) { return new Promise((resolve) => setTimeout(resolve, ms)); }

function setBusy(isBusy) {
  const button = $("button[type='submit']");
  button.disabled = isBusy;
  button.textContent = isBusy ? "분석 중..." : "AI Agent 분석 실행";
}

function updateBackendState() {
  const checked = $("#useBackend").checked;
  $("#backendState").textContent = checked ? "ON · http://127.0.0.1:8000" : "OFF · 로컬 JS 분석";
}

function riskBadge(level) {
  const lower = String(level || "LOW").toLowerCase();
  if (lower === "high" || lower === "critical") return "red";
  if (lower === "medium") return "yellow";
  return "green";
}

function priorityKo(priority) {
  return { critical: "긴급", high: "높음", medium: "보통", low: "낮음" }[priority] || priority;
}

function render(result) {
  if (!result) return;
  renderHero(result);
  renderInsightCards(result);
  renderMarkets(result.markets || []);
  renderScoreBreakdown(result.score_breakdown || {});
  renderRoadmap(result.roadmap || []);
  renderLists(result);
  renderDataSources(result);
  renderAdvancedScreens(result);
  renderReportHighlights(result);
  $("#aiSummary").textContent = result.ai_summary;
  $("#guardrails").innerHTML = (result.guardrails || []).map((text) => `<div class="guardrail">${escapeHtml(text)}</div>`).join("");
}

function renderHero(result) {
  $("#readinessScore").textContent = result.readiness_score;
  $("#scoreRing").style.setProperty("--score", result.readiness_score);
  const markets = [...new Set((result.markets || []).map((m) => m.region))].slice(0, 3).join(" · ");
  $("#passportMarkets").textContent = markets || "EU · US · ASEAN";
  $("#passportState").textContent = `Up to date · ${(result.regulations || []).length} rules matched`;
  $("#programCount").textContent = String((result.support_programs || []).length);
}

function renderInsightCards(result) {
  const request = result.request || {};
  const topReg = (result.regulations || [])[0];
  const topRecall = (result.recalls || [])[0];
  const topPatent = (result.patents || [])[0];
  const cards = [
    { label: "HS CODE", value: request.hs_code || "-", sub: `${request.product_name || "제품"} · ${request.target_market || "대상국"}` },
    { label: "PRODUCT", value: featureLabel(request.features || []), sub: "Export Focus Category" },
    { label: "CERTIFICATION", value: topReg ? topReg.title : "인증 후보 없음", sub: topReg ? topReg.matched_reason : "추가 데이터 확인 필요", badge: topReg ? priorityKo(topReg.priority) : "확인", badgeClass: topReg ? riskBadge(topReg.priority) : "yellow" },
    { label: "RECALL RISK", value: topRecall ? topRecall.risk_level : "LOW", sub: topRecall ? topRecall.hazard : "유사 리콜 없음", badge: topRecall ? topRecall.risk_level : "LOW", badgeClass: topRecall ? riskBadge(topRecall.risk_level) : "green" },
    { label: "FTO", value: topPatent ? topPatent.risk_level : "LOW", sub: topPatent ? `${topPatent.korean_keyword} · ${topPatent.ipc.join(", ")}` : "특허 혼잡도 낮음", badge: topPatent ? topPatent.risk_level : "LOW", badgeClass: topPatent ? riskBadge(topPatent.risk_level) : "green" },
  ];
  $("#insightCards").innerHTML = cards.map((card) => `
    <article class="insight-card">
      <div class="label">${escapeHtml(card.label)}</div>
      <div class="value">${escapeHtml(card.value)} ${card.badge ? `<span class="badge ${card.badgeClass}">${escapeHtml(card.badge)}</span>` : ""}</div>
      <p class="sub">${escapeHtml(card.sub)}</p>
    </article>`).join("");
}

function featureLabel(features) {
  const labels = { battery: "Battery Product", electronics: "Electronics", industrial: "Industrial", digital: "Digital Product", iot: "IoT Device", consumer: "Consumer Goods", carbon_material: "Carbon Material" };
  return (features || []).map((f) => labels[f] || f).slice(0, 2).join(" / ") || "Product";
}

function renderMarkets(markets) {
  $("#marketCards").innerHTML = markets.slice(0, 3).map((market) => `
    <article class="market-card">
      <h3>${escapeHtml(market.name)} <span class="badge ${market.opportunity_score >= 80 ? "green" : "yellow"}">${escapeHtml(market.opportunity_score)}</span></h3>
      <dl>
        <dt>Import Growth</dt><dd>+${escapeHtml(market.import_growth_yoy)}%</dd>
        <dt>Tariff(MFN)</dt><dd>${escapeHtml(market.tariff_mfn)}%</dd>
        <dt>Main Port</dt><dd>${escapeHtml(market.main_port)}</dd>
        <dt>Air / Sea</dt><dd>${escapeHtml(market.logistics_days_air)}d / ${escapeHtml(market.logistics_days_sea)}d</dd>
      </dl>
      <p class="sub" style="margin:12px 0 0;color:#9fbad1;font-size:12px;line-height:1.45">${escapeHtml(market.reason)}</p>
    </article>`).join("");
}

function renderScoreBreakdown(breakdown) {
  $("#scoreBreakdown").innerHTML = Object.entries(breakdown).map(([label, value]) => `
    <div class="score-row">
      <span>${escapeHtml(label)}</span>
      <div class="track"><div class="bar" style="--value:${Number(value)}%"></div></div>
      <b>${Number(value)}</b>
    </div>`).join("");
}

function renderRoadmap(roadmap) {
  $("#roadmap").innerHTML = roadmap.map((item) => `
    <article class="roadmap-card">
      <h3>${escapeHtml(item.day_bucket)}</h3>
      <h4>${escapeHtml(item.title)}</h4>
      <ul>${(item.tasks || []).map((task) => `<li>${escapeHtml(task)}</li>`).join("")}</ul>
      <div class="output">산출물 · ${escapeHtml(item.output)}<br>담당 · ${escapeHtml(item.owner)}</div>
    </article>`).join("");
}

function renderLists(result) {
  $("#regulationList").innerHTML = (result.regulations || []).slice(0, 5).map((item) => `
    <article class="stack-item">
      <h3>${escapeHtml(item.title)} <span class="badge ${riskBadge(item.priority)}">${escapeHtml(priorityKo(item.priority))}</span></h3>
      <p>${escapeHtml(item.scope)} · ${escapeHtml(item.matched_reason || "조건 일치")}</p>
      <p><b>실행:</b> ${escapeHtml(item.action)}</p>
      <ul>${(item.typical_evidence || []).slice(0, 4).map((x) => `<li>${escapeHtml(x)}</li>`).join("")}</ul>
    </article>`).join("");

  $("#recallList").innerHTML = (result.recalls || []).map((item) => `
    <article class="stack-item">
      <h3>${escapeHtml(item.product)} <span class="badge ${riskBadge(item.risk_level)}">${escapeHtml(item.risk_level)}</span></h3>
      <p>${escapeHtml(item.hazard)} · 유사도 ${Math.round((item.similarity || 0) * 100)}%</p>
      <ul>${(item.recommended_tests || []).slice(0, 4).map((x) => `<li>시험: ${escapeHtml(x)}</li>`).join("")}</ul>
    </article>`).join("");

  const patentHtml = (result.patents || []).slice(0, 2).map((item) => `
    <article class="stack-item">
      <h3>${escapeHtml(item.korean_keyword)} <span class="badge ${riskBadge(item.risk_level)}">${escapeHtml(item.risk_level)}</span></h3>
      <p>IPC ${escapeHtml((item.ipc || []).join(", "))} · 출원밀집도 ${escapeHtml(String(item.filings))}</p>
      <ul>${(item.avoidance_hints || []).slice(0, 3).map((x) => `<li>${escapeHtml(x)}</li>`).join("")}</ul>
    </article>`).join("");
  const supportHtml = (result.support_programs || []).slice(0, 3).map((item) => `
    <article class="stack-item">
      <h3>${escapeHtml(item.name)} <span class="badge green">${escapeHtml(item.fit_score)}</span></h3>
      <p>${escapeHtml(item.benefit)}</p>
      <p>${escapeHtml(item.priority_rule)}</p>
    </article>`).join("");
  $("#patentSupportList").innerHTML = patentHtml + supportHtml;
}

function renderDataSources(result) {
  const raw = result.raw_signals || {};
  const sources = raw.public_data_sources || PUBLIC_DATA_SOURCE_FALLBACK;
  $("#dataSourceMap").innerHTML = sources.map((source) => `
    <article class="source-card">
      <h3>${escapeHtml(source.name)}</h3>
      <p>${escapeHtml(source.use_in_product || "")}</p>
      <div class="source-meta">
        <span>${escapeHtml((source.screen_ids || []).join(" · "))}</span>
        <span>${escapeHtml(source.target_table || source.connector)}</span>
        <span>${escapeHtml(source.update_cycle || "on-demand")}</span>
      </div>
    </article>`).join("");
  const statusRows = sources.slice(0, 5).map((source) => `
    <article class="stack-item">
      <h3>${escapeHtml(source.connector)} <span class="badge yellow">seed/live</span></h3>
      <p><b>${escapeHtml(source.provider)}</b><br><span class="breakable">${escapeHtml(source.official_url || "")}</span></p>
      <p>API 키가 있으면 백엔드 파이프라인에서 live 수집을 시도하고, 없으면 동일 스키마의 seed 데이터로 정규화합니다.</p>
    </article>`).join("");
  $("#pipelineStatus").innerHTML = statusRows;
}

function renderAdvancedScreens(result) {
  renderConnectorRail(result);
  renderMlPredictions(result.ml_predictions || []);
  renderRegulationMatrix(result.regulations || []);
  renderRecallViz(result.recalls || []);
  renderFtoSupportViz(result.patents || [], result.support_programs || []);
  renderExecutionBoard(result.roadmap || [], result.ml_predictions || []);
}

function renderConnectorRail(result) {
  const sources = (result.raw_signals || {}).public_data_sources || PUBLIC_DATA_SOURCE_FALLBACK;
  $("#connectorRail").innerHTML = sources.map((source, index) => `
    <article class="connector-card">
      <div class="connector-index">${String(index + 1).padStart(2, "0")}</div>
      <div>
        <h3>${escapeHtml(source.name)}</h3>
        <p>${escapeHtml(source.target_table || source.connector)} · ${escapeHtml((source.screen_ids || []).join(" / "))}</p>
      </div>
      <span class="status-dot"></span>
    </article>`).join("");
}

function renderMlPredictions(predictions) {
  $("#mlPredictionGrid").innerHTML = predictions.map((item) => {
    const percent = item.unit === "%" ? Number(item.value) : clamp(Number(item.value) / 140 * 100);
    return `
      <article class="ml-card">
        <div class="ml-card-head">
          <span>${escapeHtml(item.model_type)}</span>
          <b>${Math.round(Number(item.confidence || 0) * 100)}%</b>
        </div>
        <h3>${escapeHtml(item.target)}</h3>
        <div class="ml-value">${escapeHtml(item.value)}<small>${escapeHtml(item.unit)}</small></div>
        <div class="ml-track"><span style="--value:${percent}%"></span></div>
        <p>${escapeHtml(item.interpretation)}</p>
      </article>`;
  }).join("");

  const combined = new Map();
  predictions.forEach((prediction) => {
    (prediction.feature_importance || []).forEach((item) => {
      combined.set(item.feature, (combined.get(item.feature) || 0) + Number(item.importance || 0));
    });
  });
  const rows = [...combined.entries()].sort((a, b) => b[1] - a[1]).slice(0, 7);
  $("#importanceChart").innerHTML = `
    <h3>통합 Feature Importance</h3>
    ${rows.map(([name, value]) => `
      <div class="importance-row">
        <span>${escapeHtml(name)}</span>
        <div><b style="--value:${Math.min(100, value * 100)}%"></b></div>
        <em>${Math.round(value * 100)}</em>
      </div>`).join("")}`;
}

function renderRegulationMatrix(regulations) {
  $("#regulationMatrix").innerHTML = regulations.slice(0, 8).map((item) => {
    const score = item.priority === "critical" ? 96 : item.priority === "high" ? 82 : item.priority === "medium" ? 58 : 34;
    return `
      <article class="matrix-cell ${riskBadge(item.priority)}">
        <span>${escapeHtml(priorityKo(item.priority))}</span>
        <h3>${escapeHtml(item.title)}</h3>
        <div class="matrix-score">${score}</div>
        <p>${escapeHtml(item.matched_reason || item.scope)}</p>
      </article>`;
  }).join("");
}

function renderRecallViz(recalls) {
  $("#recallRiskViz").innerHTML = recalls.map((item) => {
    const risk = item.risk_level === "HIGH" ? 88 : item.risk_level === "MEDIUM" ? 56 : 24;
    return `
      <article class="risk-row">
        <div>
          <h3>${escapeHtml(item.product)}</h3>
          <p>${escapeHtml(item.hazard)}</p>
        </div>
        <div class="risk-meter"><span style="--value:${risk}%"></span></div>
        <b>${escapeHtml(item.risk_level)}</b>
      </article>`;
  }).join("");
}

function renderFtoSupportViz(patents, programs) {
  const patentBlocks = patents.slice(0, 3).map((item) => `
    <article>
      <span>FTO</span>
      <h3>${escapeHtml(item.korean_keyword)}</h3>
      <div class="bubble" style="--size:${Math.round(Number(item.risk || 0.45) * 112)}px">${escapeHtml(item.risk_level)}</div>
    </article>`).join("");
  const supportBlocks = programs.slice(0, 3).map((item) => `
    <article>
      <span>SUPPORT</span>
      <h3>${escapeHtml(item.name)}</h3>
      <div class="support-score">${escapeHtml(item.fit_score)}</div>
      <p>${escapeHtml(item.roadmap_stage)}</p>
    </article>`).join("");
  $("#ftoSupportViz").innerHTML = patentBlocks + supportBlocks;
}

function renderExecutionBoard(roadmap, predictions) {
  const lead = predictions.find((item) => item.target.includes("리드타임"));
  $("#executionBoard").innerHTML = roadmap.map((item, index) => `
    <article class="execution-column">
      <span>${escapeHtml(item.day_bucket)}</span>
      <h3>${escapeHtml(item.title)}</h3>
      ${(item.tasks || []).slice(0, 4).map((task) => `<p>${escapeHtml(task)}</p>`).join("")}
      <b>${index === 1 && lead ? `예상 리드타임 ${escapeHtml(lead.value)}${escapeHtml(lead.unit)}` : escapeHtml(item.output)}</b>
    </article>`).join("");
}

function renderReportHighlights(result) {
  const container = $("#reportHighlights");
  if (!container) return;
  const topReg = (result.regulations || [])[0];
  const lead = (result.ml_predictions || []).find((item) => item.target.includes("리드타임"));
  const success = (result.ml_predictions || []).find((item) => item.target.includes("실행 가능성"));
  const support = (result.support_programs || [])[0];
  const firstTask = (result.roadmap || [])[0]?.tasks?.[0];
  const cards = [
    {
      label: "다음 우선순위",
      value: topReg ? topReg.title : "규제 후보 확인",
      sub: topReg ? topReg.action : "제품 정보 입력 후 우선 인증을 확정합니다.",
    },
    {
      label: "예상 리드타임",
      value: lead ? `${lead.value}${lead.unit}` : "확인 필요",
      sub: lead ? lead.interpretation : "시험기관 상담 후 일정이 확정됩니다.",
    },
    {
      label: "실행 가능성",
      value: success ? `${success.value}${success.unit}` : `${result.readiness_score}/100`,
      sub: "30일 자료 정리, 60일 인증 실행, 90일 파일럿 수출 기준입니다.",
    },
    {
      label: "추천 지원",
      value: support ? support.name : "지원사업 확인",
      sub: firstTask || "인증·시험 비용 지원사업을 먼저 확인합니다.",
    },
  ];
  container.innerHTML = cards.map((card) => `
    <article>
      <span>${escapeHtml(card.label)}</span>
      <strong>${escapeHtml(card.value)}</strong>
      <p>${escapeHtml(card.sub)}</p>
    </article>`).join("");
}

async function collectPublicData() {
  const useBackend = $("#useBackend").checked;
  if (!useBackend) {
    showToast("FastAPI 백엔드 사용을 켜면 파이프라인 파일을 생성합니다.");
    renderDataSources(lastResult || analyzeLocal(collectForm()));
    return;
  }
  try {
    const response = await fetch(`${API_BASE}/public-data/collect`, { method: "POST" });
    if (!response.ok) throw new Error(`API ${response.status}`);
    const payload = await response.json();
    $("#pipelineStatus").innerHTML = (payload.api_call_log || []).map((row) => `
      <article class="stack-item">
        <h3>${escapeHtml(row.connector_name)} <span class="badge ${row.status === "success" ? "green" : "yellow"}">${escapeHtml(row.status)}</span></h3>
        <p>mode ${escapeHtml(row.mode)} · records ${escapeHtml(row.record_count)}</p>
        <p class="breakable">${escapeHtml(row.error_code || payload.output_path || "")}</p>
      </article>`).join("");
    showToast("공공데이터 파이프라인 정규화 파일 생성 완료");
  } catch (error) {
    console.warn(error);
    showToast("백엔드 연결 실패 · 화면 출처 맵만 표시");
  }
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function showToast(message) {
  let toast = $(".toast");
  if (!toast) {
    toast = document.createElement("div");
    toast.className = "toast";
    document.body.appendChild(toast);
  }
  toast.textContent = message;
  toast.classList.add("show");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove("show"), 2200);
}

function fillSample() {
  $("#companyName").value = "경남 배터리테크";
  $("#region").value = "경남";
  $("#industry").value = "전기전자";
  $("#productName").value = "리튬이온 배터리팩";
  $("#hsCode").value = "850760";
  $("#targetMarket").value = "Germany / EU";
  $$("fieldset input[type='checkbox']").forEach((input) => {
    input.checked = ["battery", "electronics", "industrial"].includes(input.value);
  });
  $("#hasBom").checked = true;
  $("#hasTestReport").checked = true;
  $("#hasLabelDraft").checked = true;
  runAnalysis();
}

function downloadJson() {
  if (!lastResult) runAnalysis();
  const blob = new Blob([JSON.stringify(lastResult, null, 2)], { type: "application/json;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `grow-gate-local-ex-report-${Date.now()}.json`;
  a.click();
  URL.revokeObjectURL(url);
}

function printReport() {
  if (!lastResult) return;
  const report = buildReportHtml(lastResult);
  const popup = window.open("", "_blank", "width=980,height=900");
  if (!popup) {
    showToast("팝업 차단을 해제하면 보고서 인쇄가 가능합니다.");
    return;
  }
  popup.opener = null;
  popup.document.write(report);
  popup.document.close();
  popup.focus();
  setTimeout(() => popup.print(), 250);
}

function buildReportHtml(result) {
  const request = result.request;
  return `<!doctype html><html lang="ko"><head><meta charset="utf-8"><title>GROW-GATE Local EX Report</title>
    <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; img-src data:; base-uri 'none'; form-action 'none'">
    <style>
      body{font-family:system-ui,-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;margin:36px;color:#111827;line-height:1.65}
      h1{font-size:30px;margin-bottom:0} h2{margin-top:28px;border-bottom:2px solid #e5e7eb;padding-bottom:6px}
      .meta{display:grid;grid-template-columns:repeat(2,1fr);gap:8px 18px;background:#f8fafc;border:1px solid #e5e7eb;border-radius:14px;padding:16px;margin:20px 0}
      .score{font-size:42px;font-weight:900;color:#0f766e} .card{border:1px solid #e5e7eb;border-radius:12px;padding:14px;margin:10px 0} ul{margin-top:6px}
      .badge{display:inline-block;border-radius:999px;padding:2px 8px;background:#ecfeff;color:#0369a1;font-size:12px;font-weight:800}
    </style></head><body>
    <h1>GROW-GATE Local EX 수출 준비도 리포트</h1>
    <p>Regulation-to-Market Twin for Regional SMEs</p>
    <div class="meta">
      <div><b>기업</b><br>${escapeHtml(request.company_name)}</div>
      <div><b>지역/업종</b><br>${escapeHtml(request.region)} / ${escapeHtml(request.industry)}</div>
      <div><b>제품/HS Code</b><br>${escapeHtml(request.product_name)} / ${escapeHtml(request.hs_code)}</div>
      <div><b>대상 시장</b><br>${escapeHtml(request.target_market)}</div>
    </div>
    <div class="score">${result.readiness_score}/100 <span class="badge">${result.readiness_level}</span></div>
    <h2>AI 요약</h2><p>${escapeHtml(result.ai_summary)}</p>
    <h2>ML 예측·추정</h2>${(result.ml_predictions || []).map((m) => `<div class="card"><b>${escapeHtml(m.target)}</b> <span class="badge">${escapeHtml(m.model_type)}</span><br><span style="font-size:26px;font-weight:900;color:#0f766e">${escapeHtml(m.value)}${escapeHtml(m.unit)}</span><br><small>${escapeHtml(m.interpretation)}</small></div>`).join('')}
    <h2>규제·인증 우선순위</h2>${(result.regulations || []).slice(0,5).map((r) => `<div class="card"><b>${escapeHtml(r.title)}</b> <span class="badge">${escapeHtml(r.priority)}</span><br>${escapeHtml(r.action)}<br><small>${escapeHtml((r.typical_evidence || []).join(', '))}</small></div>`).join('')}
    <h2>30·60·90일 실행 로드맵</h2>${(result.roadmap || []).map((r) => `<div class="card"><b>${escapeHtml(r.day_bucket)} · ${escapeHtml(r.title)}</b><ul>${(r.tasks || []).map((t) => `<li>${escapeHtml(t)}</li>`).join('')}</ul><small>산출물: ${escapeHtml(r.output)} / 담당: ${escapeHtml(r.owner)}</small></div>`).join('')}
    <h2>유의사항</h2><ul>${(result.guardrails || []).map((g) => `<li>${escapeHtml(g)}</li>`).join('')}</ul>
    </body></html>`;
}

function setupWorkspaceNavigation() {
  const links = $$(".workspace-sidebar nav a[href^='#']");
  if (!links.length) return;
  const linkById = new Map();
  links.forEach((link) => {
    const id = link.getAttribute("href").slice(1);
    linkById.set(id, link);
    link.addEventListener("click", () => {
      links.forEach((item) => item.classList.remove("active"));
      link.classList.add("active");
    });
  });
  const sections = [...linkById.keys()]
    .map((id) => document.getElementById(id))
    .filter(Boolean);
  if (!sections.length || !("IntersectionObserver" in window)) {
    links[0].classList.add("active");
    return;
  }
  const observer = new IntersectionObserver((entries) => {
    const visible = entries
      .filter((entry) => entry.isIntersecting)
      .sort((a, b) => b.intersectionRatio - a.intersectionRatio)[0];
    if (!visible) return;
    links.forEach((item) => item.classList.remove("active"));
    linkById.get(visible.target.id)?.classList.add("active");
  }, { rootMargin: "-18% 0px -68% 0px", threshold: [0.08, 0.18, 0.32] });
  sections.forEach((section) => observer.observe(section));
  links[0].classList.add("active");
}

function init() {
  $("#analysisForm").addEventListener("submit", runAnalysis);
  $("#fillSample").addEventListener("click", fillSample);
  $("#useBackend").addEventListener("change", updateBackendState);
  $("#downloadJson").addEventListener("click", downloadJson);
  $("#printReport").addEventListener("click", printReport);
  $("#collectPublicData").addEventListener("click", collectPublicData);
  setupWorkspaceNavigation();
  updateBackendState();
  runAnalysis();
}

document.addEventListener("DOMContentLoaded", init);
