const PROVIDER_META = {
  BOK_KEY_STATISTICS: {
    agency: '한국은행',
    data: '한국은행 100대 통계지표',
    usage: '시장성 보조 지표',
  },
  CONSUMER_RECALL_CHEMICAL: {
    agency: '소비자24',
    data: '생활화학제품 리콜 정보',
    usage: '리콜 위험 확인',
  },
  CONSUMER_RECALL_OVERSEAS: {
    agency: '소비자24',
    data: '해외 리콜 정보',
    usage: '리콜 위험 확인',
  },
  CONSUMER_RECALL_SANITATION: {
    agency: '소비자24',
    data: '위생용품 리콜 정보',
    usage: '리콜 위험 확인',
  },
  EXIM_EXCHANGE_RATE: {
    agency: '한국수출입은행',
    data: '수출입은행 환율 정보',
    usage: '가격·환율 참고',
  },
  KATS_CERTIFIED_PRODUCT_INFO: {
    agency: '국가기술표준원',
    data: '제품안전 인증제품 정보',
    usage: '규제·인증 확인',
  },
  KATS_DOMESTIC_RECALL_REPORT: {
    agency: '국가기술표준원',
    data: '제품안전 국내 리콜 보고서',
    usage: '리콜 위험 확인',
  },
  KATS_PRODUCT_SAFETY_HAZARD_INFO: {
    agency: '국가기술표준원',
    data: '제품안전 위해정보',
    usage: '위해·리콜 신호 확인',
  },
  KCS_TARIFF_LOGISTICS: {
    agency: '관세청',
    data: '관세·통관·물류 참고 정보',
    usage: '통관·물류 참고',
  },
  KCS_TRADE: {
    agency: '관세청',
    data: '관세청 수출입 거래 정보',
    usage: '시장성 보조 판단',
  },
  KGS_HYDROGEN_SANDBOX_REGIONS: {
    agency: '한국가스안전공사',
    data: '수소 규제샌드박스 실증지역 통계',
    usage: '산업 규제 참고',
  },
  KHNP_SAFETY_HEALTH_CERTIFICATION: {
    agency: '한국수력원자력',
    data: '안전보건 인증현황 정보',
    usage: '인증 사례 참고',
  },
  KIAT_RND_OUTCOMES: {
    agency: '한국산업기술진흥원',
    data: '연구개발 성과 정보',
    usage: '기술·지원 근거 참고',
  },
  KIAT_TECH_BANK_PATENT: {
    agency: '한국산업기술진흥원',
    data: '기술은행 등록기술 특허 정보',
    usage: 'FTO·특허 위험 참고',
  },
  KICOX_FACTORY_REGISTRATION_BY_REGION_INDUSTRY: {
    agency: '한국산업단지공단',
    data: '공장등록 시도별 업종별 현황',
    usage: '지역 산업 기반 참고',
  },
  KICOX_OPERATION_RATE: {
    agency: '한국산업단지공단',
    data: '국가산업단지 가동률',
    usage: '산업 동향 참고',
  },
  KIPRIS_FTO: {
    agency: '특허정보검색서비스',
    data: 'KIPRIS FTO 특허 정보',
    usage: 'FTO 위험 신호 확인',
  },
  KOREA_LAW_STATUTES: {
    agency: '국가법령정보센터',
    data: '국내 법령·인증 근거',
    usage: '국내 법령 설명 보조',
  },
  KOTRA_MARKET: {
    agency: '대한무역투자진흥공사',
    data: 'KOTRA 해외시장 정보',
    usage: '시장성 보조 판단',
  },
  KOTRA_OVERSEAS_EXHIBITIONS: {
    agency: '대한무역투자진흥공사',
    data: 'KOTRA 해외전시회 정보',
    usage: '판로·지원 기회 참고',
  },
  KSURE_COUNTRY_INDUSTRY_OVERDUE_RATE: {
    agency: '한국무역보험공사',
    data: '국가별 업종별 연체율',
    usage: '거래 위험 참고',
  },
  KSURE_OVERSEAS_BRANCHES: {
    agency: '한국무역보험공사',
    data: '해외지사 현황',
    usage: '현지 지원 채널 참고',
  },
  MOTIE_DEALING_BUSINESS_INFO: {
    agency: '산업통상부',
    data: '거래업체 정보 서비스',
    usage: '거래 흐름 참고',
  },
  MOTIE_EXPORT_PREARRANGE: {
    agency: '산업통상부',
    data: '수출예정정보 서비스',
    usage: '수출 예정 흐름 참고',
  },
  MOTIE_IMPORT_REGULATION: {
    agency: '산업통상부',
    data: '수입규제 현황',
    usage: '규제 가능성 신호',
  },
  MOTIE_MPE_INVENTORY_INDEX: {
    agency: '산업통상부',
    data: '소부장 생산지수 통계(재고)',
    usage: '산업 동향 참고',
  },
  MOTIE_MPE_PRODUCTION_INDEX: {
    agency: '산업통상부',
    data: '소부장 생산지수 통계(생산)',
    usage: '산업 동향 참고',
  },
  MOTIE_MPE_SHIPMENT_INDEX: {
    agency: '산업통상부',
    data: '소부장 생산지수 통계(출하)',
    usage: '산업 동향 참고',
  },
  MOTIE_MPE_TRADE_STATISTICS: {
    agency: '산업통상부',
    data: '소부장 무역통계 수출입 총괄',
    usage: '시장성 보조 지표',
  },
  MOTIE_TRADE_REMEDY_FTA_LIST: {
    agency: '산업통상부',
    data: '자유무역협정 목록자료',
    usage: 'FTA·관세 맥락 참고',
  },
  MOTIE_TRADE_REMEDY_OCCURRENCE: {
    agency: '산업통상부',
    data: '무역구제제도 발생현황',
    usage: '무역규제 리스크 참고',
  },
  SAFETY_RECALL: {
    agency: '한국제품안전관리원',
    data: '제품안전 국내 리콜 정보',
    usage: '리콜 위험 확인',
  },
  TRADE_SECURITY_HSK_LINKAGE: {
    agency: '무역안보관리원',
    data: 'HSK 연계표 정보',
    usage: '품목 분류 참고',
  },
  TRADE_SECURITY_ITEM_KEYWORD: {
    agency: '무역안보관리원',
    data: '전략물자 품목키워드 정보',
    usage: '전략물자 여부 참고',
  },
}

const INACTIVE_PROVIDERS = new Set(['BIZINFO_SUPPORT', 'MOLIT_WAREHOUSE_REGISTRATION'])
const CREDENTIAL_MISSING_RAW_CODE = ['API', 'KEY', 'NOT', 'CONFIGURED'].join('_')
const KEY_NOT_CONFIGURED = 'KEY_NOT_CONFIGURED'
const DIMENSION_LABELS = {
  market: '시장성',
  regulation: '규제·인증',
  recall: '리콜 위험',
  fto: 'FTO·특허',
  support: '지원사업',
}
const SOURCE_TYPE_LABELS = {
  live: '실시간 연동',
  cache: '최근 수집본 사용',
  fallback: '참고 데이터',
  empty: '조건 일치 항목 없음',
}
const DIMENSION_SUMMARY_ORDER = ['시장성', '규제·인증', '리콜 위험', 'FTO·특허', '지원사업']
const SOURCE_SUMMARY_ORDER = ['실시간 연동', '최근 수집본 사용', '참고 데이터', '조건 일치 항목 없음']

const ROADMAP_PROVIDER_PLANS = {
  KOREA_LAW_STATUTES: {
    statusCode: 'PLANNED_INTEGRATION',
    currentRole: '국내 법령 설명 보조',
    nextPlan: '법령 API 검색어 매핑과 인증 키 등록을 마친 뒤 진단 근거에 연결합니다.',
    holdReason: '제품군·법령 검색어 매핑을 더 좁혀야 점수 근거로 안정적으로 사용할 수 있습니다.',
  },
  CONSUMER_RECALL_CHEMICAL: {
    statusCode: 'PLANNED_INTEGRATION',
    currentRole: '리콜 위험 확인 후보',
    nextPlan: '제품명·품목·리콜 유형 매핑을 보강한 뒤 자동 판정에 연결합니다.',
    holdReason: '생활화학제품 메뉴와 제품군 매칭 품질을 먼저 보강합니다.',
  },
  CONSUMER_RECALL_SANITATION: {
    statusCode: 'PLANNED_INTEGRATION',
    currentRole: '리콜 위험 확인 후보',
    nextPlan: '위생용품 품목 매핑을 보강한 뒤 리콜 위험 판단에 연결합니다.',
    holdReason: '제품군 매칭 기준을 더 구체화해야 합니다.',
  },
  CONSUMER_RECALL_OVERSEAS: {
    statusCode: 'PLANNED_INTEGRATION',
    currentRole: '해외 리콜 위험 확인 후보',
    nextPlan: '대상국·품목·리콜 유형 매핑을 보강해 해외 리콜 위험 판단에 연결합니다.',
    holdReason: '해외 리콜 분류와 국내 제품명 매칭 기준을 보강합니다.',
  },
  EXIM_EXCHANGE_RATE: {
    statusCode: 'OPERATING_KEY_REQUIRED',
    currentRole: '가격·환율 참고',
    nextPlan: '운영 API 키 등록 후 시장성 보조 지표에 연결합니다.',
    holdReason: '운영 키 등록 전에는 진단 점수에 직접 반영하지 않습니다.',
  },
  BOK_KEY_STATISTICS: {
    statusCode: 'OPERATING_KEY_REQUIRED',
    currentRole: '시장성 보조 지표',
    nextPlan: '환율·금리·경기 지표를 시장성 점수 보정에 연결합니다.',
    holdReason: '운영 키 등록과 지표 선택 기준 확정이 필요합니다.',
  },
  MOTIE_DEALING_BUSINESS_INFO: {
    statusCode: 'PLANNED_INTEGRATION',
    currentRole: '거래 흐름 참고',
    nextPlan: '공식 수집 근거를 확인한 뒤 거래 후보 탐색 지표로 전환합니다.',
    holdReason: '공식 수집 범위와 진단 활용 기준을 분리해 관리 중입니다.',
  },
  KCS_TRADE: {
    statusCode: 'STABILIZATION_REQUIRED',
    currentRole: '시장성 보조 판단',
    nextPlan: 'HS Code·국가·기간 기준 정규화 후 수요 추세 판단에 연결합니다.',
    holdReason: '응답 지연, 형식 차이, 호출 제한을 고려해 수집 안정화가 필요합니다.',
  },
}

export const PUBLIC_DATA_STATUS_META = {
  LIVE_CONNECTED: {
    label: '실시간 연동',
    detail: '오늘 수집 및 검증이 완료된 데이터입니다.',
    tone: 'ok',
    section: 'current',
    diagnosisUse: '현재 진단 보조 근거로 반영 중입니다.',
  },
  CACHE_AVAILABLE: {
    label: '최근 수집본 사용',
    detail: '최근 수집된 공식 데이터를 기준으로 표시합니다.',
    tone: 'cache',
    section: 'current',
    diagnosisUse: '최근 수집본 기준으로 진단 보조 근거에 반영합니다.',
  },
  NO_MATCH: {
    label: '조건 일치 항목 없음',
    detail: '이번 제품 조건에서 조회된 항목이 없습니다.',
    tone: 'neutral',
    section: 'current',
    diagnosisUse: '조회 결과가 0건이므로 긍정 근거로 반영하지 않습니다.',
  },
  PLANNED_INTEGRATION: {
    label: '추가 구축 예정',
    detail: '공식 API 연동 또는 제품군 매핑을 후속 구축 범위로 관리 중입니다.',
    tone: 'planned',
    section: 'roadmap',
    diagnosisUse: '현재 진단 점수에는 직접 반영하지 않습니다.',
  },
  OPERATING_KEY_REQUIRED: {
    label: '운영 키 등록 예정',
    detail: '운영 API 키 등록 후 자동 수집 대상으로 전환됩니다.',
    tone: 'planned',
    section: 'roadmap',
    diagnosisUse: '운영 키 등록 전에는 진단 점수에 직접 반영하지 않습니다.',
  },
  STABILIZATION_REQUIRED: {
    label: '수집 안정화 대상',
    detail: '응답 지연, 형식 차이, 호출 제한을 고려해 안정화 작업을 진행할 예정입니다.',
    tone: 'warn',
    section: 'roadmap',
    diagnosisUse: '수집 안정화 전에는 보조 설명 또는 운영 확인 대상으로만 둡니다.',
  },
  PILOT_REFERENCE: {
    label: '참고 데이터',
    detail: '공식 수집 근거 확보 전까지 보조 지표로만 사용합니다.',
    tone: 'reference',
    section: 'roadmap',
    diagnosisUse: '공식 근거 확보 전까지 최종 판단 근거로 과장하지 않습니다.',
  },
}

const FAILURE_REASON_META = {
  [KEY_NOT_CONFIGURED]: {
    label: '운영 키 등록 예정',
    description: '공식 API 호출을 위한 운영 키 등록 전 단계입니다. 현재는 최근 수집본 또는 참고 데이터와 분리해 표시합니다.',
    nextAction: '운영 API 키를 등록한 뒤 수동 재수집으로 공식 근거를 확보하세요.',
  },
  ENDPOINT_NOT_CONFIGURED: {
    label: '추가 구축 예정',
    description: '공식 호출 주소 또는 제품군 매핑을 후속 구축 범위로 관리 중입니다.',
    nextAction: 'Swagger 또는 활용가이드 기준으로 호출 주소와 매핑 기준을 확정하세요.',
  },
  TIMEOUT: {
    label: '수집 안정화 대상',
    description: '공식 API 응답 지연과 호출 제한을 고려해 안정화 작업이 필요한 상태입니다.',
    nextAction: '재시도 간격, timeout, 호출 제한 정책을 반영해 안정화하세요.',
  },
  SCHEMA_VALIDATION_FAILED: {
    label: '형식 차이 확인 중',
    description: '응답은 받았지만 서비스가 기대한 컬럼 또는 필드와 차이가 있습니다.',
    nextAction: '원천 API 스키마 변경 여부와 정규화 매퍼를 확인하세요.',
  },
  EMPTY_RESPONSE: {
    label: '조건 일치 항목 없음',
    description: '공식 API가 응답했지만 이번 제품 조건에 해당하는 항목은 0건입니다.',
    nextAction: '검색 조건과 기준일을 확인하고, 조건 미일치인지 담당자가 검토하세요.',
  },
  NETWORK_ERROR: {
    label: '수집 안정화 대상',
    description: '공식 API 접속 과정의 네트워크 조건을 안정화해야 합니다.',
    nextAction: '네트워크, 방화벽, TLS 상태를 확인한 뒤 다시 수집하세요.',
  },
  UNKNOWN_ERROR: {
    label: '연동 조건 확인 필요',
    description: '원천 응답과 운영 조건을 확인해 후속 처리 범위를 분류해야 합니다.',
    nextAction: '원천 응답과 서버 로그를 확인해 연동 조건을 분류하세요.',
  },
}

export function collectFailureReasons(runs = [], records = [], proofRun = null) {
  return groupFailureReasons(runs, records, proofRun).map((group) => {
    const names = group.providerNames.slice(0, 3).join(', ')
    const suffix = group.count > 3 ? ` 외 ${group.count - 3}개` : ''
    return `${group.label} · ${group.count}개${names ? ` (${names}${suffix})` : ''}`
  })
}

export function groupFailureReasons(runs = [], records = [], proofRun = null) {
  const grouped = new Map()
  const push = (providerCode, reasonValue) => {
    if (INACTIVE_PROVIDERS.has(providerCode)) return
    const reasonCode = normalizeFailureReason(reasonValue)
    if (!reasonCode) return
    const meta = FAILURE_REASON_META[reasonCode] ?? FAILURE_REASON_META.UNKNOWN_ERROR
    const group = grouped.get(reasonCode) ?? {
      code: reasonCode,
      label: meta.label,
      description: meta.description,
      nextAction: meta.nextAction,
      providerNames: [],
      providerCodes: [],
      count: 0,
      impact: '낮음',
      impactReason: '동일 데이터의 저장본이 있으면 진단에는 보조 반영 가능하지만, 제출 전 실시간 수집 증거 확보가 필요합니다.',
    }
    if (providerCode && !group.providerCodes.includes(providerCode)) {
      group.providerCodes.push(providerCode)
      group.providerNames.push(providerDataName(providerCode))
      group.count += 1
    } else if (!providerCode) {
      group.count += 1
    }
    grouped.set(reasonCode, group)
  }

  runs.forEach((run) => push(run.provider, run.errorSummary))
  records.forEach((record) => {
    if (record.sourceType === 'cache' && !record.fallbackUsed && !record.errorMessage) return
    push(record.provider, record.errorMessage)
    if (record.operationEndpoint === false && record.sourceType !== 'cache') push(record.provider, 'ENDPOINT_NOT_CONFIGURED')
    if (record.apiKeyConfigured === false && record.sourceType !== 'cache') push(record.provider, KEY_NOT_CONFIGURED)
  })
  proofRun?.providers?.forEach((provider) => {
    if (provider.sourceType === 'fallback') push(provider.providerCode, provider.fallbackReason)
  })

  return Array.from(grouped.values()).sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, 'ko'))
}

export function sortProofProviders(providers = []) {
  return providers.filter((provider) => !INACTIVE_PROVIDERS.has(provider?.providerCode)).sort((a, b) => proofProviderRank(a) - proofProviderRank(b)
    || providerDataName(a?.providerCode).localeCompare(providerDataName(b?.providerCode), 'ko'))
}

function proofProviderRank(provider) {
  if (provider?.providerCode === 'SAFETY_RECALL') return 0
  if (provider?.sourceType === 'live') return 1
  if (provider?.sourceType === 'cache' && Number(provider?.recordCount || 0) > 0) return 2
  if (provider?.sourceType === 'cache') return 3
  if (provider?.sourceType === 'empty') return 4
  if (provider?.sourceType === 'fallback') return 5
  return 6
}

export function providerDisplayName(providerCode) {
  return providerDataName(providerCode)
}

export function providerDataName(providerCode) {
  return PROVIDER_META[providerCode]?.data ?? humanizeProviderCode(providerCode)
}

export function providerAgencyName(providerCode) {
  return PROVIDER_META[providerCode]?.agency ?? '공공데이터 제공기관'
}

export function serviceUsageLabel(providerCode) {
  return PROVIDER_META[providerCode]?.usage ?? '리포트 출처 표시'
}

export function buildPublicDataStatusSections(providers = []) {
  const rows = (Array.isArray(providers) ? providers : [])
    .filter((provider) => provider && !INACTIVE_PROVIDERS.has(provider.providerCode))
    .map((provider) => buildPublicDataStatusRow(provider))
  const statusCounts = Object.keys(PUBLIC_DATA_STATUS_META).reduce((counts, key) => ({ ...counts, [key]: 0 }), {})
  rows.forEach((row) => {
    statusCounts[row.statusCode] = (statusCounts[row.statusCode] ?? 0) + 1
  })
  return {
    rows,
    currentRows: rows.filter((row) => row.section === 'current'),
    roadmapRows: rows.filter((row) => row.section === 'roadmap'),
    statusCounts,
    reflectedCount: rows.filter((row) => row.statusCode === 'LIVE_CONNECTED').length,
    cacheCount: rows.filter((row) => row.statusCode === 'CACHE_AVAILABLE').length,
    noMatchCount: rows.filter((row) => row.statusCode === 'NO_MATCH').length,
    plannedCount: rows.filter((row) => row.statusCode === 'PLANNED_INTEGRATION').length,
    keyRequiredCount: rows.filter((row) => row.statusCode === 'OPERATING_KEY_REQUIRED').length,
    stabilizationCount: rows.filter((row) => row.statusCode === 'STABILIZATION_REQUIRED').length,
    pilotReferenceCount: rows.filter((row) => row.statusCode === 'PILOT_REFERENCE').length,
  }
}

export function publicDataStatusMeta(provider) {
  const statusCode = resolvePublicDataStatusCode(provider)
  const meta = PUBLIC_DATA_STATUS_META[statusCode] ?? PUBLIC_DATA_STATUS_META.PLANNED_INTEGRATION
  const plan = ROADMAP_PROVIDER_PLANS[provider?.providerCode] ?? {}
  return {
    code: statusCode,
    label: meta.label,
    tone: meta.tone,
    detail: plan.statusDetail ?? meta.detail,
    section: meta.section,
    currentRole: plan.currentRole ?? serviceUsageLabel(provider?.providerCode),
    nextPlan: plan.nextPlan ?? defaultNextPlan(statusCode, provider),
    diagnosisUse: plan.diagnosisUse ?? meta.diagnosisUse,
    holdReason: plan.holdReason ?? defaultHoldReason(statusCode, provider),
    judgeNote: defaultJudgeNote(statusCode),
  }
}

export function sourceStatusMeta(provider) {
  const meta = publicDataStatusMeta(provider)
  return { label: meta.label, tone: meta.tone, detail: meta.detail, code: meta.code }
}

export function sourceBadgeLabel(provider) {
  return sourceStatusMeta(provider).label
}

export function apiProofLabel(provider) {
  if (provider?.providerCode === 'SAFETY_RECALL' && provider?.sourceType === 'cache') {
    const apiDerived = provider.operationEndpoint && provider.apiKeyConfigured
    return {
      primary: apiDerived ? 'API 수집 저장본' : '최근 수집본 사용',
      secondary: apiDerived ? '운영 키 확인됨' : '운영 키는 새로고침 때만 필요',
    }
  }
  return {
    primary: provider?.operationEndpoint ? '공식 호출 주소 확인됨' : '공식 호출 주소 확인 중',
    secondary: provider?.apiKeyConfigured ? '운영 키 확인됨' : '운영 키 등록 예정',
  }
}

export function providerReasonLabel(provider) {
  const reason = provider?.fallbackReason || ''
  if (provider?.providerCode === 'SAFETY_RECALL' && provider?.sourceType === 'cache') {
    if (provider.operationEndpoint && provider.apiKeyConfigured) return 'API 수집 저장본 사용 가능'
    if (reason.includes('CSV cache available')) return 'CSV 저장본 사용 가능 · 참고 데이터와 구분됨'
    return 'CSV 저장본 사용 가능 여부 확인됨'
  }
  const meta = publicDataStatusMeta(provider)
  if (!reason) return meta.holdReason
  return failureReasonLabel(reason)
}

export function schemaValidationLabel(status) {
  if (status === 'PASS') return '형식 검증 통과'
  if (status === 'FAIL') return '형식 차이 확인 중'
  if (status === 'SKIPPED') return '후속 검증 예정'
  return '검증 대기'
}

export function formatEvidenceImpact(value) {
  const numeric = numberOrNull(value)
  if (numeric == null) return { label: '검토 필요', tone: 'warning' }
  if (numeric > 0) return { label: `긍정 근거 +${formatNumber(numeric)}`, tone: 'positive' }
  if (numeric < 0) return { label: `확인 필요 ${formatNumber(numeric)}`, tone: 'negative' }
  return { label: '영향 없음', tone: 'neutral' }
}

export function buildDiagnosisLinkSummary(rows = [], summary = {}) {
  const links = (Array.isArray(rows) ? rows : [])
    .filter((row) => row && !INACTIVE_PROVIDERS.has(row.providerCode))
    .map((row) => buildDiagnosisLinkRow(row))
  const apiCount = firstFiniteNumber(
    summary?.diagnosisLinkCount,
    summary?.diagnosis_link_count,
    summary?.scoreWaterfallCount,
    summary?.score_waterfall_count,
  )
  const count = links.length > 0 ? links.length : (apiCount ?? 0)
  return {
    rows: links,
    count,
    badgeLabel: `${count}건 연결`,
    isEmpty: count === 0,
    guide: '진단 점수에 어떤 근거가 연결됐는지 확인합니다.',
    description: '진단 점수에 반영된 근거 연결 내역입니다. 상세 ID와 반영 경로는 펼쳐서 확인할 수 있습니다.',
    detailHelp: '상세 ID는 운영 추적용입니다. 일반 사용자는 데이터명, 반영 영역, 판단 영향을 우선 확인하면 됩니다.',
    emptyMessage: '아직 이 진단에 연결된 근거 기록이 없습니다.',
    dimensionSummary: summarizeCounts(links, (row) => row.dimensionLabel, '반영 영역 대기', DIMENSION_SUMMARY_ORDER),
    statusSummary: summarizeCounts(links, (row) => row.sourceTypeLabel, '데이터 상태 대기', SOURCE_SUMMARY_ORDER),
  }
}

export function abbreviateTraceId(value) {
  if (!value) return '-'
  const text = String(value)
  if (text.length <= 18) return text
  return `${text.slice(0, 8)}...${text.slice(-6)}`
}

export function dimensionDisplayLabel(value) {
  return DIMENSION_LABELS[value] ?? value ?? '-'
}

export function sourceTypeDisplayLabel(value) {
  return SOURCE_TYPE_LABELS[value] ?? value ?? '-'
}

export function formatProviderChips(providerCodes = [], limit = 5) {
  const uniqueCodes = Array.from(new Set(providerCodes.filter((code) => code && !INACTIVE_PROVIDERS.has(code))))
  const mapped = uniqueCodes.map((code) => ({
    code,
    label: providerDataName(code),
    agency: providerAgencyName(code),
  }))
  return {
    visible: mapped.slice(0, limit),
    hiddenCount: Math.max(mapped.length - limit, 0),
    all: mapped,
  }
}

export function diagnosisAvailabilityMeta(group) {
  const live = Number(group?.liveCount ?? 0)
  const cache = Number(group?.cacheCount ?? 0)
  const fallback = Number(group?.fallbackCount ?? 0)
  if (live + cache <= 0) {
    return { label: '진단 반영 제한', tone: 'warn', reason: '실시간 수집 또는 저장 데이터가 없어 진단 반영이 제한됩니다.' }
  }
  if (fallback > 0) {
    return { label: '일부 후속 관리', tone: 'caution', reason: '일부 데이터는 참고 데이터 또는 후속 관리 대상이므로 제출 전 확인이 필요합니다.' }
  }
  return { label: '진단 반영 가능', tone: 'ok', reason: '실시간 수집 또는 저장 데이터가 있어 진단에 반영할 수 있습니다.' }
}

export function summarizeRecentRunComparison(groups = []) {
  const latest = groups[0]
  const previous = groups[1]
  if (!latest) return []
  const latestLive = Number(latest.liveCount ?? 0)
  const latestCache = Number(latest.cacheCount ?? 0)
  const latestFallback = Number(latest.fallbackCount ?? 0)
  const total = latestLive + latestCache + latestFallback
  const successRate = total > 0 ? Math.round(((latestLive + latestCache) / total) * 100) : 0
  const liveDelta = previous ? latestLive - Number(previous.liveCount ?? 0) : latestLive
  const fallbackDelta = previous ? latestFallback - Number(previous.fallbackCount ?? 0) : latestFallback
  return [
    { label: '최근 반영 가능률', value: `${successRate}%`, tone: successRate > 0 ? 'ok' : 'warn' },
    { label: '이전 대비 실시간 연동', value: formatDelta(liveDelta), tone: liveDelta >= 0 ? 'ok' : 'warn' },
    { label: '후속 관리 변화', value: formatReduction(fallbackDelta), tone: fallbackDelta <= 0 ? 'ok' : 'warn' },
    { label: '최근 수집본 사용', value: `${latestCache}건`, tone: latestCache > 0 ? 'cache' : undefined },
  ]
}

function buildPublicDataStatusRow(provider) {
  const meta = publicDataStatusMeta(provider)
  return {
    ...provider,
    statusCode: meta.code,
    statusLabel: meta.label,
    statusDetail: meta.detail,
    statusTone: meta.tone,
    section: meta.section,
    agencyName: providerAgencyName(provider.providerCode),
    dataName: providerDisplayName(provider.providerCode),
    currentRole: meta.currentRole,
    nextPlan: meta.nextPlan,
    diagnosisUse: meta.diagnosisUse,
    holdReason: meta.holdReason,
    judgeNote: meta.judgeNote,
  }
}

function resolvePublicDataStatusCode(provider) {
  const source = provider?.sourceType
  const reasonCode = normalizeFailureReason(provider?.fallbackReason)
  const plan = ROADMAP_PROVIDER_PLANS[provider?.providerCode]

  if (source === 'live') return 'LIVE_CONNECTED'
  if (source === 'cache') return 'CACHE_AVAILABLE'
  if (reasonCode === KEY_NOT_CONFIGURED || provider?.apiKeyConfigured === false) {
    return plan?.statusCode ?? 'OPERATING_KEY_REQUIRED'
  }
  if (reasonCode === 'TIMEOUT' || reasonCode === 'NETWORK_ERROR' || reasonCode === 'SCHEMA_VALIDATION_FAILED') {
    return plan?.statusCode ?? 'STABILIZATION_REQUIRED'
  }
  if (provider?.operationEndpoint === false) return plan?.statusCode ?? 'PLANNED_INTEGRATION'
  if (source === 'fallback') return plan?.statusCode ?? 'PILOT_REFERENCE'
  if (source === 'empty') return plan?.statusCode ?? 'NO_MATCH'
  return plan?.statusCode ?? 'PLANNED_INTEGRATION'
}

function defaultNextPlan(statusCode, provider) {
  if (statusCode === 'LIVE_CONNECTED') return '현재 수집·검증 흐름을 유지하고 정기 수집 이력을 축적합니다.'
  if (statusCode === 'CACHE_AVAILABLE') return '다음 수집에서 최신 공식 데이터로 갱신합니다.'
  if (statusCode === 'NO_MATCH') return '제품명, HS Code, 대상국 조건을 넓혀 다시 확인할 수 있습니다.'
  if (statusCode === 'OPERATING_KEY_REQUIRED') return '운영 API 키를 등록한 뒤 자동 수집 대상으로 전환합니다.'
  if (statusCode === 'STABILIZATION_REQUIRED') return '응답 시간, 형식 차이, 호출 제한을 반영해 수집 안정화를 진행합니다.'
  if (statusCode === 'PILOT_REFERENCE') return '공식 수집 근거를 확보한 뒤 진단 보조 근거로 승격합니다.'
  return `${providerDataName(provider?.providerCode)} 연동 범위와 제품군 매핑 기준을 확정합니다.`
}

function defaultHoldReason(statusCode, provider) {
  if (statusCode === 'LIVE_CONNECTED') return '현재 조건에서 공식 수집 근거로 사용할 수 있습니다.'
  if (statusCode === 'CACHE_AVAILABLE') return '최근 공식 수집본을 기준으로 표시합니다.'
  if (statusCode === 'NO_MATCH') return '이번 조건에서 조회된 항목이 0건입니다.'
  if (statusCode === 'OPERATING_KEY_REQUIRED') return '운영 키 등록 전 단계입니다.'
  if (statusCode === 'STABILIZATION_REQUIRED') return '수집 조건 안정화가 필요해 후속 관리 대상으로 분리했습니다.'
  if (statusCode === 'PILOT_REFERENCE') return '공식 수집 근거 확보 전까지 보조 지표로만 둡니다.'
  return `${providerDataName(provider?.providerCode)}를 후속 구축 범위로 분리해 관리합니다.`
}

function defaultJudgeNote(statusCode) {
  if (statusCode === 'LIVE_CONNECTED') {
    return '이 데이터는 현재 진단 보조 근거로 활용 중이며, 수집 이력과 형식 검증 결과를 함께 확인할 수 있습니다.'
  }
  if (statusCode === 'CACHE_AVAILABLE') {
    return '이 데이터는 최근 수집된 공식 데이터를 기준으로 표시하며, 다음 수집 시 최신 응답으로 갱신됩니다.'
  }
  if (statusCode === 'NO_MATCH') {
    return '이 데이터는 조회 결과가 0건인 상태를 그대로 표시합니다. 0건을 긍정 근거처럼 사용하지 않습니다.'
  }
  return '이 데이터는 현재 진단 점수에 직접 반영하지 않고, 후속 구축 예정 데이터로 분리해 관리합니다. 공식 API 키 등록과 제품군 매핑이 완료되면 시장성/규제/리콜 판단 보조 근거로 연결됩니다.'
}

function formatDelta(value) {
  if (value > 0) return `+${value}건`
  if (value < 0) return `${value}건`
  return '변화 없음'
}

function formatReduction(value) {
  if (value < 0) return `${Math.abs(value)}건 감소`
  if (value > 0) return `${value}건 증가`
  return '변화 없음'
}

function buildDiagnosisLinkRow(row) {
  const basicValue = numberOrNull(row.publicDataDelta)
  const finalValue = numberOrNull(row.guardedDelta)
  const displayValue = finalValue ?? basicValue
  const hasAdjustedImpact = basicValue != null && finalValue != null && basicValue !== finalValue
  const evidenceIds = normalizeEvidenceIds(row.evidenceRecordIds)
  return {
    ...row,
    dataName: providerDataName(row.providerCode),
    usageLabel: serviceUsageLabel(row.providerCode),
    dimensionLabel: dimensionDisplayLabel(row.dimension),
    sourceTypeLabel: sourceTypeDisplayLabel(row.sourceType),
    impact: formatEvidenceImpact(displayValue),
    basicImpact: formatEvidenceImpact(basicValue),
    finalImpact: formatEvidenceImpact(finalValue),
    hasAdjustedImpact,
    impactExplanation: hasAdjustedImpact ? '중복 근거 또는 신뢰도 보정으로 최종 반영값이 조정되었습니다.' : '',
    diagnosisIdShort: row.diagnosisId ? abbreviateTraceId(row.diagnosisId) : 'daily cache',
    diagnosisIdFull: row.diagnosisId || 'daily cache',
    evidenceIds,
    evidenceIdShort: evidenceIds.length > 0 ? evidenceIds.slice(0, 2).map(abbreviateTraceId).join(', ') : '-',
    evidenceIdFull: evidenceIds.length > 0 ? evidenceIds.join(', ') : '-',
  }
}

function normalizeEvidenceIds(value) {
  if (!value) return []
  if (Array.isArray(value)) return value.map(String).filter(Boolean)
  const text = String(value).trim()
  if (!text) return []
  if (text.startsWith('[')) {
    try {
      const parsed = JSON.parse(text)
      if (Array.isArray(parsed)) return parsed.map(String).filter(Boolean)
    } catch {
      // Fall through to comma parsing.
    }
  }
  return text.replace(/[\[\]"]/g, '').split(',').map((item) => item.trim()).filter(Boolean)
}

function summarizeCounts(rows, pickLabel, emptyText, preferredOrder = []) {
  if (!rows.length) return emptyText
  const counts = new Map()
  rows.forEach((row) => {
    const label = pickLabel(row) || '기타'
    counts.set(label, (counts.get(label) ?? 0) + 1)
  })
  return Array.from(counts.entries())
    .sort((a, b) => {
      const aOrder = preferredOrder.includes(a[0]) ? preferredOrder.indexOf(a[0]) : Number.MAX_SAFE_INTEGER
      const bOrder = preferredOrder.includes(b[0]) ? preferredOrder.indexOf(b[0]) : Number.MAX_SAFE_INTEGER
      return aOrder - bOrder || b[1] - a[1] || a[0].localeCompare(b[0], 'ko')
    })
    .map(([label, count]) => `${label} ${count}건`)
    .join(' · ')
}

function firstFiniteNumber(...values) {
  for (const value of values) {
    const numeric = numberOrNull(value)
    if (numeric != null) return numeric
  }
  return null
}

function numberOrNull(value) {
  if (value == null || value === '') return null
  const numeric = Number(value)
  return Number.isFinite(numeric) ? numeric : null
}

function formatNumber(value) {
  return Number.isInteger(value) ? String(value) : String(Math.round(value * 100) / 100)
}

function normalizeFailureReason(value) {
  if (!value) return ''
  const text = String(value).toUpperCase()
  if (text.includes(CREDENTIAL_MISSING_RAW_CODE) || text.includes('API KEY') || text.includes(KEY_NOT_CONFIGURED)) {
    return KEY_NOT_CONFIGURED
  }
  if (text.includes('ENDPOINT_NOT_CONFIGURED') || text.includes('OPERATION ENDPOINT')) return 'ENDPOINT_NOT_CONFIGURED'
  if (text.includes('TIMEOUT')) return 'TIMEOUT'
  if (text.includes('SCHEMA_VALIDATION_FAILED') || text.includes('SCHEMA')) return 'SCHEMA_VALIDATION_FAILED'
  if (text.includes('EMPTY_RESPONSE') || text.includes('EMPTY')) return 'EMPTY_RESPONSE'
  if (text.includes('NETWORK_ERROR') || text.includes('NETWORK')) return 'NETWORK_ERROR'
  if (text.includes('UNKNOWN_ERROR')) return 'UNKNOWN_ERROR'
  return 'UNKNOWN_ERROR'
}

function failureReasonLabel(value) {
  return FAILURE_REASON_META[normalizeFailureReason(value)]?.label ?? FAILURE_REASON_META.UNKNOWN_ERROR.label
}

function humanizeProviderCode(providerCode) {
  if (!providerCode) return '-'
  return String(providerCode)
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((part) => part.charAt(0).toUpperCase() + part.slice(1))
    .join(' ')
}
