import assert from 'node:assert/strict'
import {
  apiProofLabel,
  collectFailureReasons,
  diagnosisAvailabilityMeta,
  formatProviderChips,
  groupFailureReasons,
  providerAgencyName,
  providerDataName,
  providerDisplayName,
  providerReasonLabel,
  schemaValidationLabel,
  sourceBadgeLabel,
  sourceStatusMeta,
  sortProofProviders,
  summarizeRecentRunComparison,
} from './publicDataProofPresenter.js'

const apiKeyMissingCode = ['API', 'KEY', 'NOT', 'CONFIGURED'].join('_')

const csvCacheSafetyRecall = {
  providerCode: 'SAFETY_RECALL',
  sourceType: 'cache',
  sourceQualityStatus: 'CACHE_VERIFIED',
  operationEndpoint: false,
  apiKeyConfigured: false,
  fallbackReason: `${apiKeyMissingCode}; CSV cache available; cacheLineage=file-snapshot`,
}

assert.equal(
  providerDisplayName('SAFETY_RECALL'),
  '제품안전 국내 리콜 정보',
)
assert.equal(providerAgencyName('SAFETY_RECALL'), '한국제품안전관리원')
assert.equal(providerDataName('KOTRA_MARKET'), 'KOTRA 해외시장 정보')
assert.equal(sourceBadgeLabel(csvCacheSafetyRecall), '저장 데이터 사용')
assert.equal(providerReasonLabel(csvCacheSafetyRecall), 'CSV 저장본 사용 가능 · 예비 데이터 아님')
assert.deepEqual(apiProofLabel(csvCacheSafetyRecall), {
  primary: '저장 데이터 사용',
  secondary: 'API 키는 새로고침 때만 필요',
})
assert.deepEqual(sourceStatusMeta(csvCacheSafetyRecall), {
  label: '저장 데이터 사용',
  tone: 'cache',
  detail: '검증된 CSV 저장본을 사용합니다.',
})
assert.equal(schemaValidationLabel('PASS'), '형식 검증 통과')

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

assert.deepEqual(reasons, [])

const failureGroups = groupFailureReasons(
  [],
  [
    { provider: 'BIZINFO_SUPPORT', errorMessage: apiKeyMissingCode, sourceType: 'fallback' },
    { provider: 'KIPRIS_FTO', errorMessage: apiKeyMissingCode, sourceType: 'fallback' },
    { provider: 'KOTRA_MARKET', errorMessage: 'TIMEOUT', sourceType: 'fallback' },
  ],
  null,
)

const keyMissingGroup = failureGroups.find((group) => group.code === 'KEY_NOT_CONFIGURED')
const timeoutGroup = failureGroups.find((group) => group.code === 'TIMEOUT')
assert.equal(keyMissingGroup.label, 'API 키 설정 필요')
assert.equal(keyMissingGroup.count, 1)
assert.deepEqual(keyMissingGroup.providerNames, ['KIPRIS FTO 특허 정보'])
assert.equal(keyMissingGroup.impact, '낮음')
assert.equal(timeoutGroup.label, '응답 시간 초과')
assert.equal(timeoutGroup.providerNames[0], 'KOTRA 해외시장 정보')

assert.deepEqual(
  sortProofProviders([
    { providerCode: 'BIZINFO_SUPPORT', sourceType: 'empty' },
    { providerCode: 'KATS_CERTIFIED_PRODUCT_INFO', sourceType: 'cache', recordCount: 10 },
    csvCacheSafetyRecall,
  ]).map((provider) => provider.providerCode),
  ['SAFETY_RECALL', 'KATS_CERTIFIED_PRODUCT_INFO'],
)

assert.equal(
  sourceBadgeLabel({
    providerCode: 'SAFETY_RECALL',
    sourceType: 'cache',
    operationEndpoint: true,
    apiKeyConfigured: true,
  }),
  '저장 데이터 사용',
)

assert.deepEqual(
  formatProviderChips([
    'BIZINFO_SUPPORT',
    'KIPRIS_FTO',
    'KOTRA_MARKET',
    'SAFETY_RECALL',
    'KCS_TARIFF_LOGISTICS',
    'KOREA_LAW_STATUTES',
  ], 5).hiddenCount,
  0,
)

assert.equal(diagnosisAvailabilityMeta({ liveCount: 26, cacheCount: 13, fallbackCount: 0 }).label, '진단 반영 가능')
assert.equal(diagnosisAvailabilityMeta({ liveCount: 26, cacheCount: 13, fallbackCount: 5 }).label, '일부 확인 필요')
assert.equal(diagnosisAvailabilityMeta({ liveCount: 0, cacheCount: 0, fallbackCount: 5 }).label, '진단 반영 제한')

assert.deepEqual(
  summarizeRecentRunComparison([
    { liveCount: 26, cacheCount: 13, fallbackCount: 0 },
    { liveCount: 0, cacheCount: 0, fallbackCount: 5 },
  ]).map((card) => card.value),
  ['100%', '+26건', '5건 감소', '13건'],
)

console.log('public data proof presenter checks passed')
