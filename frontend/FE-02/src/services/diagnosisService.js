// 진단 서비스
import { apiFetch } from './apiClient.js'
import { DIAGNOSIS_LIST } from '../mock/diagnosisListData.js'
import {
  CURRENT_SESSION, KPI_DATA,
  S00_OVERVIEW, S01_INPUT, S02_CONNECTORS,
  S03_PREDICTIONS, S03_FEATURES, S04_MARKETS,
  S05_REGULATIONS, S06_RECALLS, S07_PATENTS, S07_SUPPORTS,
  S08_EXEC, S09_SCORE_BARS, S10_PIPES,
} from '../mock/diagnosisData.js'

const ENABLE_MOCK = import.meta.env.VITE_ENABLE_MOCK === 'true'

const MOCK_STAGES = {
  s00: S00_OVERVIEW,
  s01: S01_INPUT,
  s02: S02_CONNECTORS,
  s03: { predictions: S03_PREDICTIONS, features: S03_FEATURES },
  s04: S04_MARKETS,
  s05: S05_REGULATIONS,
  s06: S06_RECALLS,
  s07: { patents: S07_PATENTS, supports: S07_SUPPORTS },
  s08: S08_EXEC,
  s09: S09_SCORE_BARS,
  s10: S10_PIPES,
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
    diagnosisScore: null,
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
    if (!ENABLE_MOCK) throw err
    console.warn('[diagnosisService] getDiagnosisList API 실패, VITE_ENABLE_MOCK=true라 mock으로 대체합니다.', err)
    return DIAGNOSIS_LIST
  }
}

// 진단 결과 상세 매핑
function mapDiagnosisDetail(d, evidenceSources) {
  const diagnosisScore = d.result?.diagnosisScore ?? CURRENT_SESSION.diagnosisScore
  const overallScore = typeof diagnosisScore === 'number'
    ? diagnosisScore
    : diagnosisScore?.overall ?? CURRENT_SESSION.diagnosisScore
  const stageCards = d.result?.stageCards ?? {}
  const session = {
    ...CURRENT_SESSION,
    id: d.id,
    status: d.status,
    productId: d.productId,
    companyId: d.companyId,
    knowledgeSnapshotId: d.knowledgeSnapshotId,
    productName: d.productName,
    targetMarket: d.targetCountry,
    trustGrade: d.knowledgeSnapshot?.trustGrade ?? CURRENT_SESSION.trustGrade,
    missingFields: d.knowledgeSnapshot?.missingFields ?? CURRENT_SESSION.missingFields,
    evidenceSources: evidenceSources ?? d.result?.evidenceSources ?? CURRENT_SESSION.evidenceSources,
    updatedAt: d.createdAt,
    diagnosisScore,
    riskItems: d.result?.riskItems ?? CURRENT_SESSION.riskItems,
    actionRoadmap: d.result?.actionRoadmap ?? CURRENT_SESSION.actionRoadmap,
    agentTrace: d.result?.agentTrace ?? null,
    reportMarkdown: d.result?.reportMarkdown ?? '',
    reportFastApiUsed: Boolean(d.result?.reportFastApiUsed),
  }

  return {
    session,
    kpiData: KPI_DATA.map((kpi) =>
      kpi.step === '02' ? { ...kpi, score: overallScore } : kpi
    ),
    stages: {
      ...MOCK_STAGES,
      s00: mapOverviewStage(stageCards.s00, overallScore),
      s02: mapConnectorStage(stageCards.s02),
      s03: mapPredictionStage(stageCards.s03),
      s08: mapRoadmapStage(stageCards.s08),
      s09: mapReportStage(stageCards.s09, diagnosisScore, d.result?.summary),
      s10: mapSourceStage(stageCards.s10),
    },
  }
}

function mapOverviewStage(stageData, overallScore) {
  if (!Array.isArray(stageData) || stageData.length === 0) {
    return S00_OVERVIEW.map((item, i) => i === 0 ? { ...item, value: String(overallScore) } : item)
  }
  return stageData.map((item) => ({
    title: item.label ?? item.title ?? '진단 항목',
    value: String(item.value ?? ''),
    unit: item.unit ?? '',
    desc: item.note ?? item.desc ?? '',
  }))
}

function mapConnectorStage(stageData) {
  if (!Array.isArray(stageData) || stageData.length === 0) return S02_CONNECTORS
  return stageData.map((item, index) => ({
    no: String(index + 1).padStart(2, '0'),
    nm: item.name ?? item.provider ?? '데이터 출처',
    meta: `${item.provider ?? 'provider'} · ${item.status ?? 'connected'} · ${item.contribution ?? 0}점`,
  }))
}

function mapPredictionStage(stageData) {
  if (!stageData?.predictions) {
    return { predictions: S03_PREDICTIONS, features: S03_FEATURES }
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
  return { predictions, features: features.length ? features : S03_FEATURES }
}

function mapRoadmapStage(stageData) {
  if (!stageData) return S08_EXEC
  const buckets = [
    { key: 'd30', day: '30', label: '필수 보완', tag: 'D30' },
    { key: 'd60', day: '60', label: '증빙 확장', tag: 'D60' },
    { key: 'd90', day: '90', label: '시장 실행', tag: 'D90' },
  ]
  return buckets.map((bucket) => {
    const count = Number(stageData[bucket.key] ?? 2)
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

function mapSourceStage(stageData) {
  if (!Array.isArray(stageData) || stageData.length === 0) return S10_PIPES
  return stageData.map((item) => ({
    source: item.provider ?? item.datasetName ?? item.field ?? 'Source',
    count: item.scoreContribution != null ? `${item.scoreContribution > 0 ? '+' : ''}${item.scoreContribution}` : '0',
    label: item.fallbackUsed ? 'Fallback' : item.usedFor ?? 'Connected',
  }))
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
    if (!ENABLE_MOCK) throw err
    console.warn('[diagnosisService] getDiagnosisSources API 실패, VITE_ENABLE_MOCK=true라 상세 응답 출처로 대체합니다.', err)
    return null
  }
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
    if (!ENABLE_MOCK) throw err
    console.warn('[diagnosisService] getDiagnosisResult API 실패, VITE_ENABLE_MOCK=true라 mock으로 대체합니다.', err)
    return { session: CURRENT_SESSION, kpiData: KPI_DATA, stages: MOCK_STAGES }
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
    }
  }

  const res = await apiFetch('/diagnosis', { method: 'POST', body: JSON.stringify(body) })
  return { id: res.diagnosisId, status: res.status, message: res.message, ...payload }
}
