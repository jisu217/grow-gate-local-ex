export function collectFailureReasons(runs = [], records = [], proofRun = null) {
  const reasons = new Set()
  runs.forEach((run) => {
    if (run.errorSummary) reasons.add(run.errorSummary)
  })
  records.forEach((record) => {
    if (record.errorMessage) reasons.add(`${record.provider}: ${record.errorMessage}`)
    if (record.sourceType === 'cache' && !record.fallbackUsed) return
    if (record.operationEndpoint === false) reasons.add(`${record.provider}: operation endpoint 아님`)
    if (record.apiKeyConfigured === false) reasons.add(`${record.provider}: API key 없음`)
  })
  proofRun?.providers?.forEach((provider) => {
    if (provider.fallbackReason && provider.sourceType === 'fallback') {
      reasons.add(`${provider.providerCode}: ${provider.fallbackReason}`)
    }
  })
  return Array.from(reasons)
}

export function sortProofProviders(providers = []) {
  return [...providers].sort((a, b) => proofProviderRank(a) - proofProviderRank(b)
    || String(a?.providerCode || '').localeCompare(String(b?.providerCode || '')))
}

function proofProviderRank(provider) {
  if (provider?.providerCode === 'SAFETY_RECALL') return 0
  if (provider?.sourceType === 'live') return 1
  if (provider?.sourceType === 'cache' && Number(provider?.recordCount || 0) > 0) return 2
  if (provider?.sourceType === 'cache') return 3
  if (provider?.sourceType === 'empty') return 4
  if (provider?.sourceType === 'fallback') return 5
  return 6
}

export function providerDisplayName(providerCode) {
  if (providerCode === 'SAFETY_RECALL') return '산업통상부_국가기술표준원_제품안전_국내리콜정보'
  return providerCode || '-'
}

export function sourceBadgeLabel(provider) {
  const source = provider?.sourceType || '-'
  if (source === 'cache' && provider?.operationEndpoint && provider?.apiKeyConfigured) return 'cache · API 수집 캐시'
  if (source === 'cache' && provider?.providerCode === 'SAFETY_RECALL') return 'cache · CSV cache'
  if (source === 'live') return 'live · API 직접 수집'
  if (source === 'fallback') return 'fallback · 확인 필요'
  return source
}

export function apiProofLabel(provider) {
  if (provider?.providerCode === 'SAFETY_RECALL' && provider?.sourceType === 'cache') {
    const apiDerived = provider.operationEndpoint && provider.apiKeyConfigured
    return {
      primary: apiDerived ? 'endpoint yes' : 'CSV cache',
      secondary: apiDerived ? 'key set' : 'API key optional for refresh',
    }
  }
  return {
    primary: `endpoint ${provider?.operationEndpoint ? 'yes' : 'no'}`,
    secondary: `key ${provider?.apiKeyConfigured ? 'set' : 'missing'}`,
  }
}

export function providerReasonLabel(provider) {
  const reason = provider?.fallbackReason || ''
  if (provider?.providerCode === 'SAFETY_RECALL' && provider?.sourceType === 'cache') {
    if (provider.operationEndpoint && provider.apiKeyConfigured) return 'API-derived cache 사용 가능'
    if (reason.includes('CSV cache available')) return 'CSV cache 사용 가능 · fallback 아님'
    return 'CSV cache 사용 가능 여부 확인됨'
  }
  return reason || '-'
}
