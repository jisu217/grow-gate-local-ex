import assert from 'node:assert/strict'
import {
  apiProofLabel,
  collectFailureReasons,
  providerDisplayName,
  providerReasonLabel,
  sourceBadgeLabel,
} from './publicDataProofPresenter.js'

const csvCacheSafetyRecall = {
  providerCode: 'SAFETY_RECALL',
  sourceType: 'cache',
  sourceQualityStatus: 'CACHE_VERIFIED',
  operationEndpoint: false,
  apiKeyConfigured: false,
  fallbackReason: 'API_KEY_NOT_CONFIGURED; CSV cache available; cacheLineage=file-snapshot',
}

assert.equal(
  providerDisplayName('SAFETY_RECALL'),
  '산업통상부_국가기술표준원_제품안전_국내리콜정보',
)
assert.equal(sourceBadgeLabel(csvCacheSafetyRecall), 'cache · CSV cache')
assert.equal(providerReasonLabel(csvCacheSafetyRecall), 'CSV cache 사용 가능 · fallback 아님')
assert.deepEqual(apiProofLabel(csvCacheSafetyRecall), {
  primary: 'CSV cache',
  secondary: 'API key optional for refresh',
})

const reasons = collectFailureReasons(
  [],
  [{
    provider: 'SAFETY_RECALL',
    sourceType: 'cache',
    fallbackUsed: false,
    operationEndpoint: false,
    apiKeyConfigured: false,
  }],
  { providers: [csvCacheSafetyRecall] },
)

assert.deepEqual(reasons, ['SAFETY_RECALL: CSV cache 사용 가능 · fallback 아님'])

assert.equal(
  sourceBadgeLabel({
    providerCode: 'SAFETY_RECALL',
    sourceType: 'cache',
    operationEndpoint: true,
    apiKeyConfigured: true,
  }),
  'cache · API 수집 캐시',
)

console.log('public data proof presenter checks passed')
