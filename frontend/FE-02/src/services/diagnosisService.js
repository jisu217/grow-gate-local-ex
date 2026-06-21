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
// 백엔드 목록 응답에 없는 hsCode/diagnosisScore/summary는 null 처리.
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
    hsCode: '',
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
    console.warn('[diagnosisService] getDiagnosisList API 실패, mock으로 대체합니다.', err)
    return DIAGNOSIS_LIST
  }
}

// 진단 결과 상세 매핑
// 백엔드 제공 범위: 세션(점수·리스크·로드맵·근거자료). S02~S10(커넥터·시장·규제·리콜·
// 특허·실행보드·파이프라인)은 백엔드 데이터 모델 부재로 mock 그대로 사용.
// → S02~S10 실데이터화는 별도 백엔드 작업(공공데이터 연동·예측 모델)으로 분리.
function mapDiagnosisDetail(d) {
  const session = {
    ...CURRENT_SESSION,
    productId: d.productId,
    companyId: d.companyId,
    knowledgeSnapshotId: d.knowledgeSnapshotId,
    productName: d.productName,
    targetMarket: d.targetCountry,
    trustGrade: d.knowledgeSnapshot?.trustGrade ?? CURRENT_SESSION.trustGrade,
    missingFields: d.knowledgeSnapshot?.missingFields ?? CURRENT_SESSION.missingFields,
    evidenceSources: d.result?.evidenceSources ?? CURRENT_SESSION.evidenceSources,
    updatedAt: d.createdAt,
    diagnosisScore: d.result?.diagnosisScore ?? CURRENT_SESSION.diagnosisScore,
    riskItems: d.result?.riskItems ?? CURRENT_SESSION.riskItems,
    actionRoadmap: d.result?.actionRoadmap ?? CURRENT_SESSION.actionRoadmap,
  }

  return {
    session,
    kpiData: KPI_DATA.map((kpi) =>
      kpi.step === '02' ? { ...kpi, score: session.diagnosisScore } : kpi
    ),
    stages: {
      ...MOCK_STAGES,
      s00: S00_OVERVIEW.map((item, i) =>
        i === 0 ? { ...item, value: String(session.diagnosisScore) } : item
      ),
    },
  }
}

// 진단 결과 상세 조회
export async function getDiagnosisResult(id) {
  try {
    const detail = await apiFetch(`/diagnosis/${id}`)
    return mapDiagnosisDetail(detail)
  } catch (err) {
    console.warn('[diagnosisService] getDiagnosisResult API 실패, mock으로 대체합니다.', err)
    return { session: CURRENT_SESSION, kpiData: KPI_DATA, stages: MOCK_STAGES }
  }
}

// 새 진단 실행
export async function runDiagnosis(payload) {
  try {
    const body = {
      productId: payload.productId ?? null,
      targetCountry: payload.targetMarket ?? payload.targetCountry,
      inputData: {
        productName: payload.productName,
        hsCode: payload.hsCode,
      },
    }
    const res = await apiFetch('/diagnosis', { method: 'POST', body: JSON.stringify(body) })
    return { id: res.diagnosisId, status: res.status, message: res.message, ...payload }
  } catch (err) {
    console.warn('[diagnosisService] runDiagnosis API 실패, mock으로 대체합니다.', err)
    return { id: `diag-${Date.now()}`, status: 'pending', ...payload }
  }
}
