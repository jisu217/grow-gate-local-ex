// 진단 서비스
import { apiFetch } from './apiClient.js'

const EMPTY_SCORE = {
  overall: 0,
  market: 0,
  regulation: 0,
  recall: 0,
  fto: 0,
  support: 0,
}

const EMPTY_ACTION_ROADMAP = {
  thisWeek: [],
  beforeQuote: [],
  beforeShipment: [],
}

const EMPTY_SESSION = {
  id: null,
  status: null,
  productId: null,
  companyId: null,
  knowledgeSnapshotId: null,
  productName: '',
  targetMarket: '',
  trustGrade: null,
  missingFields: [],
  evidenceSources: [],
  updatedAt: null,
  diagnosisScore: EMPTY_SCORE,
  riskItems: [],
  actionRoadmap: EMPTY_ACTION_ROADMAP,
  gateDecision: null,
  impactEstimate: null,
  confidencePanel: null,
  agentTrace: null,
  evidenceRetrieval: null,
  exportKnowledgeTimeline: [],
  evidenceGuardrails: null,
  sourceTrustSummary: null,
  regionalIndustryProfile: null,
  scoreContributionLedger: [],
  normalizedMetrics: null,
  pythonWorker: null,
  pythonAgenticLoop: null,
  reportMarkdown: '',
  reportFastApiUsed: false,
}

const EMPTY_METRICS = [
  { step: '01', label: 'Input', score: 0 },
  { step: '02', label: 'Gate', score: 0 },
  { step: '03', label: 'Sources', score: 0 },
]

// 진단 이력 목록 항목 매핑 (백엔드 요약 응답 → FE 카드 형태)
// 백엔드 목록 응답에 없는 diagnosisScore/summary는 null 처리.
function mapDiagnosisSummary(d) {
  return {
    id: d.id,
    productId: d.productId,
    companyId: null,
    knowledgeSnapshotId: null,
    trustGrade: null,
    missingFields: null,
    evidenceSources: null,
    productName: d.productName,
    hsCode: d.hsCode ?? '',
    targetMarket: d.targetCountry,
    status: d.status,
    requestedAt: d.createdAt,
    updatedAt: null,
    diagnosisScore: d.overallScore ?? null,
    riskItems: null,
    actionRoadmap: null,
    summary: null,
  }
}

// 진단 이력 목록 조회
export async function getDiagnosisList() {
  const res = await apiFetch('/diagnosis')
  return res.diagnoses.map(mapDiagnosisSummary)
}

// 진단 결과 상세 매핑
export function mapDiagnosisDetail(d, evidenceSources, sourceEntries = []) {
  const diagnosisScore = normalizeDiagnosisScore(d.result?.diagnosisScore)
  const overallScore = typeof diagnosisScore === 'number'
    ? diagnosisScore
    : diagnosisScore?.overall ?? 0
  const stageCards = d.result?.stageCards ?? {}
  const restoredSourceTrustSummary = d.result?.sourceTrustSummary
    ?? buildSourceTrustSummaryFromSources(sourceEntries, d)
  const restoredLedger = Array.isArray(d.result?.scoreContributionLedger) && d.result.scoreContributionLedger.length > 0
    ? d.result.scoreContributionLedger
    : buildLedgerFromSources(sourceEntries)
  const session = {
    ...EMPTY_SESSION,
    id: d.id,
    status: d.status,
    productId: d.productId,
    companyId: d.companyId,
    knowledgeSnapshotId: d.knowledgeSnapshotId,
    productName: d.productName,
    targetMarket: d.targetCountry,
    trustGrade: d.knowledgeSnapshot?.trustGrade ?? null,
    missingFields: d.knowledgeSnapshot?.missingFields ?? [],
    evidenceSources: evidenceSources ?? d.result?.evidenceSources ?? [],
    updatedAt: d.createdAt,
    diagnosisScore,
    riskItems: d.result?.riskItems ?? [],
    actionRoadmap: d.result?.actionRoadmap ?? EMPTY_ACTION_ROADMAP,
    gateDecision: d.result?.gateDecision ?? null,
    impactEstimate: d.result?.impactEstimate ?? null,
    confidencePanel: d.result?.confidencePanel ?? null,
    agentTrace: d.result?.agentTrace ?? null,
    evidenceRetrieval: d.result?.evidenceRetrieval ?? null,
    exportKnowledgeTimeline: Array.isArray(d.result?.exportKnowledgeTimeline) ? d.result.exportKnowledgeTimeline : [],
    evidenceGuardrails: d.result?.evidenceGuardrails ?? null,
    sourceTrustSummary: restoredSourceTrustSummary,
    regionalIndustryProfile: d.result?.regionalIndustryProfile ?? null,
    scoreContributionLedger: restoredLedger,
    normalizedMetrics: d.result?.normalizedMetrics ?? null,
    pythonWorker: d.result?.pythonWorker ?? null,
    pythonAgenticLoop: d.result?.pythonAgenticLoop ?? null,
    reportMarkdown: d.result?.reportMarkdown ?? '',
    reportFastApiUsed: Boolean(d.result?.reportFastApiUsed),
  }

  return {
    session,
    kpiData: EMPTY_METRICS.map((kpi) =>
      kpi.step === '02' ? { ...kpi, score: overallScore } : kpi
    ),
    stages: {
      s00: mapOverviewStage(stageCards.s00, overallScore),
      s01: mapInputStage(stageCards.s01, d),
      s02: mapConnectorStage(stageCards.s02),
      s03: mapPredictionStage(stageCards.s03),
      s04: mapMarketStage(stageCards.s04),
      s05: mapRegulationStage(stageCards.s05),
      s06: mapRecallStage(stageCards.s06),
      s07: mapFtoSupportStage(stageCards.s07),
      s08: mapRoadmapStage(stageCards.s08),
      s09: mapReportStage(stageCards.s09, diagnosisScore, d.result?.summary),
      s10: mapSourceStage(stageCards.s10, {
        evidenceRetrieval: session.evidenceRetrieval,
        exportKnowledgeTimeline: session.exportKnowledgeTimeline,
        evidenceGuardrails: session.evidenceGuardrails,
        scoreContributionLedger: session.scoreContributionLedger,
        regionalIndustryProfile: session.regionalIndustryProfile,
      }),
    },
  }
}

export function buildDecisionBrief(session = {}, stages = {}) {
  const sourceSummary = session.sourceTrustSummary ?? {}
  const fallbackCount = Number(
    sourceSummary.fallbackCount
      ?? session.confidencePanel?.fallbackSourceCount
      ?? 0
  )
  const liveCount = Number(
    sourceSummary.liveCount
      ?? session.confidencePanel?.liveSourceCount
      ?? 0
  )
  const cacheCount = Number(
    sourceSummary.cacheCount
      ?? session.confidencePanel?.cacheSourceCount
      ?? 0
  )
  const gateDecision = session.gateDecision ?? {}
  const topRisks = normalizeTopRisks(session.riskItems, gateDecision.mustFixBeforeQuote)
  const nextActions = normalizeNextActions(stages.s08, gateDecision.mustFixBeforeQuote, topRisks)

  return {
    gateLabel: gateDecision.label ?? gateDecision.status ?? '조건부 검토',
    gateReason: gateDecision.oneLineReason ?? '견적 확정 전 source trace와 담당자 확인이 필요합니다.',
    fallbackReviewRequired: fallbackCount > 0,
    fallbackMessage: fallbackCount > 0
      ? `fallback 출처 ${fallbackCount}건이 포함되어 담당자 검토 후 바이어 회신을 확정해야 합니다.`
      : 'live/cache 출처 중심으로 판단 근거가 구성되었습니다.',
    sourceCounts: { live: liveCount, cache: cacheCount, fallback: fallbackCount },
    topRisks,
    nextActions,
  }
}

export function buildSourceTrustSummaryFromSources(sourceEntries = [], detail = {}) {
  const entries = Array.isArray(sourceEntries) ? sourceEntries : []
  if (entries.length === 0) return null
  const byProvider = new Map()
  let liveCount = 0
  let cacheCount = 0
  let fallbackCount = 0
  let emptyCount = 0
  let crawlCount = 0
  let externalFeedCount = 0
  let operationEndpointLiveCount = 0
  let apiKeyConfiguredLiveCount = 0
  const collected = []
  const periodStarts = []
  const periodEnds = []
  const collectionRunIds = new Set()

  entries.forEach((entry) => {
    const sourceType = entry.sourceType ?? 'fallback'
    if (sourceType === 'live') liveCount += 1
    if (sourceType === 'cache') cacheCount += 1
    if (sourceType === 'fallback') fallbackCount += 1
    if (sourceType === 'empty') emptyCount += 1
    if (sourceType === 'crawl') crawlCount += 1
    if (sourceType === 'external_feed' || sourceType === 'external_reconstructed') externalFeedCount += 1
    const isLiveOrCache = sourceType === 'live' || sourceType === 'cache'
    if (isLiveOrCache && Boolean(entry.operationEndpoint ?? entry.collectionEvidence?.operationEndpoint)) {
      operationEndpointLiveCount += 1
    }
    if (isLiveOrCache && Boolean(entry.apiKeyConfigured ?? entry.collectionEvidence?.apiKeyConfigured)) {
      apiKeyConfiguredLiveCount += 1
    }
    if (entry.fetchedAt || entry.retrievedAt) collected.push(entry.fetchedAt ?? entry.retrievedAt)
    if (entry.sourcePeriodStart) periodStarts.push(entry.sourcePeriodStart)
    if (entry.sourcePeriodEnd) periodEnds.push(entry.sourcePeriodEnd)
    if (entry.collectionRunId) collectionRunIds.add(entry.collectionRunId)

    const provider = entry.provider ?? entry.datasetName ?? 'UNKNOWN'
    const operationEndpoint = Boolean(entry.operationEndpoint ?? entry.collectionEvidence?.operationEndpoint)
    const apiKeyConfigured = Boolean(entry.apiKeyConfigured ?? entry.collectionEvidence?.apiKeyConfigured)
    const row = byProvider.get(provider) ?? {
      provider,
      liveCount: 0,
      cacheCount: 0,
      fallbackCount: 0,
      emptyCount: 0,
      crawlCount: 0,
      externalFeedCount: 0,
      operationEndpoint: false,
      apiKeyConfigured: false,
      operationEndpointLiveCount: 0,
      apiKeyConfiguredLiveCount: 0,
      endpointType: '',
      lastCollectedAt: '',
      sourcePeriodStart: '',
      sourcePeriodEnd: '',
      failureReason: '',
      status: 'no_record',
    }
    if (sourceType === 'live') row.liveCount += 1
    if (sourceType === 'cache') row.cacheCount += 1
    if (sourceType === 'fallback') row.fallbackCount += 1
    if (sourceType === 'empty') row.emptyCount += 1
    if (sourceType === 'crawl') row.crawlCount += 1
    if (sourceType === 'external_feed' || sourceType === 'external_reconstructed') row.externalFeedCount += 1
    row.operationEndpoint = row.operationEndpoint || operationEndpoint
    row.apiKeyConfigured = row.apiKeyConfigured || apiKeyConfigured
    if (isLiveOrCache && operationEndpoint) row.operationEndpointLiveCount += 1
    if (isLiveOrCache && apiKeyConfigured) row.apiKeyConfiguredLiveCount += 1
    row.endpointType = row.endpointType || entry.endpointType || entry.collectionEvidence?.endpointType || ''
    row.lastCollectedAt = maxDateString(row.lastCollectedAt, entry.fetchedAt ?? entry.retrievedAt)
    row.sourcePeriodStart = minDateString(row.sourcePeriodStart, entry.sourcePeriodStart)
    row.sourcePeriodEnd = maxDateString(row.sourcePeriodEnd, entry.sourcePeriodEnd)
    row.failureReason = row.failureReason || entry.errorMessage || entry.reason || ''
    row.status = row.fallbackCount > 0 && row.liveCount + row.cacheCount + row.crawlCount + row.externalFeedCount === 0 ? 'fallback'
      : row.fallbackCount > 0 ? 'partial'
        : row.liveCount > 0 ? 'live'
          : row.cacheCount > 0 ? 'cache'
            : row.externalFeedCount > 0 ? 'external_feed'
              : row.crawlCount > 0 ? 'crawl'
                : row.emptyCount > 0 ? 'empty' : 'no_record'
    byProvider.set(provider, row)
  })

  const officialAttemptCount = liveCount + cacheCount + fallbackCount
  const liveSuccessRate = officialAttemptCount === 0 ? 0 : Math.round(((liveCount + cacheCount) / officialAttemptCount) * 1000) / 1000
  const sortedCollected = collected.filter(Boolean).sort()
  const sortedStarts = periodStarts.filter(Boolean).sort()
  const sortedEnds = periodEnds.filter(Boolean).sort()

  return {
    diagnosisId: detail.id,
    createdAt: detail.createdAt,
    collectionRunIds: [...collectionRunIds],
    liveCount,
    cacheCount,
    fallbackCount,
    emptyCount,
    crawlCount,
    externalFeedCount,
    operationEndpointLiveCount,
    apiKeyConfiguredLiveCount,
    liveSuccessRate,
    lastCollectedAt: sortedCollected.at(-1) ?? '',
    sourcePeriodStart: sortedStarts[0] ?? '',
    sourcePeriodEnd: sortedEnds.at(-1) ?? '',
    sourcePeriod: sortedStarts.length && sortedEnds.length ? `${sortedStarts[0]} ~ ${sortedEnds.at(-1)}` : 'source period 확인 대기',
    providerHealth: [...byProvider.values()],
    sourceIds: entries.map((entry) => entry.sourceId).filter(Boolean).slice(0, 12),
  }
}

export function buildLedgerFromSources(sourceEntries = []) {
  const entries = Array.isArray(sourceEntries) ? sourceEntries : []
  return entries
    .filter((entry) => entry.sourceId || entry.provider)
    .map((entry) => ({
      sourceId: entry.sourceId ?? entry.recordId ?? `${entry.provider}-${entry.usedFor}`,
      provider: entry.provider ?? entry.datasetName ?? '',
      usedFor: entry.usedFor ?? entry.field ?? '',
      sourceType: entry.sourceType ?? 'fallback',
      operationEndpoint: Boolean(entry.operationEndpoint ?? entry.collectionEvidence?.operationEndpoint),
      apiKeyConfigured: Boolean(entry.apiKeyConfigured ?? entry.collectionEvidence?.apiKeyConfigured),
      endpointType: entry.endpointType ?? entry.collectionEvidence?.endpointType ?? '',
      fetchedAt: entry.fetchedAt ?? entry.retrievedAt ?? '',
      rawMetric: entry.rawMetric ?? entry.metrics ?? entry.scoreInputs ?? {},
      normalizedMetric: entry.normalizedMetric ?? entry.metrics ?? entry.scoreInputs ?? {},
      rawContribution: entry.rawScoreContribution ?? entry.scoreContribution ?? 0,
      confidence: entry.confidence ?? 0,
      adjustedContribution: entry.confidenceAdjustedContribution ?? entry.scoreContribution ?? 0,
      finalScoreDelta: entry.scoreContribution ?? entry.confidenceAdjustedContribution ?? 0,
      metricSummary: entry.metricSummary ?? '',
      decisionImpact: entry.decisionImpact ?? '',
    }))
}

function normalizeTopRisks(riskItems, mustFixBeforeQuote) {
  const risks = Array.isArray(riskItems) ? riskItems : []
  const mappedRisks = risks
    .map((item, index) => ({
      level: String(item.level ?? (index === 0 ? 'high' : 'medium')).toLowerCase(),
      label: item.label ?? item.title ?? String(item),
    }))
    .filter((item) => item.label)

  const mustFixRisks = (Array.isArray(mustFixBeforeQuote) ? mustFixBeforeQuote : [])
    .map((item) => ({ level: 'high', label: item }))

  const unique = [...mappedRisks, ...mustFixRisks].filter((item, index, list) =>
    list.findIndex((candidate) => candidate.label === item.label) === index
  )
  const fallbackRisks = [
    { level: 'medium', label: 'fallback 또는 cache 출처는 담당자가 원문 최신성을 확인해야 합니다.' },
    { level: 'medium', label: 'FTO, 리콜, 인증 항목은 외부 전문가 확인 전 확정 표현을 피해야 합니다.' },
    { level: 'low', label: '바이어 회신 전 HS Code, 대상국, 제품 사양 변경 여부를 재확인해야 합니다.' },
  ]
  const padded = [...unique]
  fallbackRisks.forEach((item) => {
    if (padded.length < 3 && !padded.some((risk) => risk.label === item.label)) {
      padded.push(item)
    }
  })

  return padded.slice(0, 3).length
    ? padded.slice(0, 3)
    : [{ level: 'medium', label: 'source trace와 등록 제품 지식을 담당자가 최종 확인해야 합니다.' }]
}

function normalizeNextActions(roadmapStage, mustFixBeforeQuote, topRisks) {
  const roadmap = Array.isArray(roadmapStage) ? roadmapStage : []
  const firstTask = (label) => roadmap
    .find((bucket) => bucket.day === label || bucket.label === label)
    ?.tasks?.find((task) => task?.title)

  const todayTask = firstTask('이번 주') ?? roadmap[0]?.tasks?.find((task) => task?.title)
  const quoteTask = firstTask('견적 전') ?? roadmap[1]?.tasks?.find((task) => task?.title)
  const externalTask = firstTask('출하 전') ?? roadmap[2]?.tasks?.find((task) => task?.title)
  const mustFix = Array.isArray(mustFixBeforeQuote) && mustFixBeforeQuote[0]
    ? mustFixBeforeQuote[0]
    : null
  const risk = topRisks[0]?.label ?? '고위험 항목'

  return [
    {
      bucket: '오늘 할 일',
      title: todayTask?.title ?? mustFix ?? '바이어 문의 조건과 현재 보유 문서를 확인합니다.',
      owner: todayTask?.who ?? todayTask?.owner ?? '수출 담당자',
      output: todayTask?.output ?? '회신 가능/보류 판단',
    },
    {
      bucket: '이번 주 할 일',
      title: quoteTask?.title ?? '견적 전 필수 보완 항목을 완료합니다.',
      owner: quoteTask?.who ?? quoteTask?.owner ?? '품질/인증 담당자',
      output: quoteTask?.output ?? '보완 증빙 또는 검토 메모',
    },
    {
      bucket: '외부기관 문의',
      title: externalTask?.title ?? `${risk}에 대해 인증기관·변리사·지원기관에 확인합니다.`,
      owner: externalTask?.who ?? externalTask?.owner ?? '담당자 + 외부 전문가',
      output: externalTask?.output ?? '외부 확인 기록',
    },
  ]
}

function maxDateString(current, candidate) {
  if (!candidate) return current || ''
  if (!current) return candidate
  return String(candidate) > String(current) ? candidate : current
}

function minDateString(current, candidate) {
  if (!candidate) return current || ''
  if (!current) return candidate
  return String(candidate) < String(current) ? candidate : current
}

function mapInputStage(stageData, detail) {
  if (Array.isArray(stageData) && stageData.length > 0) return stageData
  return [
    { key: 'Product', val: detail.productName ?? '' },
    { key: 'Target', val: detail.targetCountry ?? '' },
    { key: 'Status', val: detail.status ?? 'pending' },
  ]
}

function mapOverviewStage(stageData, overallScore) {
  if (!Array.isArray(stageData) || stageData.length === 0) {
    return [{
      title: 'Gate score',
      value: String(overallScore),
      unit: '점',
      desc: '백엔드 진단 결과를 기다리거나 source trace를 확인하세요.',
    }]
  }
  return stageData.map((item) => ({
    title: item.label ?? item.title ?? '진단 항목',
    value: String(item.value ?? ''),
    unit: item.unit ?? '',
    desc: item.note ?? item.desc ?? '',
  }))
}

function mapConnectorStage(stageData) {
  if (!Array.isArray(stageData) || stageData.length === 0) {
    return [{
      no: '01',
      nm: 'Source trace pending',
      meta: 'backend stageCards.s02 미수신',
    }]
  }
  return stageData.map((item, index) => ({
    no: String(index + 1).padStart(2, '0'),
    nm: item.name ?? item.provider ?? '데이터 출처',
    meta: `${item.provider ?? 'provider'} · ${item.status ?? 'connected'} · ${item.contribution ?? 0}점`,
  }))
}

function mapPredictionStage(stageData) {
  if (!stageData?.predictions) {
    return {
      predictions: [{
        head: 'Rule',
        acc: 'API',
        title: '준비도 점수 대기',
        value: 0,
        unit: '점',
        bar: 0,
        desc: '백엔드 source-traced scoring 결과가 아직 없습니다.',
      }],
      features: [],
    }
  }
  const predictions = stageData.predictions.map((item) => ({
    head: 'Rule',
    acc: 'API',
    title: item.name,
    value: item.value,
    unit: '점',
    bar: item.value,
    desc: '백엔드 진단 점수 산식에서 생성된 항목별 점수입니다.',
  }))
  const features = (stageData.features ?? []).map((label, index) => ({
    label,
    value: Math.max(30, 90 - index * 15),
  }))
  return { predictions, features }
}

function mapMarketStage(stageData) {
  if (!Array.isArray(stageData) || stageData.length === 0) {
    return [{
      flag: 'API result pending',
      score: 0,
      grade: 'l',
      gradeLabel: 'NO DATA',
      marketSize: '0 live/cache',
      growth: 'evidence 0/5',
    }]
  }
  return stageData.map((item, index) => ({
    flag: item.flag ?? item.country ?? `Market ${index + 1}`,
    score: Number(item.score ?? 0),
    grade: item.grade ?? gradeClass(Number(item.score ?? 0)),
    gradeLabel: item.gradeLabel ?? gradeLabel(Number(item.score ?? 0)),
    marketSize: item.marketSize ?? `${item.publicSourceCount ?? 0} live/cache`,
    growth: item.growth ?? `evidence ${item.evidenceStrength ?? 0}/5`,
  }))
}

function mapRegulationStage(stageData) {
  if (!Array.isArray(stageData) || stageData.length === 0) {
    return [{
      nm: 'API 규제 결과 없음',
      mkt: '진단 완료 후 source trace를 확인하세요.',
      market: ['warn', '-'],
      priority: ['warn', '확인'],
      status: ['warn', '대기'],
      duration: '-',
      cost: '-',
    }]
  }
  return stageData.map((item) => ({
    nm: item.nm ?? item.name ?? '규제 항목',
    mkt: item.mkt ?? item.description ?? '',
    market: normalizeTuple(item.market, 'warn', item.targetCountry ?? '-'),
    priority: normalizeTuple(item.priority, 'warn', item.priorityLabel ?? '확인'),
    status: normalizeTuple(item.status, 'warn', item.statusLabel ?? '검토'),
    duration: item.duration ?? '-',
    cost: item.cost ?? '-',
  }))
}

function mapRecallStage(stageData) {
  if (!Array.isArray(stageData) || stageData.length === 0) {
    return [{
      riskClass: 'l',
      badge: '대기',
      title: 'API 리콜 결과 없음',
      similarity: '0.00',
      checks: ['진단 완료 후 위해·리콜 source trace를 확인하세요.'],
    }]
  }
  return stageData.map((item) => ({
    riskClass: item.riskClass ?? 'm',
    badge: item.badge ?? '확인',
    title: item.title ?? '제품군 위해·리콜 신호',
    similarity: String(item.similarity ?? '0.00'),
    checks: Array.isArray(item.checks) && item.checks.length ? item.checks : ['담당자 근거 문서 확인 필요'],
  }))
}

function mapFtoSupportStage(stageData) {
  const patents = Array.isArray(stageData?.patents) ? stageData.patents : []
  const supports = Array.isArray(stageData?.supports) ? stageData.supports : []
  return {
    patents: patents.length ? patents.map((item) => ({
      riskClass: item.riskClass ?? 'm',
      badge: item.badge ?? '확인',
      name: item.name ?? 'FTO 준비도',
      sub: item.sub ?? item.description ?? '',
    })) : [{
      riskClass: 'm',
      badge: '대기',
      name: 'API FTO 결과 없음',
      sub: '진단 완료 후 KIPRIS/public trace를 확인하세요.',
    }],
    supports: supports.length ? supports.map((item) => ({
      name: item.name ?? '지원사업 적합도',
      org: item.org ?? item.description ?? '',
      dday: item.dday ?? '확인',
      isGreen: Boolean(item.isGreen),
    })) : [{
      name: 'API 지원사업 결과 없음',
      org: '진단 완료 후 Bizinfo/KOTRA source trace를 확인하세요.',
      dday: '대기',
      isGreen: false,
    }],
  }
}

function mapRoadmapStage(stageData) {
  if (!stageData) {
    return [
      { day: '이번 주', label: '즉시 보완', tasks: normalizeActionItems([], 'NOW') },
      { day: '견적 전', label: '확정 전 필수', tasks: normalizeActionItems([], 'QUOTE') },
      { day: '출하 전', label: '문서 패키지', tasks: normalizeActionItems([], 'SHIP') },
    ]
  }
  const workflowBuckets = [
    { key: 'thisWeek', day: '이번 주', label: '즉시 보완', tag: 'NOW' },
    { key: 'beforeQuote', day: '견적 전', label: '확정 전 필수', tag: 'QUOTE' },
    { key: 'beforeShipment', day: '출하 전', label: '문서 패키지', tag: 'SHIP' },
  ]
  if (workflowBuckets.some((bucket) => Array.isArray(stageData[bucket.key]))) {
    return workflowBuckets.map((bucket) => ({
      day: bucket.day,
      label: bucket.label,
      tasks: normalizeActionItems(stageData[bucket.key], bucket.tag),
    }))
  }
  const legacyBuckets = [
    { key: 'd30', day: '30일', label: '필수 보완', tag: 'D30' },
    { key: 'd60', day: '60일', label: '증빙 확장', tag: 'D60' },
    { key: 'd90', day: '90일', label: '시장 실행', tag: 'D90' },
  ]
  return legacyBuckets.map((bucket) => {
    const count = Number(stageData[bucket.key] ?? 1)
    return {
      day: bucket.day,
      label: bucket.label,
      tasks: Array.from({ length: Math.max(1, Math.min(count, 5)) }, (_, i) => ({
        tag: bucket.tag,
        title: `${bucket.label} 태스크 ${i + 1}`,
        who: '수출 담당자 · 제품 지식 관리자',
        urgent: bucket.key === 'd30' && i === 0,
      })),
    }
  })
}

function normalizeActionItems(items, tag) {
  if (!Array.isArray(items) || items.length === 0) {
    return [{
      tag,
      title: '담당자 확인 항목 없음',
      who: '수출 담당자',
      output: 'source trace 확인',
      reason: '백엔드 stageCards에 업무 항목이 없습니다.',
      urgent: false,
    }]
  }
  return items.map((item, index) => ({
    tag,
    title: item.title ?? `업무 항목 ${index + 1}`,
    who: item.owner ?? item.who ?? '수출 담당자',
    output: item.output ?? '',
    dueDate: item.dueDate ?? '',
    reason: item.reason ?? item.blockedIfNotDone ?? '',
    urgent: tag === 'NOW' || String(item.blockedIfNotDone ?? '').includes('불가'),
  }))
}

function mapReportStage(stageData, diagnosisScore, summary) {
  const scoreMap = typeof diagnosisScore === 'object' && diagnosisScore ? diagnosisScore : {}
  const fallbackBars = [
    { name: 'Market', value: scoreMap.market ?? 0 },
    { name: 'Regulation', value: scoreMap.regulation ?? 0 },
    { name: 'Recall', value: scoreMap.recall ?? 0 },
    { name: 'FTO', value: scoreMap.fto ?? 0 },
    { name: 'Support', value: scoreMap.support ?? 0 },
  ]
  const bars = Array.isArray(stageData?.scoreBars) ? stageData.scoreBars : fallbackBars
  return {
    summary: stageData?.summary ?? summary ?? '',
    scoreBars: bars.map((item) => ({
      name: item.name,
      value: Number(item.value ?? 0),
      amber: Number(item.value ?? 0) < 70,
    })),
  }
}

function mapSourceStage(stageData, evidenceMeta = {}) {
  const entries = !Array.isArray(stageData) || stageData.length === 0
    ? [{
      source: 'Source trace pending',
      count: '0',
      label: '데이터 없음 - source 미수집',
      metricSummary: 'backend stageCards.s10 또는 sources API 응답을 기다립니다.',
      status: 'none',
    }]
    : stageData.map((item) => ({
      source: item.sourceType === 'heuristic' ? '규칙 엔진' : item.provider ?? item.datasetName ?? item.field ?? 'Source',
      count: item.scoreContribution != null ? `${item.scoreContribution > 0 ? '+' : ''}${item.scoreContribution}` : '0',
      label: item.decisionImpact ?? sourceTypeLabel(item.sourceType, item.usedFor ?? item.field),
      metricSummary: item.metricSummary ?? item.reason ?? '',
      status: item.sourceType ?? (item.fallbackUsed ? 'fallback' : 'live'),
    }))

  return {
    pipes: entries,
    evidenceRetrieval: evidenceMeta.evidenceRetrieval ?? null,
    timeline: Array.isArray(evidenceMeta.exportKnowledgeTimeline) ? evidenceMeta.exportKnowledgeTimeline : [],
    guardrails: evidenceMeta.evidenceGuardrails ?? null,
    ledger: Array.isArray(evidenceMeta.scoreContributionLedger) ? evidenceMeta.scoreContributionLedger : [],
    regionalIndustryProfile: evidenceMeta.regionalIndustryProfile ?? null,
  }
}

function sourceTypeLabel(sourceType, usedFor) {
  const suffix = usedFor ? ` · ${usedFor}` : ''
  if (sourceType === 'live') return `live 공공데이터 수집 record${suffix}`
  if (sourceType === 'cache') return `캐시 공공데이터${suffix}`
  if (sourceType === 'fallback') return `수집 실패/fallback${suffix}`
  if (sourceType === 'crawl') return `안전 크롤링 공개 웹${suffix}`
  if (sourceType === 'external_feed') return `수출 뉴스·보고서 feed${suffix}`
  if (sourceType === 'seed') return `seed demo 데이터${suffix}`
  if (sourceType === 'heuristic') return `규칙 엔진${suffix}`
  if (sourceType === 'agent') return `workflow trace${suffix}`
  if (sourceType === 'user_input') return `담당자 입력${suffix}`
  return `출처${suffix}`
}

function normalizeTuple(value, fallbackType, fallbackLabel) {
  return Array.isArray(value) && value.length >= 2 ? value : [fallbackType, fallbackLabel]
}

function gradeClass(score) {
  if (score >= 75) return 'h'
  if (score >= 60) return 'm'
  return 'l'
}

function gradeLabel(score) {
  if (score >= 75) return 'HIGH'
  if (score >= 60) return 'MEDIUM'
  return 'LOW'
}

function normalizeDiagnosisScore(score) {
  if (typeof score === 'number') return { ...EMPTY_SCORE, overall: score }
  if (!score || typeof score !== 'object') return EMPTY_SCORE
  return {
    overall: Number(score.overall ?? 0),
    market: Number(score.market ?? 0),
    regulation: Number(score.regulation ?? 0),
    recall: Number(score.recall ?? 0),
    fto: Number(score.fto ?? 0),
    support: Number(score.support ?? 0),
  }
}

// 진단 근거/출처 조회 — 실패 시 null 반환(상세 응답의 result.evidenceSources로 대체됨)
export async function getDiagnosisSources(id) {
  const res = await apiFetch(`/diagnosis/${id}/sources`)
  return (res.sources ?? []).map((s) => {
    const contribution = s.scoreContribution != null ? ` (${s.scoreContribution > 0 ? '+' : ''}${s.scoreContribution})` : ''
    return `${s.provider ?? s.documentName ?? s.field}${contribution}`
  })
}

export async function getDiagnosisSourceEntries(id) {
  const res = await apiFetch(`/diagnosis/${id}/sources`)
  return res.sources ?? []
}

// 진단 결과 상세 조회
export async function getDiagnosisResult(id) {
  const detail = await apiFetch(`/diagnosis/${id}`)
  let evidenceSources = null
  let sourceEntries = []
  try {
    sourceEntries = await getDiagnosisSourceEntries(id)
    evidenceSources = sourceEntries.map((s) => {
      const contribution = s.scoreContribution != null ? ` (${s.scoreContribution > 0 ? '+' : ''}${s.scoreContribution})` : ''
      return `${s.provider ?? s.documentName ?? s.field}${contribution}`
    })
  } catch {
    evidenceSources = detail.result?.evidenceSources ?? null
  }
  return mapDiagnosisDetail(detail, evidenceSources, sourceEntries)
}

export async function getDiagnosisReport(id) {
  return apiFetch(`/diagnosis/${id}/report`)
}

// 새 진단 실행
export async function runDiagnosis(payload) {
  // productId 유무와 무관하게 백엔드 CreateDiagnosisRequest.InputData는
  // productName·hsCode를 항상 @NotBlank로 요구하므로 항상 함께 보낸다.
  // (기존엔 productId가 있으면 두 값을 빼서 "제품명/HS Code는 필수" 오류로 진단 생성이 실패했음)
  const body = {
    productId: payload.productId ?? null,
    targetCountry: payload.targetMarket ?? payload.targetCountry,
    inputData: {
      productName: payload.productName,
      hsCode: payload.hsCode,
      description: payload.description,
      region: payload.region,
      industry: payload.industry,
      buyerRequest: payload.buyerRequest,
      buyerDeadline: payload.buyerDeadline,
      expectedShipmentDate: payload.expectedShipmentDate,
      requestedCertifications: payload.requestedCertifications,
      currentDocuments: payload.currentDocuments,
      exportExperience: payload.exportExperience,
      budgetLimitKrw: payload.budgetLimitKrw,
    },
  }

  const res = await apiFetch('/diagnosis', { method: 'POST', body: JSON.stringify(body) })
  return { id: res.diagnosisId, status: res.status, message: res.message, ...payload }
}
