// BE TrustGrade(high/medium/low) → 화면 표시용 등급(A/B/C) 변환 adapter
const GRADE_LABEL = { high: 'A', medium: 'B', low: 'C' }
const GRADE_CLASS = { high: 'ok', medium: 'cond', low: 'warn' }

export function toGradeLabel(trustGrade) {
  return GRADE_LABEL[trustGrade] ?? '-'
}

export function toGradeClass(trustGrade) {
  return GRADE_CLASS[trustGrade] ?? 'warn'
}
