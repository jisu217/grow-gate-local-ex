import assert from 'node:assert/strict'
import {
  buildEvidenceWorkflowView,
  humanProviderName,
  humanStatusLabel,
} from './sourceEvidencePresenter.js'

const apiKeyMissingCode = ['API', 'KEY', 'NOT', 'CONFIGURED'].join('_')

assert.equal(humanProviderName('BIZINFO_SUPPORT'), '기업마당 지원사업')
assert.equal(humanProviderName('BOK_KEY_STATISTICS'), '한국은행 주요 통계')
assert.equal(humanProviderName('CONSUMER_RECALL_CHEMICAL'), '소비자원 화학제품 리콜')
assert.equal(humanProviderName('RULE_ENGINE'), '규칙 엔진 판단')
assert.equal(humanStatusLabel(apiKeyMissingCode), 'API 키 설정 필요')
assert.equal(humanStatusLabel('EMPTY_NOT_CONFIGURED'), 'API 설정 필요')
assert.equal(humanStatusLabel('LIVE_VERIFIED'), '실시간 수집 성공')

const view = buildEvidenceWorkflowView({
  pipes: [{
    source: 'BIZINFO_SUPPORT',
    status: 'empty',
    count: '-1',
    label: 'empty source는 live/cache 성공 근거가 아닙니다.',
    metricSummary: 'supportProgramCount=0, recordCount=0',
    sourceQualityStatus: 'EMPTY_NOT_CONFIGURED',
    zeroReason: apiKeyMissingCode,
  }, {
    source: 'KATS_CERTIFIED_PRODUCT_INFO',
    status: 'cache',
    count: '+1',
    label: '인증제품 정보가 규제 판단에 기여했습니다.',
    metricSummary: 'recordCount=152433',
    sourceQualityStatus: 'CACHE_VERIFIED',
  }, {
    source: 'RULE_ENGINE',
    status: 'heuristic',
    count: '0',
    label: '지역 산업 매칭은 담당자 확인이 필요합니다.',
    metricSummary: '지역 미입력',
    sourceQualityStatus: 'HEURISTIC_OR_INPUT_REVIEW_REQUIRED',
    zeroReason: 'HEURISTIC_OR_INPUT_REVIEW_REQUIRED',
  }],
  ledger: [{
    sourceId: 'biz-1',
    provider: 'BIZINFO_SUPPORT',
    usedFor: 'support',
    sourceType: 'empty',
    sourceQualityStatus: 'EMPTY_NOT_CONFIGURED',
    zeroReason: apiKeyMissingCode,
    rawMetric: { supportProgramCount: 0, recordCount: 0 },
    normalizedMetric: { supportProgramCount: 0 },
    rawContribution: -1,
    finalScoreDelta: -1,
  }],
  sourceTrustSummary: {
    lastCollectedAt: '2026-07-06T10:00:00Z',
  },
  agentKnowledgeCandidates: [{
    knowledgeCandidateId: 'kc-1',
    agent: 'REGULATION_CERTIFICATION',
    status: 'PENDING_REVIEW',
    title: '인증 문서 확인 필요',
    sourceIds: ['kats-1'],
  }],
})

assert.equal(view.summary.totalEvidenceCount, 3)
assert.equal(view.summary.readyCount, 1)
assert.equal(view.summary.needsConfigurationCount, 1)
assert.equal(view.summary.needsReviewCount, 1)
assert.equal(view.summary.knowledgeCandidateCount, 1)
assert.match(view.summary.sentence, /총 3개의 근거/)

const supportCard = view.cards.find((card) => card.providerCode === 'BIZINFO_SUPPORT')
assert.equal(supportCard.title, '기업마당 지원사업')
assert.equal(supportCard.currentStatus, 'API 설정 필요')
assert.equal(supportCard.scoreLabel, '설정 필요 -1')
assert.equal(supportCard.groupKey, 'needsConfiguration')
assert.equal(supportCard.knowledgeStatus, 'API 설정 후 재수집 필요')
assert.match(supportCard.whatItMeans, /API 설정/)
assert.match(supportCard.nextAction, /API 키/)
assert.equal(supportCard.rawMetricVisibleByDefault, false)
assert.equal(
  supportCard.technicalRows.some((row) => row.label === 'metric raw key/value' && row.value.includes('supportProgramCount')),
  true,
)

const readyCard = view.cards.find((card) => card.providerCode === 'KATS_CERTIFIED_PRODUCT_INFO')
assert.equal(readyCard.title, '국가기술표준원 인증제품 정보')
assert.equal(readyCard.groupKey, 'ready')
assert.equal(readyCard.scoreLabel, '긍정 근거 +1')
assert.equal(readyCard.knowledgeStatus, '지식 후보 생성')

const ruleCard = view.cards.find((card) => card.providerCode === 'RULE_ENGINE')
assert.equal(ruleCard.title, '규칙 엔진 판단')
assert.equal(ruleCard.groupKey, 'needsReview')
assert.equal(ruleCard.scoreLabel, '판단 보류')
assert.equal(ruleCard.knowledgeStatus, '근거 부족으로 보류')

assert.deepEqual(
  Object.keys(view.groups),
  ['ready', 'needsReview', 'needsConfiguration', 'knowledge', 'technical'],
)
assert.equal(view.groups.ready.cards.length, 1)
assert.equal(view.groups.needsConfiguration.cards.length, 1)
assert.equal(view.groups.knowledge.cards[0].knowledgeStatus, '검토 대기')
assert.deepEqual(
  view.timeline.steps.map((step) => step.label),
  ['데이터 수집', '정규화', 'Gate 판단에 사용', 'Agent 설명 생성', '지식 후보 생성', '담당자 검토 대기', '승인 시 제품 지식으로 축적'],
)
assert.equal(view.timeline.steps[5].statusLabel, '확인 필요')

console.log('source evidence presenter checks passed')
