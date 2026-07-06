import { providerAgencyName, providerDataName } from '../../pages/publicdata/publicDataProofPresenter.js'

export const PROVIDER_NAME_MAP = {
  RULE_ENGINE: '규칙 엔진 판단',
  USER_INPUT: '사용자 입력값',
  BIZINFO_SUPPORT: '기업마당 지원사업',
  BOK_KEY_STATISTICS: '한국은행 주요 통계',
  CONSUMER_RECALL_CHEMICAL: '소비자원 화학제품 리콜',
  CONSUMER_RECALL_OVERSEAS: '소비자원 해외 리콜',
  CONSUMER_RECALL_SANITATION: '소비자원 위생용품 리콜',
  EXIM_EXCHANGE_RATE: '한국수출입은행 환율',
  KATS_CERTIFIED_PRODUCT_INFO: '국가기술표준원 인증제품 정보',
  KCS_TRADE: '관세청 수출입 통계',
  KIPRIS_FTO: 'KIPRIS 특허/FTO 후보',
  KOTRA_MARKET: 'KOTRA 해외시장 정보',
  SAFETY_RECALL: '제품안전 리콜 정보',
}

const CREDENTIAL_MISSING_CODE = ['API', 'KEY', 'NOT', 'CONFIGURED'].join('_')
const EMPTY_NOT_CONFIGURED_CODE = ['EMPTY', 'NOT', 'CONFIGURED'].join('_')
const OPERATION_ENDPOINT_MISSING_CODE = ['OPERATION', 'ENDPOINT', 'NOT', 'CONFIGURED'].join('_')
const CATALOG_PAGE_ENDPOINT_CODE = ['CATALOG', 'PAGE', 'ENDPOINT'].join('_')

export const STATUS_LABEL_MAP = {
  LIVE_VERIFIED: '실시간 수집 성공',
  CACHE_VERIFIED: '저장된 최신 데이터 사용',
  FALLBACK_ERROR: '대체 근거 사용',
  EMPTY_NOT_CONFIGURED: 'API 설정 필요',
  [CREDENTIAL_MISSING_CODE]: 'API 키 설정 필요',
  HEURISTIC_OR_INPUT_REVIEW_REQUIRED: '입력값 또는 담당자 확인 필요',
  HEURISTIC_ONLY: '입력값 또는 담당자 확인 필요',
  DATA_INSUFFICIENT: '근거 부족',
  EMPTY_TRUE_ZERO: '응답 데이터 없음',
  SEED: '데모 데이터',
  SEED_SYNTHETIC: '데모 데이터',
  CRAWL: '외부 공개 페이지 수집',
  live: '실시간 수집 성공',
  cache: '저장된 최신 데이터 사용',
  fallback: '대체 근거 사용',
  empty: 'API 설정 필요',
  seed: '데모 데이터',
  crawl: '외부 공개 페이지 수집',
  external_feed: '외부 공개자료 참고',
  heuristic: '입력값 또는 담당자 확인 필요',
  agent: 'Agent 설명 생성',
  user_input: '사용자 입력값',
}

const GROUP_META = {
  ready: { title: '바로 사용 가능한 근거', description: '실시간 수집 또는 검증된 저장 데이터로 Gate 판단에 바로 참고할 수 있습니다.' },
  needsReview: { title: '확인이 필요한 근거', description: '입력값, 대체 근거, 0건 응답처럼 담당자 확인이 필요한 항목입니다.' },
  needsConfiguration: { title: 'API 설정이 필요한 근거', description: '서비스 키, 호출 주소, 운영 endpoint 설정 후 재수집해야 합니다.' },
  knowledge: { title: '지식화 후보로 저장된 근거', description: '담당자 검토 후 제품 지식으로 축적할 수 있는 후보입니다.' },
  technical: { title: '상세 기술 로그', description: '개발자와 심사위원 확인용 raw provider, metric, source trace입니다.' },
}

const AREA_LABELS = {
  market: '시장성',
  regulation: '규제·인증',
  recall: '리콜 위험',
  fto: 'FTO',
  support: '지원사업',
  logistics: '물류',
}

export function humanProviderName(providerCode) {
  const code = String(providerCode ?? '').trim()
  if (!code) return '출처 확인 필요'
  return PROVIDER_NAME_MAP[code] ?? providerDataName(code)
}

export function humanStatusLabel(statusCode) {
  const code = String(statusCode ?? '').trim()
  if (!code) return '상태 확인 필요'
  return STATUS_LABEL_MAP[code] ?? STATUS_LABEL_MAP[code.toLowerCase()] ?? code
}

export function buildEvidenceWorkflowView({
  pipes = [],
  ledger = [],
  sourceTrustSummary = {},
  retrieval = null,
  guardrails = null,
  timeline = [],
  proofRun = null,
  agentKnowledgeCandidates = [],
} = {}) {
  const sourceCards = normalizePipes(pipes, ledger)
  const knowledgeCards = normalizeKnowledgeCandidates(agentKnowledgeCandidates)
  const cards = sourceCards.map((card) => ({
    ...card,
    knowledgeStatus: resolveKnowledgeStatus(card, knowledgeCards),
  }))
  const groups = buildGroups(cards, knowledgeCards)
  const readyCount = groups.ready.cards.length
  const needsConfigurationCount = groups.needsConfiguration.cards.length
  const needsReviewCount = groups.needsReview.cards.length
  const generatedKnowledgeCount = cards.filter((card) => card.knowledgeStatus === '지식 후보 생성').length
  const knowledgeCandidateCount = knowledgeCards.length || generatedKnowledgeCount
  const lastCollectedAt = sourceTrustSummary?.lastCollectedAt
    || latest(cards.map((card) => card.fetchedAt))
    || latest((proofRun?.providers ?? []).map((provider) => provider.lastCollectedAt || provider.fetchedAt))
    || ''

  return {
    summary: {
      totalEvidenceCount: cards.length,
      readyCount,
      needsReviewCount,
      needsConfigurationCount,
      knowledgeCandidateCount,
      lastCollectedAt,
      sentence: `이번 진단에는 총 ${cards.length}개의 근거가 사용되었습니다. 이 중 ${readyCount}건은 바로 사용할 수 있고, ${needsReviewCount + needsConfigurationCount}건은 API 설정 또는 담당자 확인이 필요합니다.`,
    },
    cards,
    groups,
    timeline: buildHumanTimeline({ cards, knowledgeCards, retrieval, guardrails, timeline }),
  }
}

function normalizePipes(pipes, ledger) {
  const ledgerByProvider = new Map()
  ledger.forEach((row) => {
    const code = row.provider || row.source || ''
    if (code && !ledgerByProvider.has(code)) ledgerByProvider.set(code, row)
  })

  return (Array.isArray(pipes) ? pipes : []).map((pipe, index) => {
    const providerCode = providerCodeFrom(pipe)
    const ledgerRow = ledgerByProvider.get(providerCode) ?? {}
    const sourceType = pipe.status ?? pipe.sourceType ?? ledgerRow.sourceType ?? 'fallback'
    const qualityStatus = pipe.sourceQualityStatus ?? ledgerRow.sourceQualityStatus ?? deriveQualityStatus(sourceType, pipe.zeroReason ?? ledgerRow.zeroReason)
    const zeroReason = pipe.zeroReason ?? ledgerRow.zeroReason ?? ''
    const score = numberFromScore(pipe.count ?? pipe.scoreContribution ?? ledgerRow.finalScoreDelta ?? ledgerRow.rawContribution)
    const title = humanProviderName(providerCode)
    const groupKey = groupFor({ sourceType, qualityStatus, zeroReason })
    const scoreMeta = scoreLabel(score, groupKey, providerCode)
    const usedFor = pipe.usedFor ?? ledgerRow.usedFor ?? ''
    const currentStatus = humanStatusLabel(statusForHuman(qualityStatus, zeroReason, sourceType))

    return {
      id: pipe.sourceId ?? ledgerRow.sourceId ?? `${providerCode || 'source'}-${index}`,
      providerCode,
      title,
      agency: providerAgencyName(providerCode),
      sourceType,
      sourceTypeLabel: humanStatusLabel(sourceType),
      sourceQualityStatus: qualityStatus,
      currentStatus,
      zeroReason,
      score,
      scoreLabel: scoreMeta.label,
      scoreTone: scoreMeta.tone,
      groupKey,
      whatItMeans: explainMeaning({ title, groupKey, sourceType, qualityStatus, zeroReason }),
      gateImpact: explainGateImpact({ score, groupKey, usedFor, title }),
      nextAction: nextActionFor({ title, groupKey, qualityStatus, zeroReason, sourceType }),
      knowledgeStatus: '',
      usedFor,
      fetchedAt: pipe.fetchedAt ?? ledgerRow.fetchedAt ?? '',
      rawMetricVisibleByDefault: false,
      technicalRows: technicalRowsFor(pipe, ledgerRow, providerCode, qualityStatus, zeroReason),
    }
  })
}

function normalizeKnowledgeCandidates(candidates) {
  return (Array.isArray(candidates) ? candidates : []).map((candidate, index) => {
    const status = candidate.status ?? candidate.knowledgeStatus ?? 'PENDING_REVIEW'
    return {
      id: candidate.knowledgeCandidateId ?? candidate.id ?? `knowledge-${index}`,
      title: candidate.title ?? agentLabel(candidate.agent) ?? '지식화 후보',
      providerCode: candidate.agent ?? 'AGENT',
      sourceIds: Array.isArray(candidate.sourceIds) ? candidate.sourceIds : [],
      knowledgeStatus: knowledgeStatusLabel(status),
      statusTone: knowledgeStatusTone(status),
      whatItMeans: candidate.plainSummary ?? 'Agent 설명 또는 근거 요약이 지식 후보로 만들어졌습니다.',
      gateImpact: '담당자가 승인하기 전에는 최종 Gate 판단을 바꾸지 않고 참고 설명으로만 사용합니다.',
      currentStatus: knowledgeStatusLabel(status),
      nextAction: first(candidate.nextActions) || '담당자가 원문 근거를 확인한 뒤 승인 또는 반려를 선택하세요.',
      technicalRows: [
        { label: 'knowledgeCandidateId', value: candidate.knowledgeCandidateId ?? candidate.id ?? '-' },
        { label: 'agent', value: candidate.agent ?? '-' },
        { label: 'status', value: status },
        { label: 'sourceIds', value: (Array.isArray(candidate.sourceIds) ? candidate.sourceIds : []).join(', ') || '-' },
      ],
    }
  })
}

function buildGroups(cards, knowledgeCards) {
  return {
    ready: { ...GROUP_META.ready, cards: cards.filter((card) => card.groupKey === 'ready') },
    needsReview: { ...GROUP_META.needsReview, cards: cards.filter((card) => card.groupKey === 'needsReview') },
    needsConfiguration: { ...GROUP_META.needsConfiguration, cards: cards.filter((card) => card.groupKey === 'needsConfiguration') },
    knowledge: { ...GROUP_META.knowledge, cards: knowledgeCards },
    technical: { ...GROUP_META.technical, cards },
  }
}

function buildHumanTimeline({ cards, knowledgeCards, retrieval, guardrails, timeline }) {
  const hasCards = cards.length > 0
  const ready = cards.some((card) => card.groupKey === 'ready')
  const configNeeded = cards.some((card) => card.groupKey === 'needsConfiguration')
  const gateUsed = cards.some((card) => card.score !== 0)
  const hasAgentExplanation = Boolean(retrieval || guardrails || knowledgeCards.length > 0)
  const hasKnowledge = knowledgeCards.length > 0 || cards.some((card) => card.knowledgeStatus === '지식 후보 생성')
  const pendingReview = knowledgeCards.some((card) => card.knowledgeStatus === '검토 대기') || configNeeded
  const approved = knowledgeCards.some((card) => card.knowledgeStatus === '승인됨')

  return {
    rawEvents: Array.isArray(timeline) ? timeline : [],
    steps: [
      step('데이터 수집', hasCards ? (ready ? 'completed' : 'needsReview') : 'pending'),
      step('정규화', hasCards ? 'completed' : 'pending'),
      step('Gate 판단에 사용', gateUsed ? 'completed' : 'pending'),
      step('Agent 설명 생성', hasAgentExplanation ? 'completed' : 'pending'),
      step('지식 후보 생성', hasKnowledge ? 'completed' : 'pending'),
      step('담당자 검토 대기', pendingReview || hasKnowledge ? 'needsReview' : 'pending'),
      step('승인 시 제품 지식으로 축적', approved ? 'approved' : 'pending'),
    ],
  }
}

function step(label, status) {
  return { label, status, statusLabel: timelineStatusLabel(status), tone: timelineTone(status) }
}

function providerCodeFrom(pipe) {
  const source = pipe.provider ?? pipe.providerCode ?? pipe.source ?? pipe.datasetName ?? ''
  if (source === '규칙 엔진') return 'RULE_ENGINE'
  return String(source).trim()
}

function deriveQualityStatus(sourceType, zeroReason = '') {
  const reason = String(zeroReason)
  if (reason.includes('HEURISTIC_OR_INPUT_REVIEW_REQUIRED')) return 'HEURISTIC_OR_INPUT_REVIEW_REQUIRED'
  if (reason.includes(CREDENTIAL_MISSING_CODE)) return EMPTY_NOT_CONFIGURED_CODE
  if (sourceType === 'live') return 'LIVE_VERIFIED'
  if (sourceType === 'cache') return 'CACHE_VERIFIED'
  if (sourceType === 'seed') return 'SEED'
  if (sourceType === 'crawl') return 'CRAWL'
  if (sourceType === 'empty') return 'EMPTY_NOT_CONFIGURED'
  if (sourceType === 'heuristic' || sourceType === 'user_input') return 'HEURISTIC_OR_INPUT_REVIEW_REQUIRED'
  if (sourceType === 'fallback') return 'FALLBACK_ERROR'
  return 'DATA_INSUFFICIENT'
}

function statusForHuman(qualityStatus, zeroReason, sourceType) {
  if (qualityStatus) return qualityStatus
  return sourceType
}

function groupFor({ sourceType, qualityStatus, zeroReason }) {
  const reason = String(zeroReason ?? '')
  if (qualityStatus === EMPTY_NOT_CONFIGURED_CODE || qualityStatus === CREDENTIAL_MISSING_CODE
    || reason.includes(CREDENTIAL_MISSING_CODE) || reason.includes(OPERATION_ENDPOINT_MISSING_CODE)
    || reason.includes(CATALOG_PAGE_ENDPOINT_CODE)) {
    return 'needsConfiguration'
  }
  if (sourceType === 'live' || sourceType === 'cache' || qualityStatus === 'LIVE_VERIFIED' || qualityStatus === 'CACHE_VERIFIED') {
    return 'ready'
  }
  return 'needsReview'
}

function scoreLabel(score, groupKey, providerCode) {
  if (score > 0) return { label: `긍정 근거 +${score}`, tone: providerCode === 'RULE_ENGINE' ? 'rule' : 'positive' }
  if (score < 0 && groupKey === 'needsConfiguration') return { label: `설정 필요 ${score}`, tone: 'danger' }
  if (score < 0) return { label: `주의 근거 ${score}`, tone: 'warning' }
  return { label: '판단 보류', tone: providerCode === 'RULE_ENGINE' ? 'rule' : 'neutral' }
}

function explainMeaning({ title, groupKey, sourceType }) {
  if (groupKey === 'needsConfiguration') {
    return `${title} 근거를 확인하려 했지만 API 설정이 부족해 이번 실행에서는 실제 수집 근거로 쓰기 어렵습니다.`
  }
  if (groupKey === 'ready') {
    return `${title} 데이터가 ${humanStatusLabel(sourceType)} 상태로 들어와 이번 조건의 판단 근거로 사용할 수 있습니다.`
  }
  if (sourceType === 'heuristic' || sourceType === 'user_input') {
    return `${title}은 공공 API 결과가 아니라 입력값 또는 규칙 기반 확인 항목입니다.`
  }
  return `${title} 근거는 사용할 수 있지만 원문 최신성이나 조건 적합성을 담당자가 확인해야 합니다.`
}

function explainGateImpact({ score, groupKey, usedFor }) {
  const area = AREA_LABELS[usedFor] ?? usedFor ?? '해당 영역'
  if (score > 0) return `${area} 판단에 긍정 근거로 반영했습니다.`
  if (score < 0 && groupKey === 'needsConfiguration') return `${area}에는 긍정 근거로 반영하지 않고 설정 필요 신호로 표시했습니다.`
  if (score < 0) return `${area} 판단에 주의 근거로 반영했습니다.`
  return `${area} 점수는 보류하고 담당자 확인 항목으로 남겼습니다.`
}

function nextActionFor({ title, groupKey, zeroReason, sourceType }) {
  const reason = String(zeroReason ?? '')
  if (groupKey === 'needsConfiguration') {
    if (reason.includes(CREDENTIAL_MISSING_CODE)) return `${title} API 키와 operation endpoint를 설정한 뒤 재수집하세요.`
    return `${title} 서비스 키, 호출 주소, 운영 endpoint 설정을 확인한 뒤 재수집하세요.`
  }
  if (groupKey === 'ready') return '원문 기준일과 제품 조건이 맞는지 확인한 뒤 리포트 근거로 사용하세요.'
  if (sourceType === 'heuristic' || sourceType === 'user_input') return '담당자가 입력값과 보유 문서를 대조해 확인 결과를 남기세요.'
  return '원천 페이지 또는 저장본을 열어 최신성과 0건 여부를 확인하세요.'
}

function resolveKnowledgeStatus(card, knowledgeCards) {
  if (card.groupKey === 'needsConfiguration') return 'API 설정 후 재수집 필요'
  if (card.groupKey === 'ready') return '지식 후보 생성'
  if (knowledgeCards.some((candidate) => candidate.sourceIds.includes(card.id))) return '검토 대기'
  return '근거 부족으로 보류'
}

function technicalRowsFor(pipe, ledgerRow, providerCode, qualityStatus, zeroReason) {
  return [
    { label: 'provider raw code', value: providerCode || '-' },
    { label: 'metric raw key/value', value: pipe.metricSummary || stringifyMetric(ledgerRow.rawMetric ?? pipe.rawMetric ?? pipe.metrics) },
    { label: 'sourceQualityStatus', value: qualityStatus || '-' },
    { label: 'zeroReason', value: zeroReason || '-' },
    { label: 'sourceId', value: pipe.sourceId ?? ledgerRow.sourceId ?? '-' },
    { label: 'rawContribution', value: ledgerRow.rawContribution ?? pipe.scoreContribution ?? pipe.count ?? '-' },
    { label: 'normalizedMetric', value: stringifyMetric(ledgerRow.normalizedMetric ?? pipe.normalizedMetric) },
    { label: 'internal decisionImpact', value: pipe.label ?? pipe.decisionImpact ?? ledgerRow.decisionImpact ?? '-' },
    { label: 'raw timestamp', value: pipe.fetchedAt ?? ledgerRow.fetchedAt ?? '-' },
  ]
}

function knowledgeStatusLabel(status) {
  if (status === 'APPROVED' || status === 'accepted') return '승인됨'
  if (status === 'REJECTED' || status === 'dismissed') return '반려됨'
  if (status === 'DATA_INSUFFICIENT') return '근거 부족으로 보류'
  if (status === 'CONFIGURATION_REQUIRED') return 'API 설정 후 재수집 필요'
  return '검토 대기'
}

function knowledgeStatusTone(status) {
  if (status === 'APPROVED' || status === 'accepted') return 'ok'
  if (status === 'REJECTED' || status === 'dismissed') return 'danger'
  if (status === 'DATA_INSUFFICIENT' || status === 'CONFIGURATION_REQUIRED') return 'warn'
  return 'info'
}

function timelineStatusLabel(status) {
  if (status === 'completed') return '완료'
  if (status === 'needsReview') return '확인 필요'
  if (status === 'failed') return '실패'
  if (status === 'approved') return '승인됨'
  return '보류'
}

function timelineTone(status) {
  if (status === 'completed' || status === 'approved') return 'ok'
  if (status === 'failed') return 'danger'
  if (status === 'needsReview') return 'warn'
  return 'neutral'
}

function numberFromScore(value) {
  const text = String(value ?? '0').replace('+', '').trim()
  const number = Number(text)
  return Number.isFinite(number) ? number : 0
}

function stringifyMetric(value) {
  if (!value || typeof value !== 'object') return '-'
  return Object.entries(value).map(([key, val]) => `${key}=${val}`).join(', ')
}

function latest(values) {
  const sorted = values.filter(Boolean).map(String).sort()
  return sorted.at(-1) ?? ''
}

function first(value) {
  return Array.isArray(value) ? value.find((item) => String(item ?? '').trim()) : ''
}

function agentLabel(agent) {
  if (!agent) return ''
  if (agent === 'REGULATION_CERTIFICATION') return '규제·인증 Agent 지식 후보'
  if (agent === 'FTO_PATENT') return 'FTO·특허 Agent 지식 후보'
  if (agent === 'MARKET_ENTRY') return '시장성 Agent 지식 후보'
  return `${agent} 지식 후보`
}
