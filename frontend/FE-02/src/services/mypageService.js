// 마이페이지 서비스 (백엔드 연동 시 이 함수 내부만 fetch로 교체)
import { MYPAGE_COMPANY, MYPAGE_MEMBERS, MYPAGE_PRODUCTS } from '../mock/mypageData.js'

// 회사 정보 + 팀원 — 페이지 첫 진입 시 1회 호출
export async function getMypageOverview() {
  return { company: MYPAGE_COMPANY, members: MYPAGE_MEMBERS }
}

// 제품 목록 (제품별 진단/지식/리포트 요약 포함)
export async function getMypageProducts() {
  return [...MYPAGE_PRODUCTS]
}

// 신규 제품 등록 — POST /api/products 대응
// mock 단계에선 공유 배열(MYPAGE_PRODUCTS)에 직접 추가해 마이페이지·새 진단 화면이 같은 목록을 공유
export async function createMypageProduct({ name, hs, country, description = '' }) {
  const created = {
    id: `prod_${Date.now()}`,
    name, hs, country, description,
    score: null, chips: [],
    todo: [{ task: '제품 지식 입력하기', due: '30' }],
    kn: 0, miss: '미입력 (정보를 채워주세요)',
    reports: [],
  }
  MYPAGE_PRODUCTS.push(created)
  return created
}

// 제품 정보 수정 — PATCH /api/products/{id} 대응
export async function updateMypageProduct(id, { name, hs, country }) {
  return { id, name, hs, country }
}

// AI 진단 다시 실행 — POST /api/diagnosis 대응 (비동기 처리 가정)
export async function rerunDiagnosis(productId) {
  // 실제로는 POST 후 GET /api/diagnosis/:id 폴링으로 결과 수신
  return new Promise((resolve) => {
    setTimeout(() => resolve({ productId, status: 'done' }), 1800)
  })
}
