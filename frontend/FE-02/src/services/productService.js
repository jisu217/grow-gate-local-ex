// 제품 지식 서비스
import { apiFetch } from './apiClient.js'
import { PRODUCTS, PRODUCT_DETAILS } from '../mock/productData.js'

// 백엔드 knowledgeStatus(snake_case) → FE 표시용 key 변환
const STATUS_MAP = {
  valid: 'valid',
  expiring_soon: 'expiring',
  expired: 'expired',
  needs_review: 'needs-check',
  incomplete: 'missing',
}

// 제품 목록 항목 매핑
function mapProductSummary(p) {
  return {
    id: p.productId,
    name: p.name,
    category: p.category,
    hsCode: p.hsCode,
    targetCountry: p.targetCountry,
    description: p.description,
    grade: p.trustGrade,
    expiringCerts: p.expiringCertCount,
    updatedAt: p.updatedAt,
    knowledgeStatus: STATUS_MAP[p.knowledgeStatus] ?? p.knowledgeStatus,
    sourceBasis: undefined,
    // 목록 응답은 누락 필드명이 아닌 건수만 제공 → 건수만큼 채운 배열로 .length 사용처 호환
    missingFields: Array(p.missingFieldCount ?? 0).fill(null),
  }
}

// 전체 제품 목록 조회
export async function getProducts() {
  try {
    const res = await apiFetch('/products')
    return res.products.map(mapProductSummary)
  } catch (err) {
    console.warn('[productService] getProducts API 실패, mock으로 대체합니다.', err)
    return PRODUCTS
  }
}

// 제품 상세 매핑
// 백엔드 상세 응답에 없는 knowledgeStatus·sourceType(인증서 단위)·변경 이력은
// 각각 undefined/[] 처리. 변경 이력은 별도 /audit-logs 엔드포인트 연동 필요(미연동).
function mapProductDetail(p) {
  return {
    id: p.productId,
    name: p.name,
    category: p.category,
    hsCode: p.hsCode,
    targetCountry: p.targetCountry,
    description: p.description,
    grade: p.knowledge?.trustGrade,
    knowledgeStatus: undefined,
    sourceBasis: p.knowledge?.sourceType,
    missingFields: [],
    updatedAt: p.updatedAt,
    certs: (p.certifications ?? []).map((c) => ({
      name: c.name,
      expiryDate: c.expiresAt,
      renewalCycle: c.renewalCycle,
      status: c.daysUntilExpiry <= 30 ? '임박' : '정상',
      sourceType: undefined,
    })),
    fto: (p.fto ?? []).map((f) => ({
      target: f.targetCountry,
      status: f.riskLevel,
      note: f.summary,
    })),
    buyerRequirements: (p.buyerRequirements ?? []).map((b) => b.requirements),
    auditLog: [],
  }
}

// 특정 제품 지식 상세 조회
export async function getProductKnowledge(productId) {
  try {
    const detail = await apiFetch(`/products/${productId}/knowledge`)
    return mapProductDetail(detail)
  } catch (err) {
    console.warn('[productService] getProductKnowledge API 실패, mock으로 대체합니다.', err)
    const product = PRODUCT_DETAILS[productId]
    if (!product) throw new Error(`제품을 찾을 수 없습니다. (id: ${productId})`)
    return product
  }
}

// 제품 지식 저장 (쓰기 작업 — 조회와 달리 실패 시 mock 대체 없이 에러 그대로 전파)
// 사유: mock 성공으로 둔갑 시 사용자는 저장 완료로 착각하나 실제로는 입력 유실 위험
export async function saveProductKnowledge(productId, payload) {
  const body = {
    name: payload.productName ?? payload.name,
    category: payload.category,
    hsCode: payload.hsCode,
    targetCountry: payload.targetCountry,
    description: payload.description,
    knowledge: payload.knowledge,
    reason: payload.reason,
  }
  const res = await apiFetch(`/products/${productId}/knowledge`, {
    method: 'PUT',
    body: JSON.stringify(body),
  })
  return { ...PRODUCT_DETAILS[productId], ...payload, id: res.productId, updatedAt: res.updatedAt }
}

// 신규 제품 생성 (쓰기 작업 — saveProductKnowledge와 동일하게 실패 시 에러 그대로 전파)
export async function createProduct(payload) {
  const body = {
    name: payload.productName ?? payload.name,
    category: payload.category,
    hsCode: payload.hsCode,
    targetCountry: payload.targetCountry,
    description: payload.description,
  }
  const res = await apiFetch('/products', { method: 'POST', body: JSON.stringify(body) })
  return { ...payload, id: res.productId, createdAt: res.createdAt }
}
