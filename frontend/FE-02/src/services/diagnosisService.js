// 진단 서비스
import { DIAGNOSIS_LIST } from '../mock/diagnosisListData.js'
import {
  CURRENT_SESSION, KPI_DATA,
  S00_OVERVIEW, S01_INPUT, S02_CONNECTORS,
  S03_PREDICTIONS, S03_FEATURES, S04_MARKETS,
  S05_REGULATIONS, S06_RECALLS, S07_PATENTS, S07_SUPPORTS,
  S08_EXEC, S09_SCORE_BARS, S10_PIPES,
} from '../mock/diagnosisData.js'

// 진단 이력 목록 조회
export async function getDiagnosisList() {
  return DIAGNOSIS_LIST
}

// 진단 결과 상세 조회
export async function getDiagnosisResult(id) {
  return {
    session: CURRENT_SESSION,
    kpiData: KPI_DATA,
    stages: {
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
    },
  }
}

// 새 진단 실행
export async function runDiagnosis(payload) {
  return { id: `diag-${Date.now()}`, status: 'pending', ...payload }
}
