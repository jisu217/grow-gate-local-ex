// 진단 결과 목업 데이터
export const CURRENT_SESSION = {
  companyName: '경남 배터리테크',
  productName: '리튬이온 배터리팩',
  targetMarket: 'DE/EU',

  // 제품 지식 연결
  productId:            'prod-001',
  companyId:            'comp-001',
  knowledgeSnapshotId:  'snap-prod-001-20250528',  // 진단 시점 제품 지식 버전
  trustGrade:           'high',
  missingFields:        ['MSDS 최신본', 'CE DoC 초안'],
  evidenceSources:      ['관세청', 'KOTRA', 'KTL', 'SafetyKorea', 'KIPRIS', '기업마당', 'EU RAPEX'],
  updatedAt:            '2025-05-28',

  // 진단 결과 요약
  diagnosisScore: {
    overall: 83,
    market: 91,
    regulation: 75,
    recall: 88,
    fto: 75,
    support: 79,
  },
  riskItems: [
    { level: 'high',   label: '과열·열폭주 리콜 유사도 0.86' },
    { level: 'high',   label: 'BMS 특허 침해 위험 (삼성SDI)' },
    { level: 'medium', label: '낙하·충격 단락 유사도 0.72' },
    { level: 'medium', label: 'CE/EMC 인증 대응 필요' },
  ],
  actionRoadmap: { d30: 4, d60: 4, d90: 4 },
}

// KPI 카드
export const KPI_DATA = [
  {
    step: '01',
    eyebrow: 'Public Data',
    value: '7',
    unit: ' APIs',
    valueFontSize: '40px',
    sub: '연동 준비 완료',

    sources: ['관세청', 'KOTRA', 'KTL', 'SafetyKorea', 'KIPRIS', '기업마당', 'EU'],
  },
  {
    step: '02',
    eyebrow: 'Readiness Score',

    score: 83,
    sub: '제품 단위 수출 준비도',

    donutOffset: 16.5,
  },
  {
    step: '03',
    eyebrow: 'Regulation Passport',
    value: '6',
    unit: ' Rules',
    valueFontSize: '40px',
    sub: '적용 규제 자동 판정',

    pills: [
      { type: 'req', label: 'CE/EMC' },
      { type: 'req', label: '배터리여권' },
      { type: 'warn', label: 'GPSR' },
      { type: 'cond', label: 'CRA' },
    ],
  },
  {
    step: '04',
    eyebrow: 'Action Roadmap',
    value: '30·60·90',
    unit: ' Days',
    valueFontSize: '30px',
    sub: '우선 액션 12건 · 문서 초안 3건',

    roadSegments: [
      { days: '30', label: '기초' },
      { days: '60', label: '시험' },
      { days: '90', label: '실행' },
    ],
  },
]

// S00 개요
export const S00_OVERVIEW = [
  { title: '수출 준비도', value: '83', unit: '/100', desc: '시장성·규제·문서 종합' },
  { title: '적용 규제',   value: '6',  unit: '건',   desc: 'CE/EMC·배터리여권 등'  },
  { title: '지원사업',    value: '5',  unit: '건',   desc: '최고 적합도 92%'        },
  { title: '유사 리콜',   value: '3',  unit: '건',   desc: '최대 유사도 0.86'       },
]

// S01 입력
export const S01_INPUT = [
  { key: '기업명',     val: '경남 배터리테크' },
  { key: '지역 / 입주', val: '경남 / 테크노파크' },
  { key: '업종',       val: '전기전자 · 배터리' },
  { key: '제품명',     val: '리튬이온 배터리팩' },
  { key: 'HS Code',    val: '850760' },
  { key: '대상국',     val: '독일 / EU' },
]

// S02 커넥터
export const S02_CONNECTORS = [
  { no: '01', nm: '관세청_수출입무역통계 GW',             meta: 'trade_signal · S02 / S04 / S09' },
  { no: '02', nm: '대한무역투자진흥공사_글로벌 공급망 인사이트', meta: 'country_profile · S02 / S04 / S08 / S09' },
  { no: '03', nm: '국가기술표준원_제품 안전인증 및 리콜 정보',  meta: 'recall_case · S02 / S05 / S06 / S08 / S09' },
  { no: '04', nm: '특허청 KIPRIS Plus Open API',          meta: 'patent_signal · S02 / S07 / S08 / S09' },
  { no: '05', nm: '수출바우처·해외규격인증·지역TP 지원사업 공고', meta: 'support_program · S02 / S07 / S08 / S09' },
]

// S03 준비도 점수 fallback
export const S03_PREDICTIONS = [
  { head: 'Rule', acc: 'trace', title: '90일 파일럿 수출 준비도', value: '95.3', unit: '점',    bar: 95, desc: '시장성·규제압력·문서 준비도를 종합해 실행 준비도를 추정합니다.' },
  { head: 'Rule',  acc: 'trace', title: '인증·시험 리드타임 추정',       value: '49',   unit: 'days', bar: 55, desc: '긴급 규제 수·배터리 여부·문서 준비도를 기준으로 시험기관 상담부터 보완 완료까지 기간을 추정합니다.' },
  { head: 'Rule', acc: 'trace', title: '지원사업 신청 우선순위',       value: '84.8', unit: '점',    bar: 85, desc: '인증 갭·지역 제조기업 태그·지원사업 매칭 점수로 신청 우선순위를 정렬합니다.' },
  { head: 'Rule',  acc: 'trace', title: '시험·보완 예비비율 추정',       value: '13.6', unit: '%',    bar: 30, desc: '리콜·FTO·규제 위험을 반영해 시험 실패·라벨 보완·회피설계 상담에 필요한 예비비율을 추정합니다.' },
]
export const S03_FEATURES = [
  { label: '시장성 점수', value: 96 },
  { label: '규제 압력',   value: 88 },
  { label: '문서 준비도', value: 72 },
  { label: '지원사업 적합도', value: 60 },
  { label: '리콜 위험도', value: 48 },
]

// S04 시장
export const S04_MARKETS = [
  { flag: '🇩🇪 Germany / EU',  score: 87, grade: 'h', gradeLabel: 'HIGH',   marketSize: '€8.2B',  growth: '+18.4%' },
  { flag: '🇺🇸 United States', score: 71, grade: 'm', gradeLabel: 'MEDIUM', marketSize: '$12.4B', growth: '+11.2%' },
  { flag: '🇻🇳 Vietnam',       score: 78, grade: 'h', gradeLabel: 'HIGH',   marketSize: '$850M',  growth: '+32.6%' },
]

// S05 규제
export const S05_REGULATIONS = [
  { nm: 'UN38.3 운송시험',    mkt: '배터리 항공·해상 필수',     market: ['req','전체'], priority: ['req','긴급'],  status: ['req','누락위험'], duration: '4주',  cost: '~300만원' },
  { nm: 'CE / EMC 적합성',    mkt: '전기전자 EU 필수',          market: ['req','EU'],   priority: ['req','필수'],  status: ['warn','대응필요'], duration: '8주',  cost: '~800만원' },
  { nm: 'EU 배터리 여권',     mkt: 'Battery Regulation 2024',  market: ['req','EU'],   priority: ['req','필수'],  status: ['warn','준비중'],  duration: '12주', cost: '~500만원' },
  { nm: 'GPSR',               mkt: '일반제품안전규정 2024',     market: ['warn','EU'],  priority: ['warn','주의'], status: ['warn','검토중'],  duration: '6주',  cost: '~200만원' },
  { nm: 'CRA',                mkt: '사이버복원력법 2026년',     market: ['cond','EU'],  priority: ['cond','조건부'], status: ['cond','2026 적용'], duration: '—', cost: '—' },
  { nm: 'RoHS',               mkt: '유해물질 제한',             market: ['ok','EU'],    priority: ['req','필수'],  status: ['ok','준수 완료'], duration: '—',    cost: '완료' },
]

// S06 리콜
export const S06_RECALLS = [
  { riskClass: 'h', badge: '고위험', title: '과열 / 열폭주',   similarity: '0.86', checks: ['BMS 온도 임계값 재검토', 'UN38.3 열 충격 시험 선행', 'FMEA 열 이벤트 시나리오 추가'] },
  { riskClass: 'm', badge: '중위험', title: '낙하·충격 내부 단락', similarity: '0.72', checks: ['IEC 62133 낙하 시험 조기 실시', '셀 배치 구조 설계 검토', '체크리스트 담당자 배정 필요'] },
  { riskClass: 'm', badge: '중위험', title: '충전회로 오작동',  similarity: '0.68', checks: ['충전 IC 공급사 이력 확인', '과충전 보호 회로 이중화', '필드 데이터 6개월 추적'] },
]

// S07 특허·지원
export const S07_PATENTS = [
  { riskClass: 'h', badge: '고위험', name: '배터리 관리 시스템 (BMS)',   sub: '삼성SDI · KR10-2019-0xxxxxx · 청구항 12개' },
  { riskClass: 'm', badge: '중위험', name: '배터리팩 구조 및 방열 설계', sub: 'LG에너지솔루션 · KR10-2020-0xxxxxx · 청구항 8개' },
  { riskClass: 'l', badge: '저위험', name: '원통형 셀 배열 방식',        sub: 'Panasonic · EP3xxxxxxx · 청구항 5개' },
]
export const S07_SUPPORTS = [
  { name: '해외규격인증획득지원', org: 'KOTRA · 최대 5,000만원', dday: 'D-14', isGreen: false },
  { name: '수출바우처',           org: 'KOTRA · 최대 3,000만원', dday: 'D-30', isGreen: false },
  { name: '으뜸기업 수출지원',   org: '경남도 · 최대 1,000만원', dday: '상시', isGreen: true  },
  { name: '배터리팩 안전인증비 지원', org: '산업부 · 최대 2,000만원', dday: 'D-60', isGreen: false },
]

// S08 실행 보드
export const S08_EXEC = [
  {
    day: '30', label: '기초 정리',
    tasks: [
      { urgent: true,  tag: '인증 · 긴급', title: 'UN38.3 시험 KTL 의뢰',    who: '품질팀 · D-0 착수' },
      { urgent: true,  tag: '인증 · 긴급', title: 'MSDS 최신본 업데이트',    who: '품질팀 · D-3 완료' },
      { urgent: false, tag: '문서',         title: 'CE 기술문서(DoC) 초안 작성', who: '인증담당 · D-14 완료' },
      { urgent: false, tag: '지원',         title: '해외규격인증 지원신청 제출', who: '관리팀 · D-14 마감' },
    ],
  },
  {
    day: '60', label: '샘플·문서',
    tasks: [
      { urgent: false, tag: '인증', title: 'IEC 62133 시험 완료',      who: '품질팀 · D-45 완료 목표' },
      { urgent: false, tag: '규제', title: 'EU 배터리 여권 초안 작성', who: '인증담당 · D-50 완료' },
      { urgent: false, tag: '규제', title: 'GPSR 적합성 선언서 검토',  who: '법무/인증 · D-55 완료' },
      { urgent: false, tag: '영업', title: '독일 바이어 샘플 준비',    who: '해외영업팀 · D-60 발송' },
    ],
  },
  {
    day: '90', label: '직접 수출',
    tasks: [
      { urgent: false, tag: '인증', title: 'CE/EMC 시험성적서 취득',   who: '품질팀 · D-75 완료' },
      { urgent: false, tag: '영업', title: '독일 바이어 계약 검토',    who: '해외영업팀 · D-80' },
      { urgent: false, tag: '물류', title: 'UN38.3 포장·라벨 확정',   who: '물류팀 · D-85 완료' },
      { urgent: false, tag: '수출', title: '첫 수출 선적 진행',        who: '물류·영업 · D-90' },
    ],
  },
]

// S09 리포트 점수
export const S09_SCORE_BARS = [
  { name: '규제·인증 준비도', value: 78 },
  { name: '시장 접근성',      value: 86 },
  { name: '기술 경쟁력',      value: 88 },
  { name: '특허 안전성',      value: 72 },
  { name: '리콜 위험도 (역)', value: 68, amber: true },
  { name: '지원사업 활용도',  value: 65 },
]

// S10 파이프라인
export const S10_PIPES = [
  { source: '관세청 무역통계',    count: '1,240', label: '수집 레코드', sourceType: 'live' },
  { source: 'KOTRA 해외시장',     count: '84',    label: '리포트 건',   sourceType: 'live' },
  { source: 'KTL Certinfo',       count: '312',   label: '인증 항목',   sourceType: 'live' },
  { source: 'SafetyKorea',        count: '56',    label: '리콜 사례',   sourceType: 'live' },
  { source: 'KIPRIS 특허',        count: '2,810', label: '검색 결과',   sourceType: 'live' },
  { source: '기업마당 지원사업',  count: '18',    label: '매칭 사업',   sourceType: 'live' },
  { source: 'EU RAPEX',           count: '430',   label: 'EU 리콜 경보', sourceType: 'live' },
]
