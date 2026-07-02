// 진단 서비스
import { apiFetch } from './apiClient.js'

const viteEnv = import.meta.env ?? {}
const ENABLE_MOCK = viteEnv.VITE_ENABLE_MOCK === 'true'
const APP_MODE = viteEnv.VITE_APP_MODE ?? ''
const PRESENTATION_MODE = APP_MODE === 'presentation' || viteEnv.VITE_PRESENTATION_MODE === 'true'

const MOCK_ALLOWED = ENABLE_MOCK && !PRESENTATION_MODE && !viteEnv.PROD

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
  scoreContributionLedger: [],
  normalizedMetrics: null,
  pythonWorker: null,
  reportMarkdown: '',
  reportFastApiUsed: false,
}

const EMPTY_METRICS = [
  { step: '01', label: 'Input', score: 0 },
  { step: '02', label: 'Gate', score: 0 },
  { step: '03', label: 'Sources', score: 0 },
]

async function loadDiagnosisListMock() {
  const mockPath = '/src/mock/diagnosisListData.js'
  const module = await import(/* @vite-ignore */ mockPath)
  return module.DIAGNOSIS_LIST
}

async function loadDiagnosisMockFallback() {
  const mockPath = '/src/mock/diagnosisData.js'
  const module = await import(/* @vite-ignore */ mockPath)
  const sessionKey = ['CURRENT', 'SESSION'].join('_')
  const kpiKey = ['KPI', 'DATA'].join('_')
  return {
    session: module[sessionKey],
    kpiData: module[kpiKey],
    stages: {
      s00: module.S00_OVERVIEW,
      s01: module.S01_INPUT,
      s02: module.S02_CONNECTORS,
      s03: { predictions: module.S03_PREDICTIONS, features: module.S03_FEATURES },
      s04: [],
      s05: [],
      s06: [],
      s07: { patents: [], supports: [] },
      s08: module.S08_EXEC,
      s09: module.S09_SCORE_BARS,
      s10: module.S10_PIPES,
    },
  }
}

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
  try {
    const res = await apiFetch('/diagnosis')
    return res.diagnoses.map(mapDiagnosisSummary)
  } catch (err) {
    if (!MOCK_ALLOWED) throw err
    console.warn('[diagnosisService] getDiagnosisList API 실패, VITE_ENABLE_MOCK=true라 mock으로 대체합니다.', err)
    return loadDiagnosisListMock()
  }
}

// 진단 결과 상세 매핑
export function mapDiagnosisDetail(d, evidenceSources) {
  const diagnosisScore = normalizeDiagnosisScore(d.result?.diagnosisScore)
  const overallScore = typeof diagnosisScore === 'number'
    ? diagnosisScore
    : diagnosisScore?.overall ?? 0
  const stageCards = d.result?.stageCards ?? {}
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
    sourceTrustSummary: d.result?.sourceTrustSummary ?? null,
    scoreContributionLedger: Array.isArray(d.result?.scoreContributionLedger) ? d.result.scoreContributionLedger : [],
    normalizedMetrics: d.result?.normalizedMetrics ?? null,
    pythonWorker: d.result?.pythonWorker ?? null,
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
      }),
    },
  }
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
  }
}

function sourceTypeLabel(sourceType, usedFor) {
  const suffix = usedFor ? ` · ${usedFor}` : ''
  if (sourceType === 'live') return `live 공공데이터 수집 record${suffix}`
  if (sourceType === 'cache') return `캐시 공공데이터${suffix}`
  if (sourceType === 'fallback') return `수집 실패/fallback${suffix}`
  if (sourceType === 'crawl') return `안전 크롤링 공개 웹${suffix}`
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
  try {
    const res = await apiFetch(`/diagnosis/${id}/sources`)
    return (res.sources ?? []).map((s) => {
      const contribution = s.scoreContribution != null ? ` (${s.scoreContribution > 0 ? '+' : ''}${s.scoreContribution})` : ''
      return `${s.provider ?? s.documentName ?? s.field}${contribution}`
    })
  } catch (err) {
    if (!MOCK_ALLOWED) throw err
    console.warn('[diagnosisService] getDiagnosisSources API 실패, VITE_ENABLE_MOCK=true라 상세 응답 출처로 대체합니다.', err)
    return null
  }
}

export async function getDiagnosisSourceEntries(id) {
  const res = await apiFetch(`/diagnosis/${id}/sources`)
  return res.sources ?? []
}

// 진단 결과 상세 조회
export async function getDiagnosisResult(id) {
  try {
    const detail = await apiFetch(`/diagnosis/${id}`)
    let evidenceSources = null
    try {
      evidenceSources = await getDiagnosisSources(id)
    } catch {
      evidenceSources = detail.result?.evidenceSources ?? null
    }
    return mapDiagnosisDetail(detail, evidenceSources)
  } catch (err) {
    if (!MOCK_ALLOWED) throw err
    console.warn('[diagnosisService] getDiagnosisResult API 실패, VITE_ENABLE_MOCK=true라 mock으로 대체합니다.', err)
    return loadDiagnosisMockFallback()
  }
}

export async function getDiagnosisReport(id) {
  return apiFetch(`/diagnosis/${id}/report`)
}

// 새 진단 실행
export async function runDiagnosis(payload) {
  const body = {
    productId: payload.productId ?? null,
    targetCountry: payload.targetMarket ?? payload.targetCountry,
  }
  if (!body.productId) {
    body.inputData = {
      productName: payload.productName,
      hsCode: payload.hsCode,
      description: payload.description,
      buyerRequest: payload.buyerRequest,
      buyerDeadline: payload.buyerDeadline,
      expectedShipmentDate: payload.expectedShipmentDate,
      requestedCertifications: payload.requestedCertifications,
      currentDocuments: payload.currentDocuments,
      exportExperience: payload.exportExperience,
      budgetLimitKrw: payload.budgetLimitKrw,
    }
  } else {
    body.inputData = {
      buyerRequest: payload.buyerRequest,
      buyerDeadline: payload.buyerDeadline,
      expectedShipmentDate: payload.expectedShipmentDate,
      requestedCertifications: payload.requestedCertifications,
      currentDocuments: payload.currentDocuments,
      exportExperience: payload.exportExperience,
      budgetLimitKrw: payload.budgetLimitKrw,
    }
  }

  const res = await apiFetch('/diagnosis', { method: 'POST', body: JSON.stringify(body) })
  return { id: res.diagnosisId, status: res.status, message: res.message, ...payload }
}
