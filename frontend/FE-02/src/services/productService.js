// 제품 지식 서비스
import { apiFetch } from './apiClient.js'
import { PRODUCTS, PRODUCT_DETAILS } from '../mock/productData.js'

const ENABLE_MOCK = import.meta.env.VITE_ENABLE_MOCK === 'true'

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
    if (!ENABLE_MOCK) throw err
    console.warn('[productService] getProducts API 실패, VITE_ENABLE_MOCK=true라 mock으로 대체합니다.', err)
    return PRODUCTS
  }
}

// 제품 상세 매핑
function mapProductDetail(p) {
  const missingFields = []
  if ((p.fto ?? []).length === 0) missingFields.push('fto')
  if ((p.buyerRequirements ?? []).length === 0) missingFields.push('buyerRequirements')
  const hasExpired = (p.certifications ?? []).some((c) => c.daysUntilExpiry < 0)
  const hasExpiring = (p.certifications ?? []).some((c) => c.daysUntilExpiry >= 0 && c.daysUntilExpiry <= 30)
  const knowledgeStatus = hasExpired
    ? 'expired'
    : hasExpiring
      ? 'expiring'
      : missingFields.length > 0
        ? 'needs-check'
        : p.knowledge?.trustGrade
          ? 'valid'
          : 'missing'

  return {
    id: p.productId,
    name: p.name,
    category: p.category,
    hsCode: p.hsCode,
    targetCountry: p.targetCountry,
    description: p.description,
    grade: p.knowledge?.trustGrade,
    knowledgeStatus,
    sourceBasis: p.knowledge?.sourceType,
    missingFields,
    updatedAt: p.updatedAt,
    certs: (p.certifications ?? []).map((c) => ({
      id: c.id,
      name: c.name,
      expiryDate: c.expiresAt,
      renewalCycle: c.renewalCycle,
      status: c.daysUntilExpiry < 0 ? '만료' : c.daysUntilExpiry <= 30 ? '임박' : '정상',
      sourceType: undefined,
    })),
    fto: (p.fto ?? []).map((f) => ({
      id: f.id,
      target: f.targetCountry,
      status: f.riskLevel,
      note: f.summary,
    })),
    buyerRequirements: (p.buyerRequirements ?? []).map((b) => ({
      id: b.id,
      buyerName: b.buyerName,
      country: b.country,
      requirements: b.requirements,
    })),
    auditLog: [],
  }
}

// 특정 제품 지식 상세 조회
export async function getProductKnowledge(productId) {
  try {
    const detail = await apiFetch(`/products/${productId}/knowledge`)
    return mapProductDetail(detail)
  } catch (err) {
    if (!ENABLE_MOCK) throw err
    console.warn('[productService] getProductKnowledge API 실패, VITE_ENABLE_MOCK=true라 mock으로 대체합니다.', err)
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

// 제품 삭제
export async function deleteProduct(productId) {
  return apiFetch(`/products/${productId}`, { method: 'DELETE' })
}

// 변경 이력(Audit Log) 조회 — audit:view 권한 없는 계정은 백엔드가 403을 반환하므로 빈 배열로 대체
export async function getProductAuditLogs(productId) {
  try {
    const res = await apiFetch(`/products/${productId}/audit-logs`)
    return (res.logs ?? []).map((log) => ({
      date: log.changedAt,
      by: log.changedBy,
      field: (log.changedFields ?? []).join(', '),
      oldValue: log.previousValue,
      newValue: log.newValue,
      reason: log.reason,
    }))
  } catch (err) {
    if (!ENABLE_MOCK) throw err
    console.warn('[productService] getProductAuditLogs API 실패, VITE_ENABLE_MOCK=true라 빈 배열로 대체합니다.', err)
    return []
  }
}

// 인증서 추가/수정/삭제 (쓰기 작업 — 실패 시 에러 그대로 전파)
export async function addCertification(productId, payload) {
  return apiFetch(`/products/${productId}/certifications`, { method: 'POST', body: JSON.stringify(payload) })
}

export async function updateCertification(productId, certificationId, payload) {
  return apiFetch(`/products/${productId}/certifications/${certificationId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deleteCertification(productId, certificationId) {
  return apiFetch(`/products/${productId}/certifications/${certificationId}`, { method: 'DELETE' })
}

// FTO 추가/수정/삭제 (쓰기 작업 — 실패 시 에러 그대로 전파)
export async function addFto(productId, payload) {
  return apiFetch(`/products/${productId}/fto`, { method: 'POST', body: JSON.stringify(payload) })
}

export async function updateFto(productId, ftoId, payload) {
  return apiFetch(`/products/${productId}/fto/${ftoId}`, { method: 'PUT', body: JSON.stringify(payload) })
}

export async function deleteFto(productId, ftoId) {
  return apiFetch(`/products/${productId}/fto/${ftoId}`, { method: 'DELETE' })
}

// 바이어 요구사항 추가/수정/삭제 (쓰기 작업 — 실패 시 에러 그대로 전파)
export async function addBuyerRequirement(productId, payload) {
  return apiFetch(`/products/${productId}/buyer-requirements`, { method: 'POST', body: JSON.stringify(payload) })
}

export async function updateBuyerRequirement(productId, buyerRequirementId, payload) {
  return apiFetch(`/products/${productId}/buyer-requirements/${buyerRequirementId}`, {
    method: 'PUT',
    body: JSON.stringify(payload),
  })
}

export async function deleteBuyerRequirement(productId, buyerRequirementId) {
  return apiFetch(`/products/${productId}/buyer-requirements/${buyerRequirementId}`, { method: 'DELETE' })
}

// 지식 출처 추가/삭제 (쓰기 작업 — 실패 시 에러 그대로 전파)
export async function addKnowledgeSource(productId, payload) {
  return apiFetch(`/products/${productId}/knowledge/sources`, { method: 'POST', body: JSON.stringify(payload) })
}

export async function deleteKnowledgeSource(productId, sourceId) {
  return apiFetch(`/products/${productId}/knowledge/sources/${sourceId}`, { method: 'DELETE' })
}
