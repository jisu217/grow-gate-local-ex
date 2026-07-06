import assert from 'node:assert/strict'
import {
  apiProofLabel,
  abbreviateTraceId,
  buildDiagnosisLinkSummary,
  buildPublicDataStatusSections,
  collectFailureReasons,
  diagnosisAvailabilityMeta,
  formatEvidenceImpact,
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
assert.equal(sourceBadgeLabel(csvCacheSafetyRecall), '최근 수집본 사용')
assert.equal(providerReasonLabel(csvCacheSafetyRecall), 'CSV 저장본 사용 가능 · 참고 데이터와 구분됨')
assert.deepEqual(apiProofLabel(csvCacheSafetyRecall), {
  primary: '최근 수집본 사용',
  secondary: '운영 키는 새로고침 때만 필요',
})
assert.deepEqual(sourceStatusMeta(csvCacheSafetyRecall), {
  label: '최근 수집본 사용',
  tone: 'cache',
  detail: '최근 수집된 공식 데이터를 기준으로 표시합니다.',
  code: 'CACHE_AVAILABLE',
})
assert.equal(schemaValidationLabel('PASS'), '형식 검증 통과')
assert.equal(schemaValidationLabel('SKIPPED'), '후속 검증 예정')

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
assert.equal(keyMissingGroup.label, '운영 키 등록 예정')
assert.equal(keyMissingGroup.count, 1)
assert.deepEqual(keyMissingGroup.providerNames, ['KIPRIS FTO 특허 정보'])
assert.equal(keyMissingGroup.impact, '낮음')
assert.equal(timeoutGroup.label, '수집 안정화 대상')
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
  '최근 수집본 사용',
)

const statusSections = buildPublicDataStatusSections([
  { providerCode: 'KOTRA_MARKET', sourceType: 'live', recordCount: 3 },
  { providerCode: 'KATS_CERTIFIED_PRODUCT_INFO', sourceType: 'cache', recordCount: 10 },
  { providerCode: 'KOTRA_OVERSEAS_EXHIBITIONS', sourceType: 'empty', recordCount: 0 },
  { providerCode: 'KOREA_LAW_STATUTES', sourceType: 'empty', recordCount: 0 },
  { providerCode: 'EXIM_EXCHANGE_RATE', sourceType: 'empty', apiKeyConfigured: false, recordCount: 0 },
  { providerCode: 'KCS_TRADE', sourceType: 'fallback', fallbackReason: 'TIMEOUT', recordCount: 0 },
])

assert.equal(statusSections.currentRows.length, 3)
assert.equal(statusSections.roadmapRows.length, 3)
assert.equal(statusSections.statusCounts.LIVE_CONNECTED, 1)
assert.equal(statusSections.statusCounts.CACHE_AVAILABLE, 1)
assert.equal(statusSections.statusCounts.NO_MATCH, 1)
assert.equal(statusSections.statusCounts.PLANNED_INTEGRATION, 1)
assert.equal(statusSections.statusCounts.OPERATING_KEY_REQUIRED, 1)
assert.equal(statusSections.statusCounts.STABILIZATION_REQUIRED, 1)

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
assert.equal(diagnosisAvailabilityMeta({ liveCount: 26, cacheCount: 13, fallbackCount: 5 }).label, '일부 후속 관리')
assert.equal(diagnosisAvailabilityMeta({ liveCount: 0, cacheCount: 0, fallbackCount: 5 }).label, '진단 반영 제한')

assert.deepEqual(formatEvidenceImpact(-1), { label: '확인 필요 -1', tone: 'negative' })
assert.deepEqual(formatEvidenceImpact(4), { label: '긍정 근거 +4', tone: 'positive' })
assert.deepEqual(formatEvidenceImpact(0), { label: '영향 없음', tone: 'neutral' })
assert.deepEqual(formatEvidenceImpact(null), { label: '검토 필요', tone: 'warning' })
assert.deepEqual(formatEvidenceImpact(undefined), { label: '검토 필요', tone: 'warning' })
assert.equal(abbreviateTraceId('57ddd3bd-c8d6-40fd-b25c-978b6f67c8f6'), '57ddd3bd...67c8f6')

const diagnosisLinks = buildDiagnosisLinkSummary([
  {
    diagnosisId: '57ddd3bd-c8d6-40fd-b25c-978b6f67c8f6',
    providerCode: 'KOTRA_MARKET',
    dimension: 'market',
    sourceType: 'live',
    publicDataDelta: 4,
    guardedDelta: 4,
    evidenceRecordIds: ['275ad569-f336-44dd-8d67-014547b03cc8'],
  },
  {
    diagnosisId: '57ddd3bd-c8d6-40fd-b25c-978b6f67c8f6',
    providerCode: 'CONSUMER_RECALL_CHEMICAL',
    dimension: 'recall',
    sourceType: 'empty',
    publicDataDelta: -2,
    guardedDelta: -1,
    evidenceRecordIds: ['6710fd98-9fd7-4ea7-af08-1388356bb38d'],
  },
  {
    diagnosisId: 'hidden',
    providerCode: 'BIZINFO_SUPPORT',
    dimension: 'support',
    sourceType: 'empty',
    publicDataDelta: 0,
    guardedDelta: 0,
  },
], { diagnosisLinkCount: 99 })

assert.equal(diagnosisLinks.count, 2)
assert.equal(diagnosisLinks.badgeLabel, '2건 연결')
assert.equal(diagnosisLinks.dimensionSummary, '시장성 1건 · 리콜 위험 1건')
assert.equal(diagnosisLinks.statusSummary, '실시간 연동 1건 · 조건 일치 항목 없음 1건')
assert.equal(diagnosisLinks.rows[0].impact.label, '긍정 근거 +4')
assert.equal(diagnosisLinks.rows[0].hasAdjustedImpact, false)
assert.equal(diagnosisLinks.rows[1].impact.label, '확인 필요 -1')
assert.equal(diagnosisLinks.rows[1].hasAdjustedImpact, true)
assert.equal(diagnosisLinks.rows[1].impactExplanation, '중복 근거 또는 신뢰도 보정으로 최종 반영값이 조정되었습니다.')

const emptyDiagnosisLinks = buildDiagnosisLinkSummary(null)
assert.equal(emptyDiagnosisLinks.count, 0)
assert.equal(emptyDiagnosisLinks.badgeLabel, '0건 연결')
assert.equal(emptyDiagnosisLinks.emptyMessage, '아직 이 진단에 연결된 근거 기록이 없습니다.')

assert.deepEqual(
  summarizeRecentRunComparison([
    { liveCount: 26, cacheCount: 13, fallbackCount: 0 },
    { liveCount: 0, cacheCount: 0, fallbackCount: 5 },
  ]).map((card) => card.value),
  ['100%', '+26건', '5건 감소', '13건'],
)

console.log('public data proof presenter checks passed')
