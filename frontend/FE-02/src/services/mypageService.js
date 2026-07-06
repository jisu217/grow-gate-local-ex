// 마이페이지 서비스 — 로그인 사용자 기준 DB API 연동
import { apiFetch } from './apiClient.js'
import { getDiagnosisList, getDiagnosisReport } from './diagnosisService.js'
import {
  calculateKnowledgeRegistrationRate,
  describeKnowledgeRegistration,
} from '../utils/knowledgeRegistration.js'

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
  const registrationRate = calculateKnowledgeRegistrationRate(product)
  const registrationLabel = describeKnowledgeRegistration({ ...product, registrationRate })
  const chips = []

  if (registrationRate >= 100) chips.push('제품 정보 확인 완료')
  if (expiringCount > 0) chips.push(`만료 임박 ${expiringCount}건`)
  if (missingCount > 0) chips.push(`누락 정보 ${missingCount}건`)

  const viewProduct = {
    id: product.productId,
    runId: product.runId ?? null,
    diagnosisStatus: product.diagnosisStatus ?? null,
    name: product.name,
    hs: product.hsCode ?? '',
    country: product.targetCountry ?? '',
    category: product.category ?? '',
    description: product.description ?? '',
    buyerName: product.buyerName ?? '',
    buyerCountry: product.buyerCountry ?? '',
    buyerRequest: product.buyerRequest ?? '',
    buyerRequirementCount: product.buyerRequirementCount ?? 0,
    score: product.score ?? null,
    chips,
    kn: registrationRate,
    registrationLabel,
    miss: registrationLabel.replace(/^데이터 기준 \d+% · /, ''),
    reports: product.reports ?? [],
    updatedAt: product.updatedAt,
  }
  return {
    ...viewProduct,
    todo: buildProductTodo(viewProduct),
  }
}

export function buildReportArchive(diagnoses = [], productId) {
  return diagnoses
    .filter((diagnosis) => diagnosis.productId === productId)
    .filter((diagnosis) => String(diagnosis.status ?? '').toLowerCase() === 'done')
    .map((diagnosis) => ({
      diagnosisId: diagnosis.id,
      file: `grow-gate-diagnosis-${diagnosis.id}.md`,
      date: formatReportDate(diagnosis.requestedAt),
      format: 'Markdown',
      score: diagnosis.diagnosisScore ?? null,
      targetMarket: diagnosis.targetMarket ?? '',
      downloadLabel: 'Markdown 다운로드',
    }))
}

export function buildRerunDiagnosisPath(product) {
  if (!product?.id) return '/diagnosis/new'
  const params = new URLSearchParams({
    productId: product.id,
    mode: 'rerun',
  })
  if (product.next) params.set('next', product.next)
  return `/diagnosis/new?${params.toString()}`
}

export function buildKnowledgeEditPath(product) {
  if (!product?.id) return '/mypage'
  const params = new URLSearchParams()
  if (product.miss) params.set('missingFields', product.miss)
  const query = params.toString()
  return `/mypage/knowledge/${encodeURIComponent(product.id)}/edit${query ? `?${query}` : ''}`
}

export function buildActionBoardPath(product) {
  if (product?.runId) return `/diagnosis/${encodeURIComponent(product.runId)}#action-board`
  return buildRerunDiagnosisPath({ ...product, next: 'action-board' })
}

export function buildProductTodo(product = {}) {
  const isRunning = product.diagnosisStatus === 'running' || product.diagnosisStatus === 'pending'
  if (isRunning) {
    return [{
      task: '진단 완료 상태 확인하기',
      due: '30',
      owner: '수출 담당자',
      output: '완료된 Gate 결과',
      path: product.runId ? `/diagnosis/${encodeURIComponent(product.runId)}` : '/diagnosis',
      actionLabel: '진단 상태 보기',
    }]
  }
  if (Number(product.registrationRate ?? product.kn ?? 0) < 100) {
    return [{
      task: '제품 정보 보완하기',
      due: '30',
      owner: '제품 담당자',
      output: '보완된 제품 지식',
      path: buildKnowledgeEditPath(product),
      actionLabel: '보완 화면으로 이동',
    }]
  }
  return [{
    task: 'Gate 판단 실행하기',
    due: '30',
    owner: '수출 담당자',
    output: '새 Gate 판단',
    path: buildRerunDiagnosisPath(product),
    actionLabel: 'Gate 판단 시작',
  }]
}

export function buildRerunDiagnosisPayload(product) {
  if (!product?.id) return null
  return {
    productId: product.id,
    targetCountry: product.buyerCountry || product.country,
    inputData: {
      productName: product.name,
      hsCode: product.hs,
      description: product.description ?? '',
      region: '',
      industry: product.category ?? '',
      buyerRequest: product.buyerRequest ?? '',
      buyerDeadline: null,
      expectedShipmentDate: null,
      requestedCertifications: [],
      currentDocuments: [],
      exportExperience: '',
      budgetLimitKrw: null,
    },
  }
}

function formatReportDate(value) {
  if (!value) return ''
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return String(value).slice(0, 10)
  return parsed.toISOString().slice(0, 10)
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
  const diagnosisIndex = await getDiagnosisIndexByProductId()
  return (res.products ?? []).map((product) => {
    const index = diagnosisIndex.get(product.productId) ?? { latest: null, reports: [] }
    const latest = index.latest
    return toMypageProduct({
      ...product,
      runId: latest?.id,
      diagnosisStatus: latest?.status,
      score: latest?.diagnosisScore,
      reports: index.reports,
    })
  })
}

async function getDiagnosisIndexByProductId() {
  try {
    const diagnoses = await getDiagnosisList()
    const byProductId = new Map()
    diagnoses.forEach((diagnosis) => {
      if (!diagnosis.productId) return
      const current = byProductId.get(diagnosis.productId) ?? { latest: null, diagnoses: [] }
      if (!current.latest) current.latest = diagnosis
      current.diagnoses.push(diagnosis)
      byProductId.set(diagnosis.productId, current)
    })
    byProductId.forEach((value, productId) => {
      value.reports = buildReportArchive(value.diagnoses, productId)
    })
    return byProductId
  } catch (err) {
    console.warn('[mypageService] 최신 진단 목록을 불러오지 못해 제품 기본 정보만 표시합니다.', err)
    return new Map()
  }
}

export async function getMypageReport(diagnosisId) {
  return getDiagnosisReport(diagnosisId)
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
    buyerName: '',
    buyerCountry: '',
    buyerRequest: '',
    knowledgeStatus: 'incomplete',
    certificationCount: 0,
    ftoCount: 0,
    buyerRequirementCount: 0,
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

// 제품 삭제 — DELETE /api/products/{id} (백엔드 소프트 삭제: 감사 로그·진단 이력은 보존)
export async function deleteMypageProduct(id) {
  await apiFetch(`/products/${id}`, { method: 'DELETE' })
  return { id }
}
