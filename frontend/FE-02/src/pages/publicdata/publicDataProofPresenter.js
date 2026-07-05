const PROVIDER_META = {
  BIZINFO_SUPPORT: {
    agency: '기업마당',
    data: '기업마당 지원사업',
    usage: '지원사업 매칭',
  },
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
  MOLIT_WAREHOUSE_REGISTRATION: {
    agency: '국토교통부',
    data: '물류창고업 등록정보',
    usage: '물류 기반 참고',
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

const CREDENTIAL_MISSING_RAW_CODE = ['API', 'KEY', 'NOT', 'CONFIGURED'].join('_')
const KEY_NOT_CONFIGURED = 'KEY_NOT_CONFIGURED'

const FAILURE_REASON_META = {
  [KEY_NOT_CONFIGURED]: {
    label: 'API 키 설정 필요',
    description: '공식 API를 호출하려면 서비스 키가 필요합니다. 현재는 저장 데이터 또는 예비 데이터로 진단을 보조합니다.',
    nextAction: 'API 키를 설정한 뒤 수동 재수집으로 실시간 근거를 확보하세요.',
  },
  ENDPOINT_NOT_CONFIGURED: {
    label: '공식 호출 주소 확인 필요',
    description: '공공데이터 호출 주소가 아직 확정되지 않았습니다.',
    nextAction: 'Swagger 또는 활용가이드 기준으로 endpoint 설정을 확인하세요.',
  },
  TIMEOUT: {
    label: '응답 시간 초과',
    description: '공식 API 응답이 제한 시간 안에 도착하지 않았습니다.',
    nextAction: '잠시 뒤 재수집하거나 timeout 설정과 API 상태를 확인하세요.',
  },
  SCHEMA_VALIDATION_FAILED: {
    label: '데이터 형식 확인 필요',
    description: '응답은 받았지만 서비스가 기대한 컬럼 또는 필드와 맞지 않습니다.',
    nextAction: '원천 API 스키마 변경 여부와 정규화 매퍼를 확인하세요.',
  },
  EMPTY_RESPONSE: {
    label: '응답 데이터 없음',
    description: '공식 API가 정상 응답했지만 이번 조건에 해당하는 데이터가 없습니다.',
    nextAction: '검색 조건과 기준일을 확인하고, 실제 0건인지 담당자가 검토하세요.',
  },
  NETWORK_ERROR: {
    label: '네트워크 연결 오류',
    description: '공식 API에 접속하는 과정에서 네트워크 오류가 발생했습니다.',
    nextAction: '네트워크, 방화벽, TLS 상태를 확인한 뒤 다시 수집하세요.',
  },
  UNKNOWN_ERROR: {
    label: '알 수 없는 오류',
    description: '분류되지 않은 오류입니다. 저장 데이터가 있으면 진단 보조에는 사용할 수 있습니다.',
    nextAction: '원천 응답과 서버 로그를 확인해 원인을 분류하세요.',
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
  return [...providers].sort((a, b) => proofProviderRank(a) - proofProviderRank(b)
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

export function sourceStatusMeta(provider) {
  const source = provider?.sourceType || '-'
  const quality = provider?.sourceQualityStatus || ''
  if (source === 'live') {
    return { label: '실시간 수집', tone: 'ok', detail: '공식 API에서 이번 실행 때 받은 데이터입니다.' }
  }
  if (source === 'cache') {
    const detail = quality === 'CACHE_VERIFIED' || Number(provider?.recordCount ?? 0) > 0
      ? '검증된 CSV 저장본을 사용합니다.'
      : '이전 수집 또는 저장 데이터를 사용합니다.'
    return { label: '저장 데이터 사용', tone: 'cache', detail }
  }
  if (source === 'fallback') {
    return { label: '예비 데이터 사용', tone: 'warn', detail: '공식 수집 근거가 부족해 예비 데이터로 표시합니다.' }
  }
  if (source === 'empty') {
    return { label: '응답 데이터 없음', tone: 'warn', detail: '이번 조건에서 사용할 데이터가 없습니다.' }
  }
  return { label: '확인 필요', tone: 'warn', detail: '수집 상태를 확인해야 합니다.' }
}

export function sourceBadgeLabel(provider) {
  return sourceStatusMeta(provider).label
}

export function apiProofLabel(provider) {
  if (provider?.providerCode === 'SAFETY_RECALL' && provider?.sourceType === 'cache') {
    const apiDerived = provider.operationEndpoint && provider.apiKeyConfigured
    return {
      primary: apiDerived ? 'API 수집 저장본' : '저장 데이터 사용',
      secondary: apiDerived ? 'API 키 확인됨' : 'API 키는 새로고침 때만 필요',
    }
  }
  return {
    primary: provider?.operationEndpoint ? '공식 호출 주소 확인됨' : '공식 호출 주소 확인 필요',
    secondary: provider?.apiKeyConfigured ? 'API 키 설정됨' : 'API 키 설정 필요',
  }
}

export function providerReasonLabel(provider) {
  const reason = provider?.fallbackReason || ''
  if (provider?.providerCode === 'SAFETY_RECALL' && provider?.sourceType === 'cache') {
    if (provider.operationEndpoint && provider.apiKeyConfigured) return 'API 수집 저장본 사용 가능'
    if (reason.includes('CSV cache available')) return 'CSV 저장본 사용 가능 · 예비 데이터 아님'
    return 'CSV 저장본 사용 가능 여부 확인됨'
  }
  if (!reason) return '-'
  return failureReasonLabel(reason)
}

export function schemaValidationLabel(status) {
  if (status === 'PASS') return '형식 검증 통과'
  if (status === 'FAIL') return '데이터 형식 확인 필요'
  if (status === 'SKIPPED') return '검증 생략'
  return '검증 대기'
}

export function formatProviderChips(providerCodes = [], limit = 5) {
  const uniqueCodes = Array.from(new Set(providerCodes.filter(Boolean)))
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
    return { label: '일부 확인 필요', tone: 'caution', reason: '일부 데이터는 예비 데이터이므로 제출 전 확인이 필요합니다.' }
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
    { label: '최근 수집 성공률', value: `${successRate}%`, tone: successRate > 0 ? 'ok' : 'warn' },
    { label: '이전 대비 성공', value: formatDelta(liveDelta), tone: liveDelta >= 0 ? 'ok' : 'warn' },
    { label: '실패 변화', value: formatReduction(fallbackDelta), tone: fallbackDelta <= 0 ? 'ok' : 'warn' },
    { label: '저장 데이터 사용', value: `${latestCache}건`, tone: latestCache > 0 ? 'cache' : undefined },
  ]
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
