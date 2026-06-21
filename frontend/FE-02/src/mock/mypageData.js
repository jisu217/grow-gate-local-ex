// 마이페이지 목업 데이터 (백엔드 연동 전 임시)

// 회사 정보 카드용 — GET /api/companies/me + GET /api/users/me 대응
export const MYPAGE_COMPANY = {
  name: '경남 배터리테크',
  industry: '전기전자 · 배터리',
  manager: '홍길동',
  email: 'demo@example.com',
}

// 팀원 권한 카드용 — GET /api/companies/me/members 대응 (※백엔드 신규 필요)
export const MYPAGE_MEMBERS = [
  { name: '홍길동', initial: '홍', role: 'admin',  roleLabel: '관리자' },
  { name: '김담당', initial: '김', role: 'editor', roleLabel: '편집' },
  { name: '이수출', initial: '이', role: 'editor', roleLabel: '편집' },
]

// 제품 단위 데이터 — 제품 선택 시 바뀌는 카드들의 원본
// 실제로는 GET /api/products + GET /api/diagnosis/:id + GET /api/reports 조합으로 채워짐
// 진단 영역별 점수의 근거가 되는 공공데이터 출처
export const BREAKDOWN_SOURCES = {
  '시장성':     '관세청 수출입무역통계',
  '규제·인증':  'KTL Certinfo · EU 규정',
  '리콜·품질':  'SafetyKorea 리콜정보',
  '특허·FTO':   'KIPRIS 특허정보',
  '지원사업':   '기업마당 지원사업',
  '문서완성도': '등록 문서 대조',
}

export const MYPAGE_PRODUCTS = [
  {
    id: 'prod_001', runId: 'diag-001', name: '리튬배터리 팩', country: '독일', hs: '850760', score: 83,
    chips: ['규제 위험 2건', '인증 필요 3건', 'FTO 위험 낮음'],
    todo: [
      { task: 'CE 인증 견적 요청', due: '30' },
      { task: '독일 RoHS 자료 검토', due: '30' },
      { task: 'KOTRA 상담 예약', due: '60' },
    ],
    kn: 72, miss: '미입력 4항목',
    reports: [
      { file: '진단_리튬배터리_0619.pdf', date: '06.19' },
      { file: '진단_리튬배터리_0612.pdf', date: '06.12' },
    ],
    // 점수 근거 — 영역별 점수 × 비중(weight)의 가중합이 종합 점수가 됨
    detail: {
      grade: 'B+',
      summary: '시장성은 충분하지만, EU 배터리 인증·문서가 아직 부족해 수출 실행 전 보완이 필요합니다.',
      breakdown: [
        { key: '시장성',     score: 91, weight: 30 },
        { key: '규제·인증',  score: 75, weight: 15 },
        { key: '리콜·품질',  score: 88, weight: 30 },
        { key: '특허·FTO',   score: 75, weight: 5 },
        { key: '지원사업',   score: 79, weight: 5 },
        { key: '문서완성도', score: 68, weight: 15 },
      ],
      missingDocs: ['시험성적서', '라벨 초안', '적합성 선언서', '영문 매뉴얼', '배터리 구성표'],
    },
  },
  {
    id: 'prod_002', runId: 'diag-002', name: 'BMS 모듈', country: '미국', hs: '853710', score: 67,
    chips: ['규제 위험 4건', '인증 필요 2건', 'FTO 위험 보통'],
    todo: [
      { task: 'UL 인증 요건 확인', due: '30' },
      { task: 'FCC 적합성 검토', due: '60' },
      { task: '미국 변호사 지정', due: '90' },
    ],
    kn: 54, miss: '미입력 7항목',
    reports: [
      { file: '진단_BMS_0617.pdf', date: '06.17' },
    ],
    detail: {
      grade: 'C+',
      summary: '미국 시장성은 보통이며, UL·FCC 인증과 제품 문서 보완이 시급합니다.',
      breakdown: [
        { key: '시장성',     score: 80, weight: 30 },
        { key: '규제·인증',  score: 60, weight: 15 },
        { key: '리콜·품질',  score: 70, weight: 30 },
        { key: '특허·FTO',   score: 55, weight: 5 },
        { key: '지원사업',   score: 65, weight: 5 },
        { key: '문서완성도', score: 50, weight: 15 },
      ],
      missingDocs: ['UL 인증 자료', 'FCC 적합성 자료', '시험성적서', '회로 설계 문서'],
    },
  },
  {
    id: 'prod_003', runId: 'diag-003', name: '휴대용 충전기', country: 'ASEAN', hs: '850440', score: 45,
    chips: ['규제 위험 5건', '인증 필요 4건', 'FTO 위험 높음'],
    todo: [
      { task: '제품 지식 추가 입력', due: '30' },
      { task: '인도네시아 SNI 확인', due: '60' },
      { task: '특허 회피 설계 검토', due: '90' },
    ],
    kn: 31, miss: '미입력 11항목',
    reports: [],
    detail: {
      grade: 'D',
      summary: '제품 지식이 많이 비어 있어 진단 신뢰도가 낮습니다. 기본 정보부터 보완이 필요합니다.',
      breakdown: [
        { key: '시장성',     score: 60, weight: 30 },
        { key: '규제·인증',  score: 35, weight: 15 },
        { key: '리콜·품질',  score: 50, weight: 30 },
        { key: '특허·FTO',   score: 30, weight: 5 },
        { key: '지원사업',   score: 40, weight: 5 },
        { key: '문서완성도', score: 25, weight: 15 },
      ],
      missingDocs: ['제품 사양서', 'SNI 인증 자료', '시험성적서', '안전 자료', '라벨 초안'],
    },
  },
]
