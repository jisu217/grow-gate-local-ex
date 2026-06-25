// 마이페이지 서비스 — 로그인 사용자 기준 DB API 연동
import { apiFetch } from './apiClient.js'

const ROLE_LABELS = {
  owner: '소유자',
  admin: '관리자',
  editor: '편집',
  viewer: '조회',
}

function normalizeRole(role) {
  return String(role ?? 'owner').toLowerCase()
}

function toMypageMember(member) {
  const role = normalizeRole(member.role)
  return {
    name: member.name,
    initial: member.name?.slice(0, 1) ?? 'U',
    role,
    roleLabel: ROLE_LABELS[role] ?? role,
  }
}

function toMypageProduct(product) {
  const missingCount = product.missingFieldCount ?? 0
  const expiringCount = product.expiringCertCount ?? 0
  const chips = []

  if (product.knowledgeStatus === 'valid') chips.push('제품 지식 확인 완료')
  if (expiringCount > 0) chips.push(`만료 임박 ${expiringCount}건`)
  if (missingCount > 0) chips.push(`누락 정보 ${missingCount}건`)

  return {
    id: product.productId,
    runId: null,
    name: product.name,
    hs: product.hsCode ?? '',
    country: product.targetCountry ?? '',
    category: product.category ?? '',
    description: product.description ?? '',
    score: null,
    chips,
    todo: missingCount > 0
      ? [{ task: '제품 지식 보완하기', due: '30' }]
      : [{ task: 'AI 진단 실행하기', due: '30' }],
    kn: product.knowledgeStatus === 'valid' ? 100 : missingCount > 0 ? 60 : 30,
    miss: missingCount > 0 ? `누락 정보 ${missingCount}건` : '기본 정보 저장됨',
    reports: [],
    updatedAt: product.updatedAt,
  }
}

// 회사 정보 + 팀원 — 현재 로그인 사용자 기준
export async function getMypageOverview() {
  const user = await apiFetch('/users/me')
  let company = {
    name: user.companyName ?? '개인 계정',
    industry: '',
    manager: user.name,
    email: user.email,
  }
  let members = [toMypageMember(user)]

  if (user.userType === 'company') {
    const [companyInfo, memberInfo] = await Promise.all([
      apiFetch('/companies/me'),
      apiFetch('/companies/me/members'),
    ])

    company = {
      name: companyInfo.name,
      industry: companyInfo.industry ?? '',
      manager: user.name,
      email: user.email,
      businessNumber: companyInfo.businessNumber,
    }
    members = (memberInfo.members ?? []).map(toMypageMember)
  }

  return { company, members }
}

// 제품 목록 — 백엔드가 인증 사용자 기준으로 개인/회사 범위를 분리해서 반환
export async function getMypageProducts() {
  const res = await apiFetch('/products')
  return (res.products ?? []).map(toMypageProduct)
}

// 신규 제품 등록 — POST /api/products
export async function createMypageProduct({ name, hs, country, description = '', category = '' }) {
  const created = await apiFetch('/products', {
    method: 'POST',
    body: JSON.stringify({
      name,
      category,
      hsCode: hs,
      targetCountry: country,
      description,
    }),
  })

  return toMypageProduct({
    productId: created.productId,
    name: created.name,
    category,
    hsCode: hs,
    targetCountry: country,
    description,
    knowledgeStatus: 'incomplete',
    missingFieldCount: 2,
    expiringCertCount: 0,
    updatedAt: created.createdAt,
  })
}

// 제품 기본 정보 수정 — PUT /api/products/{id}/knowledge
export async function updateMypageProduct(id, { name, hs, country, description, category }) {
  await apiFetch(`/products/${id}/knowledge`, {
    method: 'PUT',
    body: JSON.stringify({
      name,
      category,
      hsCode: hs,
      targetCountry: country,
      description,
      reason: '마이페이지 제품 기본 정보 수정',
    }),
  })
  return { id, name, hs, country, description, category }
}

// AI 진단 다시 실행 — 실제 진단 생성 API로 연결
export async function rerunDiagnosis(productId, targetCountry) {
  return apiFetch('/diagnosis', {
    method: 'POST',
    body: JSON.stringify({ productId, targetCountry }),
  })
}
