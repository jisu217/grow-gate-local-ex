export const PARTNERS = ['관세청', 'KOTRA', 'KTL', 'SafetyKorea', 'KIPRIS', '기업마당', 'EU']

export const HOME_FEATURES = [
  {
    id: 'gate',
    no: '01',
    icon: '01',
    title: '한 번의 Gate 판단',
    description: '제품 정보, HS Code, 대상국, 바이어 문의를 기준으로 견적 전 진행 가능 여부를 한 화면에서 점검합니다.',
    actionLabel: '자세히 보기',
    modal: {
      title: '한 번의 Gate 판단',
      description: '제품명, HS Code, 대상국, 바이어 문의를 기준으로 견적 전 진행 가능성을 확인합니다.',
      input: '제품명, HS Code, 대상국, 바이어 문의',
      result: 'GO / CONDITIONAL GO / HOLD / STOP 중 하나로 표시합니다.',
      screen: '진단 생성 화면과 진단 결과 00 진단 개요로 연결됩니다.',
      caution: 'AI가 최종 판단하지 않고 담당자 검토를 돕습니다.',
    },
  },
  {
    id: 'evidence',
    no: '02',
    icon: '02',
    title: '근거가 보이는 데이터',
    description: '공공데이터 상태와 제품 정보를 연결해, 기업이 바로 검토할 실행 후보로 정리합니다.',
    actionLabel: '자세히 보기',
    modal: {
      title: '근거가 보이는 데이터',
      description: '관세청, KOTRA, KATS, KIPRIS, 기업마당 등 공공데이터의 live/cache/fallback 상태를 함께 보여줍니다.',
      input: '공공데이터 수집 상태, 제품 지식, source trace',
      result: '어떤 근거가 점수에 반영됐는지 확인할 수 있습니다.',
      screen: '진단 결과 10 근거 데이터와 공공데이터 수집 현황으로 연결됩니다.',
      caution: 'fallback/seed는 live 데이터처럼 표시하지 않습니다.',
    },
  },
  {
    id: 'board',
    no: '03',
    icon: '03',
    title: 'Gate 통과 할 일',
    description: '이번 주, 견적 전, 출하 전 담당자가 무엇을 확인해야 하는지 산출물 단위로 받습니다.',
    actionLabel: '자세히 보기',
    modal: {
      title: 'Gate 통과 할 일',
      description: '판단 결과를 이번 주, 견적 전, 출하 전 업무로 바꿉니다.',
      input: 'Gate 결과, 부족 근거, 담당자 확인 항목',
      result: '담당자, 산출물, 기한, 확인 이유가 있는 업무 카드로 표시합니다.',
      screen: '진단 결과 08 실행 보드와 마이페이지 오늘 할 일로 연결됩니다.',
      caution: '자동 완료가 아니라 담당자가 검토할 실행 후보입니다.',
    },
  },
]

export const HOME_READINESS = [
  { k: '공공데이터', v: '관세청·KOTRA·KATS·KIPRIS·지원사업 connector와 cache/fallback 기록' },
  { k: '서비스 구현', v: 'React 화면에서 Spring 진단 생성, 상태 갱신, 결과 리포트까지 연결' },
  { k: '운영 근거', v: 'JWT 검증, 권한 검사, audit pagination, soft delete, retry/backoff 반영' },
  { k: '제출 산출물', v: '변경사항·평가 대응·미완료 범위를 공개 문서로 분리 정리' },
]

export const HOME_BEFORE_AFTER = [
  {
    before: '담당자가 포털을 따로 열어 HS Code, 리콜, 특허, 지원사업을 수작업 확인',
    after: '제품·바이어 문의 기준으로 출처와 규칙 근거를 한 결과에 연결',
    afterOutputs: ['출처 연결', 'Gate 판단'],
  },
  {
    before: '점수는 나오지만 견적을 보내도 되는지 판단하기 어려움',
    after: 'GO / CONDITIONAL GO / HOLD / STOP으로 견적 전 Gate를 먼저 제시',
    afterOutputs: ['업무 생성'],
  },
  {
    before: '자료 누락을 알게 되어도 담당자별 다음 행동이 모호함',
    after: '이번 주·견적 전·출하 전 업무, 지식화 후보, 리포트 반영 상태를 함께 생성',
    afterOutputs: ['지식화 후보', '리포트 반영'],
  },
]

export function buildHomeStats(dashboard = {}) {
  const providers = Array.isArray(dashboard?.providers) ? dashboard.providers : []
  const providerCount = uniqueProviderCount(providers)
  const recordCount = numberOrZero(dashboard?.recordCount)
  const traceRate = recordCount > 0 ? 100 : null
  const newestFetchedAt = dashboard?.newestFetchedAt ? formatShortDateTime(dashboard.newestFetchedAt) : ''
  const liveCount = providers.reduce((sum, row) => sum + numberOrZero(row.liveCount), 0)
  const cacheCount = providers.reduce((sum, row) => sum + numberOrZero(row.cacheCount), 0)
  const fallbackCount = providers.reduce((sum, row) => sum + numberOrZero(row.fallbackCount), 0)
  const sourceHelper = newestFetchedAt
    ? `최근 수집 기준 · live ${liveCount} · cache ${cacheCount} · fallback ${fallbackCount}`
    : '최근 수집 기준 확인 필요'

  return [
    {
      value: providerCount > 0 ? `${providerCount}개` : '-',
      label: '공공데이터 provider 상태 추적',
      helper: providerCount > 0 ? sourceHelper : '최근 수집 상태 확인 필요',
      tone: providerCount > 0 ? 'ready' : 'warn',
    },
    {
      value: '6축',
      label: '시장·규제·리콜·FTO·지원 점수',
      helper: '진단 점수 축 기준',
      tone: 'ready',
    },
    {
      value: traceRate == null ? '-' : `${traceRate}%`,
      label: '출처 trace 포함 Gate 판단',
      helper: traceRate == null ? '진단 source trace 확인 필요' : 'source trace가 있는 수집 기록 기준',
      tone: traceRate == null ? 'warn' : 'ready',
    },
    {
      value: '3단계',
      label: '이번 주·견적 전·출하 전 업무화',
      helper: '실행 보드 단계 기준',
      tone: 'ready',
    },
  ]
}

export function buildHomeStatsFallback() {
  return [
    '공공데이터 provider 상태 추적',
    '시장·규제·리콜·FTO·지원 점수',
    '출처 trace 포함 Gate 판단',
    '이번 주·견적 전·출하 전 업무화',
  ].map((label) => ({
    value: '-',
    label,
    helper: '최근 수집 상태 확인 필요',
    tone: 'warn',
  }))
}

function uniqueProviderCount(providers) {
  const codes = new Set()
  providers.forEach((row) => {
    const code = String(row?.provider ?? '').trim()
    if (code) codes.add(code)
  })
  return codes.size
}

function numberOrZero(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number : 0
}

function formatShortDateTime(value) {
  const parsed = new Date(value)
  if (Number.isNaN(parsed.getTime())) return String(value).slice(0, 16)
  return parsed.toISOString().slice(0, 16).replace('T', ' ')
}
