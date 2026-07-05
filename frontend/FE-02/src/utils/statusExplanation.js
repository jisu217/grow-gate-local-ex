const NEGATIVE_STATUS_PATTERNS = [
  ['not configured', 'not configured'],
  ['insufficient evidence', 'insufficient evidence'],
  ['누락위험', '누락위험'],
  ['고위험', '고위험'],
  ['중위험', '중위험'],
  ['긴급', '긴급'],
  ['보강', '보강'],
  ['fallback', 'fallback'],
  ['미연결', '미연결'],
  ['담당자 확인 필요', '담당자 확인 필요'],
  ['검토 필요', '검토 필요'],
  ['출처확인', '출처확인'],
  ['검토', '검토'],
  ['확인', '확인'],
  ['높음', '높음'],
  ['대기', '대기'],
]

const POSITIVE_STATUS_PATTERNS = ['정상', '완료', '연결', 'live', 'cache', 'ok']

const TEMPLATES = {
  긴급: {
    severity: 'high',
    plainSummary: '수출 전에 먼저 확인해야 할 필수 항목이 남아 있습니다.',
    reason: '대상국 규제나 인증 근거가 부족하거나, 견적 전에 확인해야 할 조건이 있습니다.',
    nextAction: ['인증/규제 담당자 검토 후 견적을 확정하세요.', '필요한 인증서와 시험성적서 보유 여부를 먼저 확인하세요.'],
    confidence: 'medium',
  },
  보강: {
    severity: 'medium',
    plainSummary: '진단은 가능하지만 근거 자료를 더 채우면 판단이 좋아집니다.',
    reason: '지원사업, 인증, 제품 설명 자료 중 일부가 부족합니다.',
    nextAction: ['제품 설명서, 인증 보유 여부, 바이어 요구사항을 추가하세요.', 'source trace가 부족한 항목은 담당자가 원문을 확인하세요.'],
    confidence: 'medium',
  },
  고위험: {
    severity: 'high',
    plainSummary: '그대로 진행하면 일정이나 비용 문제가 커질 수 있습니다.',
    reason: 'FTO, 리콜, 규제 근거 중 중요한 항목이 부족하거나 위험 신호가 있습니다.',
    nextAction: ['전문가 검토나 추가 검색을 먼저 진행하세요.', '견적 확정 전 확인 결과를 문서로 남기세요.'],
    confidence: 'medium',
  },
  중위험: {
    severity: 'medium',
    plainSummary: '바로 중단할 정도는 아니지만 확인할 항목이 있습니다.',
    reason: '일부 유사 리콜이나 규제 확인 항목이 발견되었습니다.',
    nextAction: ['체크리스트를 따라 보완 후 다음 단계로 진행하세요.', '제품안전/리콜 근거를 원문으로 확인하세요.'],
    confidence: 'medium',
  },
  누락위험: {
    severity: 'high',
    plainSummary: '판단에 필요한 자료가 빠져 있어 위험하게 표시되었습니다.',
    reason: '필수 근거 데이터가 live로 확인되지 않았거나 fallback 상태입니다.',
    nextAction: ['관련 문서나 공공데이터 연결 상태를 확인하세요.', '견적 전에 누락된 증빙을 지식화하세요.'],
    confidence: 'low',
  },
  fallback: {
    severity: 'medium',
    plainSummary: '실시간 데이터 대신 예비 데이터로 판단했습니다.',
    reason: '공공 API 연결 또는 응답이 부족해 cache/seed/fallback 데이터를 사용했습니다.',
    nextAction: ['제출 전 live 수집 근거를 확보하세요.', 'fallback 원인을 확인하고 재수집을 시도하세요.'],
    confidence: 'low',
  },
  'not configured': {
    severity: 'high',
    plainSummary: '필요한 API나 출처 연결 설정이 아직 부족합니다.',
    reason: 'API key 또는 operation endpoint가 설정되지 않아 live 근거를 확인하지 못했습니다.',
    nextAction: ['API key와 endpoint 설정을 확인하세요.', '연결 전에는 담당자 원문 확인을 병행하세요.'],
    confidence: 'low',
  },
  'insufficient evidence': {
    severity: 'medium',
    plainSummary: '판단에 필요한 근거 데이터가 충분하지 않습니다.',
    reason: '현재 source trace만으로는 이 상태를 확정하기 어렵습니다.',
    nextAction: ['관련 공공데이터나 보유 문서를 추가하세요.', '다음 판단 전 근거 건수를 확인하세요.'],
    confidence: 'low',
  },
  '담당자 확인 필요': {
    severity: 'medium',
    plainSummary: '자동 진단만으로 확정하지 말고 담당자가 확인해야 합니다.',
    reason: '근거 데이터가 부족하거나 바이어 회신 전에 사람의 검토가 필요한 조건입니다.',
    nextAction: ['담당자를 지정해 원문과 보유 문서를 확인하세요.', '확인 결과를 리포트나 실행보드에 남기세요.'],
    confidence: 'medium',
  },
  '검토 필요': {
    severity: 'medium',
    plainSummary: '추가 검토 후 다음 단계로 진행하는 것이 좋습니다.',
    reason: '규제, 리콜, 특허, 지원사업 중 확인해야 할 근거가 남아 있습니다.',
    nextAction: ['전문가 또는 담당자 검토를 요청하세요.', '확인된 근거만 바이어 회신에 사용하세요.'],
    confidence: 'medium',
  },
  미연결: {
    severity: 'high',
    plainSummary: '필요한 근거 데이터 연결이 아직 되지 않았습니다.',
    reason: '공공데이터 또는 source trace 같은 근거 데이터가 연결되지 않아 자동 판단 근거가 제한됩니다.',
    nextAction: ['공공데이터/API 연결 상태를 확인하세요.', '연결 전에는 원문 문서로 보완하세요.'],
    confidence: 'low',
  },
  출처확인: {
    severity: 'medium',
    plainSummary: '표시된 판단의 원문 출처를 확인해야 합니다.',
    reason: 'live/cache/fallback 중 어떤 근거인지 확인한 뒤 비용이나 기간을 확정해야 합니다.',
    nextAction: ['근거 데이터 탭에서 source trace를 확인하세요.', '원문 링크나 보유 문서와 대조하세요.'],
    confidence: 'medium',
  },
  검토: {
    severity: 'medium',
    plainSummary: '확정 판단 전 검토가 필요한 상태입니다.',
    reason: '현재 근거만으로는 바로 확정하기 어렵습니다.',
    nextAction: ['관련 담당자가 source trace를 확인하세요.', '필요하면 외부 전문가 검토를 요청하세요.'],
    confidence: 'medium',
  },
  확인: {
    severity: 'medium',
    plainSummary: '진행 전에 확인해야 할 항목입니다.',
    reason: '자동 진단 결과를 업무 결정에 쓰기 전에 근거 확인이 필요합니다.',
    nextAction: ['근거 데이터와 보유 문서를 함께 확인하세요.', '확인 결과를 실행보드에 반영하세요.'],
    confidence: 'medium',
  },
  높음: {
    severity: 'medium',
    plainSummary: '우선 확인해야 할 가능성이 높은 항목입니다.',
    reason: '대상 제품이나 국가 조건상 중요도가 높게 표시되었습니다.',
    nextAction: ['견적 전에 관련 근거를 확인하세요.', '필요한 문서를 먼저 준비하세요.'],
    confidence: 'medium',
  },
  대기: {
    severity: 'low',
    plainSummary: '아직 판단 근거가 충분히 들어오지 않았습니다.',
    reason: '진단 결과나 source trace가 준비되기 전 상태입니다.',
    nextAction: ['진단 완료 후 다시 확인하세요.', '근거 데이터 탭에서 수집 상태를 확인하세요.'],
    confidence: 'low',
  },
}

export function statusFromText(value) {
  const text = String(value ?? '').trim()
  if (!text) return ''
  const lower = text.toLowerCase()
  for (const [pattern, status] of NEGATIVE_STATUS_PATTERNS) {
    if (lower.includes(pattern.toLowerCase())) return status
  }
  return text
}

export function needsStatusExplanation(value) {
  const text = String(value ?? '').trim()
  if (!text) return false
  const lower = text.toLowerCase()
  if (NEGATIVE_STATUS_PATTERNS.some(([pattern]) => lower.includes(pattern.toLowerCase()))) return true
  return false
}

export function isClearlyPositiveStatus(value) {
  const lower = String(value ?? '').trim().toLowerCase()
  if (!lower) return false
  return POSITIVE_STATUS_PATTERNS.some((pattern) => lower.includes(pattern.toLowerCase()))
}

export function buildStatusExplanation(value, context = {}) {
  const status = statusFromText(value)
  const template = TEMPLATES[status] ?? defaultTemplate(status)
  const sourceType = normalizeSourceType(context.sourceType)
  const evidenceCount = Number.isFinite(Number(context.evidenceCount)) ? Number(context.evidenceCount) : 0
  const reason = firstNonBlank(context.reason, context.description, template.reason)

  return {
    severity: template.severity,
    status,
    plainSummary: template.plainSummary,
    reason,
    nextAction: Array.isArray(context.nextAction) && context.nextAction.length > 0
      ? context.nextAction.slice(0, 2)
      : template.nextAction.slice(0, 2),
    sourceType,
    evidenceCount,
    confidence: context.confidence ?? template.confidence,
    dataStatus: dataStatusText(sourceType, evidenceCount),
  }
}

function defaultTemplate(status) {
  return {
    severity: isClearlyPositiveStatus(status) ? 'low' : 'medium',
    plainSummary: `${status || '이 상태'} 항목은 업무 결정 전에 의미를 확인하는 것이 좋습니다.`,
    reason: '등록된 상태값을 기준으로 담당자 확인이 필요한지 판단합니다.',
    nextAction: ['근거 데이터와 보유 문서를 함께 확인하세요.', '필요하면 담당자 검토 결과를 남기세요.'],
    confidence: 'medium',
  }
}

function normalizeSourceType(value) {
  const text = String(value ?? '').trim()
  return text || 'unknown'
}

function dataStatusText(sourceType, evidenceCount) {
  const source = normalizeSourceType(sourceType)
  const count = Number.isFinite(Number(evidenceCount)) ? Number(evidenceCount) : 0
  if (source === 'unknown') return `데이터 상태 unknown · 근거 ${count}건`
  return `데이터 상태 ${source} · 근거 ${count}건`
}

function firstNonBlank(...values) {
  for (const value of values) {
    const text = String(value ?? '').trim()
    if (text) return text
  }
  return ''
}
