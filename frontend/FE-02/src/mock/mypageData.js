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
export const MYPAGE_PRODUCTS = [
  {
    id: 'prod_001', runId: 'diag-001', name: '리튬배터리 팩', country: '독일', hs: '850760', score: 83,
    description: '전동공구, 산업용 장비, 이동형 전원장치 등에 사용되는 충전식 리튬이온 전원 모듈',
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
  },
  {
    id: 'prod_002', runId: 'diag-002', name: 'BMS 모듈', country: '미국', hs: '853710', score: 67,
    description: '배터리 상태 감시 및 보호회로 제어를 위한 전자 제어 모듈',
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
  },
  {
    id: 'prod_003', runId: 'diag-003', name: '휴대용 충전기', country: 'ASEAN', hs: '850440', score: 45,
    description: 'USB-C 기반 소형 전원공급장치 및 휴대용 충전 어댑터',
    chips: ['규제 위험 5건', '인증 필요 4건', 'FTO 위험 높음'],
    todo: [
      { task: '제품 지식 추가 입력', due: '30' },
      { task: '인도네시아 SNI 확인', due: '60' },
      { task: '특허 회피 설계 검토', due: '90' },
    ],
    kn: 31, miss: '미입력 11항목',
    reports: [],
  },
]
