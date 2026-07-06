import { Fragment, useEffect, useMemo, useState } from 'react'
import styles from './DailyPublicData.module.css'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import {
  collectPublicData,
  createPublicDataProofRun,
  getLatestPublicDataProofRun,
  getPublicDataDashboard,
  getPublicDataRecords,
  getPublicDataRuns,
  refreshDailyPublicData,
} from '../../services/publicDataService.js'
import { getAuthUser } from '../../services/authService.js'
import {
  apiProofLabel,
  buildDiagnosisLinkSummary,
  buildPublicDataStatusSections,
  diagnosisAvailabilityMeta,
  formatProviderChips,
  groupFailureReasons,
  providerAgencyName,
  providerReasonLabel,
  schemaValidationLabel,
  sortProofProviders,
  summarizeRecentRunComparison,
} from './publicDataProofPresenter.js'

const INACTIVE_PROVIDER_CODES = new Set(['BIZINFO_SUPPORT', 'MOLIT_WAREHOUSE_REGISTRATION'])

export default function DailyPublicData() {
  const authUser = getAuthUser()
  const canRunManualCollection = Boolean(authUser?.userId)
  const [dashboard, setDashboard] = useState(null)
  const [runs, setRuns] = useState([])
  const [records, setRecords] = useState([])
  const [pageState, setPageState] = useState('loading')
  const [refreshState, setRefreshState] = useState('idle')
  const [refreshResult, setRefreshResult] = useState(null)
  const [proofRun, setProofRun] = useState(null)
  const [proofState, setProofState] = useState('idle')
  const [proofNotice, setProofNotice] = useState('')
  const [recordFilter, setRecordFilter] = useState('all')
  const [expandedProviders, setExpandedProviders] = useState({})
  const [expandedRuns, setExpandedRuns] = useState({})
  const [diagnosisLinksExpanded, setDiagnosisLinksExpanded] = useState(false)
  const [manualForm, setManualForm] = useState({
    hsCode: '850760',
    targetCountry: 'KR',
    productName: '무역',
    lawSearchQuery: '무역',
  })
  const [manualCollectState, setManualCollectState] = useState('idle')
  const [manualCollectMessage, setManualCollectMessage] = useState('')

  const load = async () => {
    setPageState('loading')
    const [dashboardResult, runResult, recordResult, proofResult] = await Promise.allSettled([
      getPublicDataDashboard(),
      getPublicDataRuns(),
      getPublicDataRecords(),
      getLatestPublicDataProofRun(),
    ])

    const dashboardData = valueOrNull(dashboardResult)
    const runData = valueOrNull(runResult)
    const recordData = valueOrNull(recordResult)
    const proofData = valueOrNull(proofResult)
    const proofFallbackDashboard = proofDashboard(proofData)

    setDashboard(hasDashboardEvidence(dashboardData) ? dashboardData : (proofFallbackDashboard ?? dashboardData))
    setRuns(runData?.runs ?? [])
    setRecords(recordData?.records ?? [])

    if (proofData) {
      setProofRun(proofData)
      setProofState('ready')
      setProofNotice('')
    } else {
      setProofRun(null)
      setProofState('issue')
      setProofNotice('proof run 조회를 완료하지 못했습니다.')
    }

    if (dashboardData || runData || recordData || proofData) {
      setPageState('ok')
    } else {
      setPageState('issue')
    }
  }

  useEffect(() => {
    load()
  }, [])

  const dailyGroups = useMemo(() => groupRunsByDate(runs, records), [runs, records])
  const recentRunGroups = dailyGroups.slice(0, 2)
  const latestRun = runs[0]
  const externalCount = records.filter((record) => isExternalRecord(record) && !record.fallbackUsed).length
  const failureGroups = groupFailureReasons(runs, records, proofRun)
  const proofProviders = sortProofProviders(proofRun?.providers ?? [])
  const statusSections = useMemo(() => buildPublicDataStatusSections(proofProviders), [proofProviders])
  const filteredProofProviders = filterProofProviders(proofProviders, recordFilter)
  const filteredStatusSections = useMemo(() => buildPublicDataStatusSections(filteredProofProviders), [filteredProofProviders])
  const activeScoreWaterfall = (proofRun?.scoreWaterfall ?? []).filter((row) => !INACTIVE_PROVIDER_CODES.has(row.providerCode))
  const diagnosisLinkSummary = useMemo(
    () => buildDiagnosisLinkSummary(activeScoreWaterfall, proofRun),
    [activeScoreWaterfall, proofRun],
  )
  const uniqueAgencies = new Set(proofProviders.map((provider) => providerAgencyName(provider.providerCode)))
  const totalRecordCount = proofProviders.reduce((sum, provider) => sum + Number(provider.recordCount ?? 0), 0)
  const comparisonCards = summarizeRecentRunComparison(recentRunGroups)

  const handleRefresh = async () => {
    setRefreshState('running')
    setRefreshResult(null)
    try {
      const result = await refreshDailyPublicData()
      setRefreshResult(result)
      await load()
      setRefreshState('done')
    } catch {
      setRefreshState('issue')
    }
  }

  const handleManualCollect = async (event) => {
    event.preventDefault()
    setManualCollectState('running')
    setManualCollectMessage('')
    try {
      const response = await collectPublicData({
        hsCode: manualForm.hsCode.trim(),
        targetCountry: manualForm.targetCountry.trim(),
        productName: manualForm.productName.trim(),
        lawSearchQuery: manualForm.lawSearchQuery.trim(),
      })
      const runId = response?.run?.id || response?.runId || 'created'
      setManualCollectMessage(`수집 run 생성: ${runId}`)
      await load()
      setManualCollectState('done')
    } catch {
      setManualCollectMessage('검색 수집을 완료하지 못했습니다. 입력 조건과 운영 연동 상태를 확인하세요.')
      setManualCollectState('issue')
    }
  }

  const updateManualForm = (field, value) => {
    setManualForm((prev) => ({ ...prev, [field]: value }))
  }

  const handleProofRun = async () => {
    setProofState('running')
    setProofNotice('')
    try {
      const next = await createPublicDataProofRun('daily-refresh-screen')
      setProofRun(next)
      setProofState('done')
    } catch {
      setProofNotice('proof run 생성을 완료하지 못했습니다.')
      setProofState('issue')
    }
  }

  return (
    <main className={styles.wrap}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>공공데이터 수집 현황</h1>
          <p className={styles.sub}>
            매일 08:00 KST에 최근 활성 제품 최대 30개를 대상으로 공공데이터와 허용된 RSS/API feed를 수집합니다.
          </p>
        </div>
        {canRunManualCollection && (
          <div className={styles.actions}>
            <button className={styles.secondaryBtn} onClick={handleProofRun} disabled={proofState === 'running'}>
              {proofState === 'running' ? 'Proof 생성 중' : 'Proof run 생성'}
            </button>
            <button className={styles.refreshBtn} onClick={handleRefresh} disabled={refreshState === 'running'}>
              {refreshState === 'running' ? '수집 중' : '수동 재수집'}
            </button>
          </div>
        )}
      </div>

      {pageState === 'loading' && <StatusMessage type="loading" message="수집 로그를 불러오는 중입니다." />}
      {pageState === 'issue' && (
        <StatusMessage
          type="empty"
          message="수집 로그를 불러오지 못했습니다. 네트워크를 확인하고 다시 시도해주세요."
          onAction={load}
        />
      )}

      {pageState === 'ok' && (
        <>
          {refreshState === 'issue' && (
            <div className={styles.warn}>수동 재수집 요청을 완료하지 못했습니다. 운영 키, 호출 주소, 응답 시간을 확인하세요.</div>
          )}
          {refreshResult && (
            <div className={styles.notice}>
              수동 재수집 완료: 대상 {refreshResult.plannedCount ?? 0}개, live {refreshResult.successCount ?? 0}건,
              cache {refreshResult.cacheHitCount ?? 0}건, 후속 관리 {refreshResult.fallbackCount ?? 0}건
            </div>
          )}
          {proofNotice && <div className={styles.warn}>{proofNotice}</div>}

          <section className={styles.introCard}>
            <h2>수집 상태 안내</h2>
            <p className={styles.sub}>
              GROW-GATE Local EX가 매일 수집·검증하는 공공데이터 연결 상태입니다.
            </p>
            <p className={styles.guidance}>
              현재 반영 중인 데이터와 후속 구축 예정 데이터를 분리해 보여줍니다. 조회 결과가 0건이어도 조건 미일치인지, 운영 연동 준비 중인지 구분합니다.
            </p>
          </section>

          {canRunManualCollection && (
            <section className={styles.manualSearch}>
              <div className={styles.sectionHead}>
                <h2>검색 수집</h2>
                <span>국가법령정보센터 검색어와 제품 조건으로 공공데이터를 다시 수집합니다.</span>
              </div>
              <form className={styles.manualForm} onSubmit={handleManualCollect}>
                <label>
                  <span>HS Code</span>
                  <input
                    value={manualForm.hsCode}
                    onChange={(event) => updateManualForm('hsCode', event.target.value)}
                    inputMode="numeric"
                    pattern="[0-9]{6,10}"
                    required
                  />
                </label>
                <label>
                  <span>대상국</span>
                  <input
                    value={manualForm.targetCountry}
                    onChange={(event) => updateManualForm('targetCountry', event.target.value)}
                    maxLength={40}
                    required
                  />
                </label>
                <label>
                  <span>제품명</span>
                  <input
                    value={manualForm.productName}
                    onChange={(event) => updateManualForm('productName', event.target.value)}
                    maxLength={160}
                    required
                  />
                </label>
                <label>
                  <span>법령 검색어</span>
                  <input
                    value={manualForm.lawSearchQuery}
                    onChange={(event) => updateManualForm('lawSearchQuery', event.target.value)}
                    maxLength={80}
                    placeholder="무역"
                  />
                </label>
                <button className={styles.refreshBtn} type="submit" disabled={manualCollectState === 'running'}>
                  {manualCollectState === 'running' ? '검색 중' : '검색 수집'}
                </button>
              </form>
              {manualCollectMessage && (
                <div className={manualCollectState === 'issue' ? styles.warn : styles.notice}>{manualCollectMessage}</div>
              )}
            </section>
          )}

          <section className={styles.summaryGrid} aria-label="public data collection summary">
            <Metric label="연결 기관" value={`${uniqueAgencies.size}개`} />
            <Metric label="수집 데이터" value={`${proofProviders.length || dashboard?.recordCount || 0}개`} />
            <Metric label="오늘 수집 완료" value={`${statusSections.reflectedCount || dashboard?.successCount || 0}개`} tone="ok" />
            <Metric label="최근 수집본 사용" value={`${statusSections.cacheCount || dashboard?.cacheHitCount || 0}개`} />
            <Metric label="조건 일치 없음" value={`${statusSections.noMatchCount}개`} />
            <Metric label="추가 구축 예정" value={`${statusSections.plannedCount}개`} tone="warn" />
            <Metric label="운영 키 등록 예정" value={`${statusSections.keyRequiredCount}개`} tone="warn" />
            <Metric label="수집 안정화 대상" value={`${statusSections.stabilizationCount}개`} tone="warn" />
            <Metric label="총 수집 건수" value={`${totalRecordCount || dashboard?.recordCount || 0}건`} />
            <Metric label="최근 수집일" value={formatDateTime(latestRun?.startedAt ?? dashboard?.newestFetchedAt)} />
            <Metric label="외부 보조 데이터" value={`${externalCount}건`} />
          </section>

          {uniqueAgencies.size > 0 && (
            <section className={styles.section}>
              <div className={styles.sectionHead}>
                <h2>기관별 수집 상태 요약</h2>
                <span>기본 화면에는 기관명을 우선 표시하고, 상세에서 provider key를 확인합니다.</span>
              </div>
              <div className={styles.agencyGrid}>
                {Array.from(uniqueAgencies).slice(0, 20).map((agency) => (
                  <span key={agency} className={styles.agencyChip}>{agency}</span>
                ))}
              </div>
            </section>
          )}

          <section className={styles.section}>
            <div className={styles.sectionHead}>
              <h2>수집 데이터 확인</h2>
              <span>기관, 데이터명, 수집 상태, 검증 결과를 쉬운 말로 확인합니다.</span>
            </div>
            <div className={styles.proofGrid}>
              <Metric label="Proof 실행 ID" value={proofRun?.proofRunId ?? proofRun?.proof_run_id ?? '-'} />
              <Metric label="검증 상태" value={proofRun?.proofStatus ?? proofRun?.proof_status ?? proofState} tone={proofRun?.proofStatus === 'PASS' ? 'ok' : 'warn'} />
              <Metric label="형식 검증 통과" value={`${proofRun?.schemaPassCount ?? proofRun?.schema_pass_count ?? 0}건`} tone="ok" />
              <Metric label="형식 확인 필요" value={`${proofRun?.schemaFailCount ?? proofRun?.schema_fail_count ?? 0}건`} tone={(proofRun?.schemaFailCount ?? proofRun?.schema_fail_count ?? 0) > 0 ? 'warn' : undefined} />
            </div>
            <div className={styles.filterBar} aria-label="수집 데이터 필터">
              {[
                ['all', '전체'],
                ['live', '실시간 연동'],
                ['cache', '최근 수집본 사용'],
                ['fallback', '참고 데이터'],
                ['attention', '후속 관리'],
              ].map(([value, label]) => (
                <button
                  key={value}
                  type="button"
                  className={`${styles.filterBtn} ${recordFilter === value ? styles.filterBtnActive : ''}`}
                  onClick={() => setRecordFilter(value)}
                >
                  {label}
                </button>
              ))}
            </div>

            <PublicDataStatusTable
              title="현재 진단에 반영 중인 데이터"
              description="실시간 연동, 최근 수집본, 조건 미일치 상태를 구분합니다. 0건은 이번 조건에서 조회 결과가 없다는 뜻입니다."
              rows={filteredStatusSections.currentRows}
              expandedProviders={expandedProviders}
              setExpandedProviders={setExpandedProviders}
              emptyText="현재 필터 조건에 표시할 반영 중 데이터가 없습니다."
            />
            <PublicDataStatusTable
              title="추가 구축 예정 데이터"
              description="운영 키 등록, 제품군 매핑, 수집 안정화가 필요한 데이터를 후속 구축 범위로 분리했습니다."
              rows={filteredStatusSections.roadmapRows}
              expandedProviders={expandedProviders}
              setExpandedProviders={setExpandedProviders}
              emptyText="현재 필터 조건에 표시할 후속 구축 대상이 없습니다."
            />
          </section>

          <section className={styles.section}>
            <div className={styles.diagnosisLinkPanel}>
              <div className={styles.diagnosisLinkHeader}>
                <button
                  type="button"
                  className={styles.diagnosisLinkTitle}
                  aria-expanded={diagnosisLinksExpanded}
                  aria-controls="diagnosis-link-detail"
                  onClick={() => setDiagnosisLinksExpanded((prev) => !prev)}
                >
                  <span>진단 사용 연결</span>
                  <span className={styles.countBadge}>{diagnosisLinkSummary.badgeLabel}</span>
                </button>
                <button
                  type="button"
                  className={styles.expandBtn}
                  aria-expanded={diagnosisLinksExpanded}
                  aria-controls="diagnosis-link-detail"
                  onClick={() => setDiagnosisLinksExpanded((prev) => !prev)}
                  disabled={diagnosisLinkSummary.count === 0}
                >
                  {diagnosisLinksExpanded ? '접기' : '펼쳐보기'} <span aria-hidden="true">{diagnosisLinksExpanded ? '⌃' : '⌄'}</span>
                </button>
              </div>
              <p className={styles.diagnosisLinkGuide}>{diagnosisLinkSummary.description}</p>
              <div className={styles.linkSummaryGrid} aria-label="진단 사용 연결 요약">
                <div>
                  <span>반영 영역</span>
                  <strong>{diagnosisLinkSummary.dimensionSummary}</strong>
                </div>
                <div>
                  <span>데이터 상태</span>
                  <strong>{diagnosisLinkSummary.statusSummary}</strong>
                </div>
              </div>
              {diagnosisLinkSummary.count === 0 && (
                <div className={styles.empty}>{diagnosisLinkSummary.emptyMessage}</div>
              )}
            </div>
            <div
              id="diagnosis-link-detail"
              className={`${styles.tableWrap} ${styles.diagnosisLinkDetail}`}
              hidden={!diagnosisLinksExpanded}
            >
              <p className={styles.detailHelp}>{diagnosisLinkSummary.detailHelp}</p>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>데이터</th>
                    <th>반영 영역</th>
                    <th>데이터 상태</th>
                    <th>판단 영향</th>
                    <th>근거 기록</th>
                    <th>운영 추적 ID</th>
                  </tr>
                </thead>
                <tbody>
                  {diagnosisLinkSummary.rows.length === 0 && (
                    <tr><td colSpan="6" className={styles.empty}>{diagnosisLinkSummary.emptyMessage}</td></tr>
                  )}
                  {diagnosisLinkSummary.rows.slice(0, 12).map((row, index) => (
                    <tr key={`${row.diagnosisId || 'cache'}-${row.providerCode}-${row.dimension}-${index}`}>
                      <td>
                        {row.dataName}
                        <span className={styles.inlineMeta}>{row.providerCode}</span>
                      </td>
                      <td>{row.dimensionLabel}</td>
                      <td>{row.sourceTypeLabel}</td>
                      <td>
                        <span className={`${styles.impactBadge} ${impactClass(row.impact.tone)}`}>{row.impact.label}</span>
                        {row.hasAdjustedImpact && (
                          <span className={styles.impactFlow}>
                            기본 반영: {row.basicImpact.label} → 최종 반영: {row.finalImpact.label}
                            <small>{row.impactExplanation}</small>
                          </span>
                        )}
                      </td>
                      <td title={row.evidenceIdFull}>{row.evidenceIdShort}</td>
                      <td>
                        <span className={styles.traceId} title={row.diagnosisIdFull}>진단 {row.diagnosisIdShort}</span>
                        <span className={styles.inlineMeta} title={row.evidenceIdFull}>근거 {row.evidenceIdShort}</span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className={styles.section}>
            <div className={styles.sectionHead}>
              <h2>최근 수집 실행 이력</h2>
              <span>최근 2회 수집 결과를 기준으로 실시간 연동, 최근 수집본, 후속 관리 대상을 확인합니다.</span>
            </div>
            {comparisonCards.length > 0 && (
              <div className={styles.comparisonGrid}>
                {comparisonCards.map((card) => (
                  <Metric key={card.label} label={card.label} value={card.value} tone={card.tone === 'ok' ? 'ok' : card.tone === 'warn' ? 'warn' : undefined} />
                ))}
              </div>
            )}
            <div className={styles.tableWrap}>
              <table className={`${styles.table} ${styles.runTable}`}>
                <thead>
                  <tr>
                    <th>수집일</th>
                    <th>수집 대상</th>
                    <th>실시간 연동</th>
                    <th>최근 수집본</th>
                    <th>후속 관리</th>
                    <th>외부 데이터</th>
                    <th>진단 반영</th>
                    <th>상세</th>
                  </tr>
                </thead>
                <tbody>
                  {recentRunGroups.length === 0 && (
                    <tr><td colSpan="8" className={styles.empty}>아직 수집 run이 없습니다.</td></tr>
                  )}
                  {recentRunGroups.map((group) => {
                    const availability = diagnosisAvailabilityMeta(group)
                    const chips = formatProviderChips(group.providers, 5)
                    const expanded = Boolean(expandedRuns[group.date])
                    const shownChips = expanded ? chips.all : chips.visible
                    return (
                      <tr key={group.date}>
                        <td data-label="수집일">{group.date}</td>
                        <td data-label="수집 대상">제품 {group.productCount}개</td>
                        <td data-label="실시간 연동" className={styles.ok}>실시간 연동 {group.liveCount}건</td>
                        <td data-label="최근 수집본">최근 수집본 {group.cacheCount}건</td>
                        <td data-label="후속 관리" className={group.fallbackCount > 0 ? styles.warnText : ''}>후속 관리 {group.fallbackCount}건</td>
                        <td data-label="외부 데이터">외부 보조 데이터 {group.externalCount}건</td>
                        <td data-label="진단 반영">
                          <span className={`${styles.badge} ${availability.tone === 'ok' ? styles.badgeOk : styles.badgeWarn}`}>
                            {availability.label}
                          </span>
                          <span className={styles.inlineMeta}>{availability.reason}</span>
                        </td>
                        <td data-label="상세">
                          <div className={styles.providerChips}>
                            {shownChips.map((chip) => (
                              <span key={`${group.date}-${chip.code}`} className={styles.providerChip} title={chip.code}>
                                {chip.label}
                              </span>
                            ))}
                            {chips.hiddenCount > 0 && (
                              <button
                                type="button"
                                className={styles.moreBtn}
                                onClick={() => setExpandedRuns((prev) => ({ ...prev, [group.date]: !expanded }))}
                              >
                                {expanded ? '접기' : `+ ${chips.hiddenCount}개 더보기`}
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    )
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <section className={styles.section}>
            <div className={styles.sectionHead}>
              <h2>후속 관리 사유</h2>
              <span>원인별로 묶어서 진단 영향도와 다음 조치를 확인합니다.</span>
            </div>
            <div className={styles.failureGrid}>
              {failureGroups.length === 0 && <div className={styles.empty}>최근 후속 관리 사유가 없습니다.</div>}
              {failureGroups.map((group) => (
                <article key={group.code} className={styles.failureCard}>
                  <div className={styles.failureTitle}>
                    <strong>{group.label} · {group.count}개</strong>
                    <span>진단 영향도: {group.impact}</span>
                  </div>
                  <p>{group.description}</p>
                  <div className={styles.providerChips}>
                    {group.providerNames.map((name) => (
                      <span key={`${group.code}-${name}`} className={styles.providerChip}>{name}</span>
                    ))}
                  </div>
                  <p className={styles.reasonHelp}>이유: {group.impactReason}</p>
                  <p className={styles.reasonHelp}>다음 조치: {group.nextAction}</p>
                </article>
              ))}
            </div>
          </section>
        </>
      )}
    </main>
  )
}

function valueOrNull(result) {
  return result.status === 'fulfilled' ? result.value : null
}

function hasDashboardEvidence(dashboard) {
  if (!dashboard) return false
  return Boolean(
    (dashboard.runCount ?? 0) > 0 ||
    (dashboard.recordCount ?? 0) > 0 ||
    (dashboard.successCount ?? 0) > 0 ||
    (dashboard.cacheHitCount ?? 0) > 0 ||
    (dashboard.fallbackCount ?? 0) > 0 ||
    (dashboard.providers?.length ?? 0) > 0,
  )
}

function proofDashboard(proofRun) {
  if (!proofRun) return null
  return {
    runCount: proofRun.proofRunId || proofRun.proof_run_id ? 1 : 0,
    recordCount: proofRun.totalProviderCount ?? proofRun.total_provider_count ?? 0,
    successCount: proofRun.liveProviderCount ?? proofRun.live_provider_count ?? 0,
    fallbackCount: proofRun.fallbackProviderCount ?? proofRun.fallback_provider_count ?? 0,
    cacheHitCount: proofRun.cacheProviderCount ?? proofRun.cache_provider_count ?? 0,
    newestFetchedAt: proofRun.completedAt ?? proofRun.completed_at ?? proofRun.startedAt ?? proofRun.started_at,
    providers: Array.isArray(proofRun.providers)
      ? proofRun.providers.map((provider) => ({ provider: provider.providerCode })).filter((provider) => provider.provider)
      : [],
  }
}

function Metric({ label, value, detail, tone }) {
  return (
    <div className={styles.metric}>
      <span>{label}</span>
      <b className={tone === 'ok' ? styles.ok : tone === 'warn' ? styles.warnText : ''}>{value}</b>
      {detail && <small>{detail}</small>}
    </div>
  )
}

function PublicDataStatusTable({
  title,
  description,
  rows,
  expandedProviders,
  setExpandedProviders,
  emptyText,
}) {
  return (
    <section className={styles.statusSection}>
      <div className={styles.statusSectionHead}>
        <h3>{title}</h3>
        <span>{description}</span>
      </div>
      <div className={styles.tableWrap}>
        <table className={styles.table}>
          <thead>
            <tr>
              <th>기관</th>
              <th>데이터명</th>
              <th>현재 상태</th>
              <th>수집 건수</th>
              <th>현재 역할</th>
              <th>후속 구축</th>
              <th>상세</th>
            </tr>
          </thead>
          <tbody>
            {rows.length === 0 && (
              <tr><td colSpan="7" className={styles.empty}>{emptyText}</td></tr>
            )}
            {rows.map((row) => {
              const apiProof = apiProofLabel(row)
              const expanded = Boolean(expandedProviders[row.providerCode])
              const detailId = `public-data-detail-${row.providerCode}`
              return (
                <Fragment key={`${row.providerCode}-${row.rawResponseSha256 || row.fallbackReason || row.fetchedAt}`}>
                  <tr>
                    <td data-label="기관">{row.agencyName}</td>
                    <td data-label="데이터명">
                      {row.dataName}
                      <span className={styles.inlineMeta}>{row.currentRole}</span>
                    </td>
                    <td data-label="현재 상태">
                      <span className={`${styles.statusBadge} ${statusClass(row.statusTone)}`}>{row.statusLabel}</span>
                      <span className={styles.inlineMeta}>{row.statusDetail}</span>
                    </td>
                    <td data-label="수집 건수">{row.recordCount ?? 0}건</td>
                    <td data-label="현재 역할">{row.currentRole}</td>
                    <td data-label="후속 구축">{row.nextPlan}</td>
                    <td data-label="상세">
                      <button
                        type="button"
                        className={styles.detailBtn}
                        aria-expanded={expanded}
                        aria-controls={detailId}
                        onClick={() => setExpandedProviders((prev) => ({ ...prev, [row.providerCode]: !expanded }))}
                      >
                        {expanded ? '접기' : '상세 보기'}
                      </button>
                    </td>
                  </tr>
                  {expanded && (
                    <tr id={detailId} className={styles.detailRow}>
                      <td colSpan="7">
                        <div className={styles.detailSummaryGrid}>
                          <div>
                            <span>현재 상태</span>
                            <strong>{row.statusLabel}</strong>
                            <p>{row.statusDetail}</p>
                          </div>
                          <div>
                            <span>이번 진단 반영 여부</span>
                            <strong>{row.diagnosisUse}</strong>
                            <p>{row.holdReason}</p>
                          </div>
                          <div>
                            <span>후속 구축 계획</span>
                            <strong>{row.nextPlan}</strong>
                            <p>{row.judgeNote}</p>
                          </div>
                        </div>
                        <div className={styles.techDetailGrid}>
                          <span>provider key: {row.providerCode}</span>
                          <span>source: {row.sourceType || '-'}</span>
                          <span>quality: {row.sourceQualityStatus || '-'}</span>
                          <span>API 확인: {apiProof.primary} · {apiProof.secondary}</span>
                          <span>hash: {shortHash(row.rawResponseSha256)}</span>
                          <span>schema: {schemaValidationLabel(row.schemaValidationStatus)}</span>
                          <span>메모: {providerReasonLabel(row)}</span>
                        </div>
                      </td>
                    </tr>
                  )}
                </Fragment>
              )
            })}
          </tbody>
        </table>
      </div>
    </section>
  )
}

function filterProofProviders(providers, filter) {
  providers = providers.filter((provider) => !INACTIVE_PROVIDER_CODES.has(provider.providerCode))
  if (filter === 'live') return providers.filter((provider) => provider.sourceType === 'live')
  if (filter === 'cache') return providers.filter((provider) => provider.sourceType === 'cache')
  if (filter === 'fallback') return providers.filter((provider) => provider.sourceType === 'fallback')
  if (filter === 'attention') {
    return providers.filter((provider) => provider.sourceType === 'fallback' || provider.sourceType === 'empty' || provider.schemaValidationStatus === 'FAIL')
  }
  return providers
}

function statusClass(tone) {
  if (tone === 'ok') return styles.statusOk
  if (tone === 'cache') return styles.statusCache
  if (tone === 'warn') return styles.statusWarn
  if (tone === 'planned') return styles.statusPlanned
  if (tone === 'reference') return styles.statusReference
  return styles.statusNeutral
}

function groupRunsByDate(runs, records) {
  const recordsByRun = new Map()
  records.forEach((record) => {
    if (!record.runId) return
    recordsByRun.set(record.runId, [...(recordsByRun.get(record.runId) ?? []), record])
  })

  const groups = new Map()
  runs.forEach((run) => {
    const date = formatDate(run.startedAt)
    const group = groups.get(date) ?? {
      date,
      productCount: 0,
      providers: new Set(),
      liveCount: 0,
      cacheCount: 0,
      fallbackCount: 0,
      externalCount: 0,
      diagnosisUsable: false,
    }
    group.productCount += 1
    group.liveCount += run.successCount ?? 0
    group.cacheCount += run.cacheHitCount ?? 0
    group.fallbackCount += run.fallbackCount ?? 0
    group.diagnosisUsable = group.diagnosisUsable || Boolean(run.diagnosisUsable)
    const runRecords = recordsByRun.get(run.runId) ?? []
    runRecords.forEach((record) => {
      if (record.provider) group.providers.add(record.provider)
      if (isExternalRecord(record) && !record.fallbackUsed) group.externalCount += 1
    })
    groups.set(date, group)
  })

  return Array.from(groups.values()).map((group) => ({
    ...group,
    providers: Array.from(group.providers).sort(),
  }))
}

function isExternalRecord(record) {
  return record.provider === 'EXPORT_EVIDENCE_FEED' || record.dataFamily === 'EXTERNAL_EVIDENCE'
}

function shortHash(value) {
  if (!value) return '-'
  const hash = String(value).replace('sha256:', '')
  return `sha256:${hash.slice(0, 10)}...`
}

function impactClass(tone) {
  if (tone === 'positive') return styles.impactPositive
  if (tone === 'negative') return styles.impactNegative
  if (tone === 'warning') return styles.impactWarning
  return styles.impactNeutral
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace('Z', '').slice(0, 16)
}
