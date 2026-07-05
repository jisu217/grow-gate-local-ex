import assert from 'node:assert/strict'
import { buildDecisionBrief, mapDiagnosisDetail } from './diagnosisService.js'

const detail = {
  id: 'diag-1',
  productId: 'prod-1',
  productName: '리튬이온 배터리팩',
  targetCountry: 'DE',
  status: 'done',
  knowledgeSnapshot: {
    trustGrade: 'high',
    missingFields: [],
  },
  result: {
    diagnosisScore: {
      overall: 82,
      market: 78,
      regulation: 81,
      recall: 69,
      fto: 74,
      support: 88,
    },
    summary: 'source-traced readiness scoring result',
    gateDecision: {
      status: 'CONDITIONAL_GO',
      label: '조건부 진행',
      oneLineReason: 'FTO 보완 필요',
      mustFixBeforeQuote: ['KIPRIS 1차 검색'],
    },
    confidencePanel: {
      level: 'MEDIUM',
      liveSourceCount: 1,
      cacheSourceCount: 0,
      fallbackSourceCount: 1,
    },
    stageCards: {
      s04: [{ flag: 'DE', score: 78, publicSourceCount: 2, evidenceStrength: 4 }],
      s05: [{ name: 'EU Battery Regulation', targetCountry: 'DE', priorityLabel: '높음' }],
      s06: [{ title: 'Battery recall signal', similarity: 0.42, checks: ['SDS 확인'] }],
      s07: {
        patents: [{ name: 'BMS FTO', riskClass: 'm', badge: '검토' }],
        supports: [{ name: '수출바우처', org: '기업마당', dday: 'D-20', isGreen: true }],
      },
      s08: {
        thisWeek: [{ title: 'UN38.3 보유 여부 확인', owner: '품질 담당자', output: '시험성적서', dueDate: '2026-07-05' }],
        beforeQuote: [{ title: 'KIPRIS FTO 검색', owner: '대표/변리사', output: 'FTO 메모' }],
        beforeShipment: [{ title: '문서 ZIP 구성', owner: '해외영업', output: '바이어 제출 문서' }],
      },
      s10: [{
        provider: 'KCS_TRADE',
        sourceType: 'live',
        scoreContribution: 9,
        confidence: 0.82,
        metricSummary: 'exportAmountUsd=45678',
        decisionImpact: '시장성 점수 상향',
      }],
    },
    evidenceRetrieval: {
      mode: 'source-bound-evidence-search',
      claimBoundary: 'DB 지식과 source trace 안에서만 근거 chunk를 찾습니다.',
      topK: [{
        chunkId: 'public-data:1',
        sourceId: 'KCS_TRADE-1',
        sourceType: 'live',
        displayRole: '공공데이터 근거 chunk',
        score: 2.5,
        usedSentence: 'provider KCS_TRADE metrics exportAmountUsd=45678',
      }],
    },
    exportKnowledgeTimeline: [{
      eventId: 'EVT-KCS_TRADE-1',
      eventType: 'EXPORT_METRIC_SIGNAL',
      provider: 'KCS_TRADE',
      sourceType: 'live',
      usedFor: 'market',
      periodLabel: '2026-06-01 ~ 2026-06-30',
      summary: 'exportAmountUsd=45678',
      scoreImpact: 9,
    }],
    evidenceGuardrails: {
      verdict: 'pass',
      unsupportedClaimCount: 0,
      fallbackPositiveContributionCount: 0,
      officialLiveOrCacheRecordCount: 1,
    },
    sourceTrustSummary: {
      publicDataProofRunId: 'pdr_diag_1',
      publicDataCollectionRunId: 'run_diag_1',
      liveCount: 1,
      cacheCount: 0,
      fallbackCount: 1,
      externalFeedCount: 1,
      liveSuccessRate: 0.5,
      sourceQualityCounts: {
        LIVE_VERIFIED: 1,
        EMPTY_NOT_CONFIGURED: 1,
      },
      lastCollectedAt: '2026-07-03T09:00:00Z',
      sourcePeriod: '2026-06-01 ~ 2026-06-30',
      retryPayload: {
        hsCode: '850760',
        targetCountry: 'DE',
        productName: '리튬이온 배터리팩',
      },
      providerHealth: [{
        provider: 'KCS_TRADE',
        liveCount: 1,
        cacheCount: 0,
        fallbackCount: 0,
        status: 'live',
      }, {
        provider: 'SAFETY_RECALL',
        liveCount: 0,
        cacheCount: 0,
        fallbackCount: 1,
        status: 'fallback',
        failureReason: 'API key is not configured',
      }],
    },
    scoreContributionLedger: [{
      sourceId: 'KCS_TRADE:850760:DE',
      provider: 'KCS_TRADE',
      usedFor: 'market',
      sourceType: 'live',
      sourceQualityStatus: 'LIVE_VERIFIED',
      zeroReason: '',
      rawMetric: { exportAmountUsd: 45678 },
      normalizedMetric: { exportAmountUsd: 45678, yoyGrowthPct: 12.4 },
      rawContribution: 9,
      confidence: 0.82,
      adjustedContribution: 7,
      finalScoreDelta: 7,
      metricSummary: 'exportAmountUsd=45678, yoyGrowthPct=12.4',
    }],
  },
}

const mapped = mapDiagnosisDetail(detail)

assert.equal(mapped.session.id, 'diag-1')
assert.equal(mapped.session.productName, '리튬이온 배터리팩')
assert.equal(mapped.stages.s04[0].flag, 'DE')
assert.equal(mapped.stages.s04[0].score, 78)
assert.equal(mapped.stages.s05[0].nm, 'EU Battery Regulation')
assert.equal(mapped.stages.s06[0].title, 'Battery recall signal')
assert.equal(mapped.stages.s07.patents[0].name, 'BMS FTO')
assert.equal(mapped.stages.s07.supports[0].name, '수출바우처')
assert.equal(mapped.session.gateDecision.status, 'CONDITIONAL_GO')
assert.equal(mapped.session.confidencePanel.level, 'MEDIUM')
assert.equal(mapped.stages.s08[0].day, '이번 주')
assert.equal(mapped.stages.s08[0].tasks[0].title, 'UN38.3 보유 여부 확인')
assert.equal(mapped.session.evidenceRetrieval.mode, 'source-bound-evidence-search')
assert.equal(mapped.session.exportKnowledgeTimeline[0].eventType, 'EXPORT_METRIC_SIGNAL')
assert.equal(mapped.session.evidenceGuardrails.verdict, 'pass')
assert.equal(mapped.session.sourceTrustSummary.liveSuccessRate, 0.5)
assert.equal(mapped.session.sourceTrustSummary.publicDataProofRunId, 'pdr_diag_1')
assert.equal(mapped.session.sourceTrustSummary.providerHealth[1].failureReason, 'API key is not configured')
assert.equal(mapped.stages.s10.pipes[0].source, 'KCS_TRADE')
assert.equal(mapped.stages.s10.pipes[0].status, 'live')
assert.equal(mapped.stages.s10.pipes[0].metricSummary, 'exportAmountUsd=45678')
assert.equal(mapped.stages.s10.evidenceRetrieval.topK[0].sourceId, 'KCS_TRADE-1')
assert.equal(mapped.stages.s10.timeline[0].scoreImpact, 9)
assert.equal(mapped.stages.s10.ledger[0].rawMetric.exportAmountUsd, 45678)
assert.equal(mapped.stages.s10.ledger[0].finalScoreDelta, 7)
assert.equal(mapped.stages.s10.ledger[0].sourceQualityStatus, 'LIVE_VERIFIED')

const decisionBrief = buildDecisionBrief(mapped.session, mapped.stages)
assert.equal(decisionBrief.fallbackReviewRequired, true)
assert.match(decisionBrief.fallbackMessage, /담당자 검토/)
assert.equal(decisionBrief.proofRunId, 'pdr_diag_1')
assert.equal(decisionBrief.collectionRunId, 'run_diag_1')
assert.equal(decisionBrief.sourceQualityCounts.EMPTY_NOT_CONFIGURED, 1)
assert.equal(decisionBrief.notConfiguredCount, 1)
assert.equal(decisionBrief.topRisks.length, 3)
assert.deepEqual(
  decisionBrief.nextActions.map((action) => action.bucket),
  ['오늘 할 일', '이번 주 할 일', '외부기관 문의']
)
assert.equal(decisionBrief.nextActions[0].title, 'UN38.3 보유 여부 확인')
assert.equal(decisionBrief.nextActions[2].owner, '해외영업')

const sparseMapped = mapDiagnosisDetail({
  id: 'diag-empty',
  productId: 'prod-empty',
  productName: '신규 제품',
  targetCountry: 'US',
  status: 'running',
  knowledgeSnapshot: {},
  result: {},
})

assert.equal(sparseMapped.session.id, 'diag-empty')
assert.equal(sparseMapped.session.productName, '신규 제품')
assert.equal(sparseMapped.session.diagnosisScore.overall, 0)
assert.deepEqual(sparseMapped.session.evidenceSources, [])
assert.deepEqual(sparseMapped.session.riskItems, [])
assert.deepEqual(sparseMapped.session.actionRoadmap, {
  thisWeek: [],
  beforeQuote: [],
  beforeShipment: [],
})
assert.equal(sparseMapped.kpiData[0].score, 0)

const sourceRestored = mapDiagnosisDetail({
  id: 'diag-source-restore',
  productName: '전장 모듈',
  targetCountry: 'DE',
  status: 'completed',
  knowledgeSnapshot: {},
  result: {
    diagnosisScore: { overall: 70 },
    stageCards: {},
  },
}, null, [{
  sourceId: 'KCS_TRADE-record-1',
  provider: 'KCS_TRADE',
  sourceType: 'live',
  sourceQualityStatus: 'LIVE_VERIFIED',
  usedFor: 'market',
  scoreContribution: 7,
  rawScoreContribution: 9,
  confidenceAdjustedContribution: 7,
  confidence: 0.82,
  collectionRunId: 'run-1',
  operationEndpoint: true,
  apiKeyConfigured: true,
  apiKeyStored: false,
  endpointType: 'operation',
  fetchedAt: '2026-07-03T09:00:00Z',
  sourcePeriodStart: '2026-06-01',
  sourcePeriodEnd: '2026-06-30',
  metricSummary: 'exportAmountUsd=45678',
  decisionImpact: '시장성 +7',
  metrics: { exportAmountUsd: 45678 },
  collectionEvidence: {
    operationEndpoint: true,
    apiKeyConfigured: true,
    endpointType: 'operation',
  },
}, {
  sourceId: 'SAFETY_RECALL-record-2',
  provider: 'SAFETY_RECALL',
  sourceType: 'fallback',
  sourceQualityStatus: 'FALLBACK_ERROR',
  usedFor: 'recall',
  fallbackUsed: true,
  scoreContribution: 0,
  confidence: 0.2,
  operationEndpoint: false,
  apiKeyConfigured: false,
  endpointType: 'catalog',
  fetchedAt: '2026-07-03T09:01:00Z',
  metricSummary: 'fallback',
  decisionImpact: '담당자 검토 필요',
}])

assert.equal(sourceRestored.session.sourceTrustSummary.liveCount, 1)
assert.equal(sourceRestored.session.sourceTrustSummary.fallbackCount, 1)
assert.equal(sourceRestored.session.sourceTrustSummary.operationEndpointLiveCount, 1)
assert.equal(sourceRestored.session.sourceTrustSummary.apiKeyConfiguredLiveCount, 1)
assert.equal(sourceRestored.session.sourceTrustSummary.providerHealth[0].operationEndpoint, true)
assert.equal(sourceRestored.session.sourceTrustSummary.providerHealth[1].apiKeyConfigured, false)
assert.equal(sourceRestored.stages.s10.ledger[0].operationEndpoint, true)
assert.equal(sourceRestored.stages.s10.ledger[0].apiKeyConfigured, true)
assert.equal(sourceRestored.stages.s10.ledger[0].endpointType, 'operation')
assert.equal(sourceRestored.stages.s10.ledger[0].rawMetric.exportAmountUsd, 45678)
assert.equal(sourceRestored.stages.s10.ledger[0].sourceQualityStatus, 'LIVE_VERIFIED')

const noPublicDataSources = [
  {
    sourceId: 'MARKET-empty',
    provider: 'KOTRA_MARKET',
    sourceType: 'empty',
    sourceQualityStatus: 'EMPTY_NOT_CONFIGURED',
    usedFor: 'market',
    scoreContribution: 0,
    confidence: 0,
    operationEndpoint: false,
    apiKeyConfigured: false,
    endpointType: 'catalog_or_application_page',
    fetchedAt: '2026-07-05T02:00:00Z',
    metricSummary: 'recordCount=0',
    decisionImpact: '시장성 live metric 미확보',
    errorMessage: 'CATALOG_PAGE_ENDPOINT',
    zeroReason: 'CATALOG_PAGE_ENDPOINT',
  },
  {
    sourceId: 'REG-fallback',
    provider: 'KCS_TRADE',
    sourceType: 'fallback',
    sourceQualityStatus: 'FALLBACK_ERROR',
    usedFor: 'regulation',
    fallbackUsed: true,
    scoreContribution: -1,
    confidence: 0,
    operationEndpoint: true,
    apiKeyConfigured: true,
    endpointType: 'operation',
    fetchedAt: '2026-07-05T02:01:00Z',
    metricSummary: 'timeout',
    decisionImpact: '규제 근거 담당자 확인 필요',
    errorMessage: 'TIMEOUT',
  },
  {
    sourceId: 'RECALL-empty',
    provider: 'SAFETY_RECALL',
    sourceType: 'empty',
    sourceQualityStatus: 'EMPTY_NOT_CONFIGURED',
    usedFor: 'recall',
    scoreContribution: 0,
    confidence: 0,
    operationEndpoint: false,
    apiKeyConfigured: false,
    endpointType: 'catalog_or_application_page',
    fetchedAt: '2026-07-05T02:02:00Z',
    metricSummary: 'recordCount=0',
    decisionImpact: '리콜 live metric 미확보',
    errorMessage: 'API key is not configured',
    zeroReason: 'key not configured',
  },
  {
    sourceId: 'FTO-fallback',
    provider: 'KIPRIS_FTO',
    sourceType: 'fallback',
    sourceQualityStatus: 'EMPTY_NOT_CONFIGURED',
    usedFor: 'fto',
    fallbackUsed: true,
    scoreContribution: -3,
    confidence: 0,
    operationEndpoint: true,
    apiKeyConfigured: false,
    endpointType: 'operation',
    fetchedAt: '2026-07-05T02:03:00Z',
    metricSummary: 'patentCount unavailable',
    decisionImpact: 'FTO 담당자/변리사 확인 필요',
    errorMessage: 'API key is not configured',
  },
  {
    sourceId: 'SUPPORT-fallback',
    provider: 'BIZINFO_SUPPORT',
    sourceType: 'fallback',
    sourceQualityStatus: 'EMPTY_NOT_CONFIGURED',
    usedFor: 'support',
    fallbackUsed: true,
    scoreContribution: -1,
    confidence: 0,
    operationEndpoint: false,
    apiKeyConfigured: false,
    endpointType: 'catalog_or_application_page',
    fetchedAt: '2026-07-05T02:04:00Z',
    metricSummary: 'support item unavailable',
    decisionImpact: '지원사업 담당자 확인 필요',
    errorMessage: 'CATALOG_PAGE_ENDPOINT',
  },
]

const allBucketsNoData = mapDiagnosisDetail({
  id: 'diag-all-buckets-no-data',
  productName: '의료용 센서 모듈',
  targetCountry: 'JP',
  status: 'completed',
  createdAt: '2026-07-05T02:05:00Z',
  knowledgeSnapshot: {},
  result: {
    diagnosisScore: { overall: 52, market: 45, regulation: 48, recall: 50, fto: 44, support: 51 },
    gateDecision: {
      status: 'HOLD',
      finalGate: 'HOLD',
      fallbackCount: 3,
      llmInfluencedGate: false,
    },
    stageCards: {
      s10: noPublicDataSources,
    },
  },
}, null, noPublicDataSources)

assert.equal(allBucketsNoData.session.sourceTrustSummary.liveCount, 0)
assert.equal(allBucketsNoData.session.sourceTrustSummary.cacheCount, 0)
assert.equal(allBucketsNoData.session.sourceTrustSummary.fallbackCount, 3)
assert.equal(allBucketsNoData.session.sourceTrustSummary.emptyCount, 2)
assert.equal(allBucketsNoData.session.sourceTrustSummary.liveSuccessRate, 0)
assert.equal(allBucketsNoData.session.sourceTrustSummary.providerHealth.length, 5)
assert.deepEqual(
  allBucketsNoData.session.sourceTrustSummary.providerHealth.map((row) => row.status),
  ['empty', 'fallback', 'empty', 'fallback', 'fallback']
)
assert.deepEqual(
  allBucketsNoData.stages.s10.pipes.map((pipe) => pipe.status),
  ['empty', 'fallback', 'empty', 'fallback', 'fallback']
)
assert.deepEqual(
  allBucketsNoData.stages.s10.ledger.map((row) => row.usedFor),
  ['market', 'regulation', 'recall', 'fto', 'support']
)
assert.deepEqual(
  allBucketsNoData.stages.s10.ledger.map((row) => row.sourceQualityStatus),
  ['EMPTY_NOT_CONFIGURED', 'FALLBACK_ERROR', 'EMPTY_NOT_CONFIGURED', 'EMPTY_NOT_CONFIGURED', 'EMPTY_NOT_CONFIGURED']
)
assert.equal(allBucketsNoData.stages.s10.pipes[0].sourceQualityStatus, 'EMPTY_NOT_CONFIGURED')
assert.equal(allBucketsNoData.stages.s10.pipes[0].zeroReason, 'CATALOG_PAGE_ENDPOINT')
assert.equal(allBucketsNoData.session.gateDecision.llmInfluencedGate, false)
