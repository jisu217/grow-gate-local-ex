const WEIGHTS = {
  profile: 20,
  description: 10,
  trustGrade: 15,
  certification: 20,
  fto: 20,
  buyerRequirement: 15,
}

function hasText(value) {
  return String(value ?? '').trim().length > 0
}

function hasCount(product, key) {
  const value = Number(product?.[key])
  return Number.isFinite(value) && value > 0
}

function hasList(product, ...keys) {
  return keys.some((key) => Array.isArray(product?.[key]) && product[key].length > 0)
}

function hasExplicitRiskBasis(product) {
  return ['ftoCount', 'buyerRequirementCount'].some((key) => Number.isFinite(Number(product?.[key]))) ||
    ['fto', 'buyerRequirements'].some((key) => Array.isArray(product?.[key]))
}

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

function hasCertificationBasis(product) {
  const status = String(product?.knowledgeStatus ?? '').toLowerCase()
  return hasCount(product, 'certificationCount') ||
    hasList(product, 'certifications', 'certs') ||
    hasCount(product, 'expiringCertCount') ||
    ['valid', 'expiring', 'expiring_soon', 'expired'].includes(status)
}

export function calculateKnowledgeRegistrationRate(product = {}) {
  let score = 0

  if (
    hasText(product.name ?? product.productName) &&
    hasText(product.hsCode ?? product.hs) &&
    hasText(product.targetCountry ?? product.country)
  ) {
    score += WEIGHTS.profile
  }

  if (hasText(product.description)) score += WEIGHTS.description
  if (hasText(product.trustGrade ?? product.grade)) score += WEIGHTS.trustGrade
  if (hasCertificationBasis(product)) score += WEIGHTS.certification

  if (hasExplicitRiskBasis(product)) {
    if (hasCount(product, 'ftoCount') || hasList(product, 'fto')) score += WEIGHTS.fto
    if (hasCount(product, 'buyerRequirementCount') || hasList(product, 'buyerRequirements')) {
      score += WEIGHTS.buyerRequirement
    }
  } else if (Number.isFinite(Number(product.missingFieldCount))) {
    const completedRiskItems = clamp(2 - Number(product.missingFieldCount), 0, 2)
    score += Math.round(((WEIGHTS.fto + WEIGHTS.buyerRequirement) * completedRiskItems) / 2)
  }

  return clamp(score, 0, 100)
}

export function describeKnowledgeRegistration(product = {}) {
  const rate = Number.isFinite(Number(product.registrationRate))
    ? Number(product.registrationRate)
    : calculateKnowledgeRegistrationRate(product)
  const missingCount = Number(product.missingFieldCount ?? product.missingFields?.length ?? 0)
  const suffix = rate >= 100
    ? '필수 항목 입력 완료'
    : missingCount > 0
      ? `보완 ${missingCount}건`
      : '추가 근거 필요'

  return `데이터 기준 ${rate}% · ${suffix}`
}
