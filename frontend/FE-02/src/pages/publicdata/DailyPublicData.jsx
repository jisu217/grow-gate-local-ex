import { useEffect, useMemo, useState } from 'react'
import styles from './DailyPublicData.module.css'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import {
  createPublicDataProofRun,
  getLatestPublicDataProofRun,
  getPublicDataDashboard,
  getPublicDataRecords,
  getPublicDataRuns,
  refreshDailyPublicData,
} from '../../services/publicDataService.js'
import { getAuthUser } from '../../services/authService.js'

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
  const [proofError, setProofError] = useState('')

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

    setDashboard(dashboardData ?? proofDashboard(proofData))
    setRuns(runData?.runs ?? [])
    setRecords(recordData?.records ?? [])

    if (proofData) {
      setProofRun(proofData)
      setProofState('ready')
      setProofError('')
    } else {
      setProofRun(null)
      setProofState('error')
      setProofError(errorMessage(proofResult) || 'proof run 조회에 실패했습니다.')
    }

    if (dashboardData || runData || recordData || proofData) {
      setPageState('ok')
    } else {
      setPageState('error')
    }
  }

  useEffect(() => {
    load()
  }, [])

  const dailyGroups = useMemo(() => groupRunsByDate(runs, records), [runs, records])
  const latestRun = runs[0]
  const providerNames = (dashboard?.providers ?? []).map((provider) => provider.provider)
  const externalCount = records.filter((record) => isExternalRecord(record) && !record.fallbackUsed).length
  const failureReasons = collectFailureReasons(runs, records).slice(0, 4)

  const handleRefresh = async () => {
    setRefreshState('running')
    setRefreshResult(null)
    try {
      const result = await refreshDailyPublicData()
      setRefreshResult(result)
      await load()
      setRefreshState('done')
    } catch {
      setRefreshState('error')
    }
  }

  const handleProofRun = async () => {
    setProofState('running')
    setProofError('')
    try {
      const next = await createPublicDataProofRun('daily-refresh-screen')
      setProofRun(next)
      setProofState('done')
    } catch (error) {
      setProofError(error.message || 'proof run 생성에 실패했습니다.')
      setProofState('error')
    }
  }

  return (
    <main className={styles.wrap}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>아침 자동 수집</h1>
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
      {pageState === 'error' && (
        <StatusMessage
          type="error"
          message="수집 로그를 불러오지 못했습니다. 네트워크를 확인하고 다시 시도해주세요."
          onAction={load}
        />
      )}

      {pageState === 'ok' && (
        <>
          {refreshState === 'error' && (
            <div className={styles.warn}>수동 재수집 요청에 실패했습니다. API key, endpoint, timeout 설정을 확인하세요.</div>
          )}
          {refreshResult && (
            <div className={styles.notice}>
              수동 재수집 완료: 대상 {refreshResult.plannedCount ?? 0}개, live {refreshResult.successCount ?? 0}건,
              cache {refreshResult.cacheHitCount ?? 0}건, fallback {refreshResult.fallbackCount ?? 0}건
            </div>
          )}
          {proofError && <div className={styles.warn}>{proofError}</div>}

          <section className={styles.summaryGrid} aria-label="daily refresh summary">
            <Metric label="수집일" value={formatDateTime(latestRun?.startedAt ?? dashboard?.newestFetchedAt)} />
            <Metric label="최근 수집 대상" value={`${runs.length}개`} />
            <Metric label="성공" value={`live ${dashboard?.successCount ?? 0}건`} tone="ok" />
            <Metric label="캐시" value={`cache ${dashboard?.cacheHitCount ?? 0}건`} />
            <Metric label="실패" value={`fallback ${dashboard?.fallbackCount ?? 0}건`} tone="warn" />
            <Metric label="외부 데이터" value={`RSS/API ${externalCount}건`} />
            <Metric label="사용 가능 여부" value={latestRun?.availabilityLabel ?? '확인 필요'} tone={latestRun?.diagnosisUsable ? 'ok' : 'warn'} />
            <Metric label="호출 provider" value={`${providerNames.length}개`} detail={providerNames.join(', ')} />
          </section>

          <section className={styles.section}>
            <div className={styles.sectionHead}>
              <h2>수집 증거 Proof</h2>
              <span>{proofRun?.judgeProofPolicy ?? 'raw hash와 schema validation으로 live/cache proof를 확인합니다.'}</span>
            </div>
            <div className={styles.proofGrid}>
              <Metric label="proofRunId" value={proofRun?.proofRunId ?? proofRun?.proof_run_id ?? '-'} />
              <Metric label="proofStatus" value={proofRun?.proofStatus ?? proofRun?.proof_status ?? proofState} tone={proofRun?.proofStatus === 'PASS' ? 'ok' : 'warn'} />
              <Metric label="schema pass" value={`${proofRun?.schemaPassCount ?? proofRun?.schema_pass_count ?? 0}건`} tone="ok" />
              <Metric label="schema fail" value={`${proofRun?.schemaFailCount ?? proofRun?.schema_fail_count ?? 0}건`} tone={(proofRun?.schemaFailCount ?? proofRun?.schema_fail_count ?? 0) > 0 ? 'warn' : undefined} />
            </div>

            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Provider</th>
                    <th>Source</th>
                    <th>API proof</th>
                    <th>Records</th>
                    <th>Raw response hash</th>
                    <th>Schema validation</th>
                    <th>Fallback reason</th>
                  </tr>
                </thead>
                <tbody>
                  {(!Array.isArray(proofRun?.providers) || proofRun.providers.length === 0) && (
                    <tr><td colSpan="7" className={styles.empty}>아직 proof provider row가 없습니다. Proof run을 생성하세요.</td></tr>
                  )}
                  {proofRun?.providers?.slice(0, 12).map((provider) => (
                    <tr key={`${provider.providerCode}-${provider.rawResponseSha256 || provider.fallbackReason || provider.fetchedAt}`}>
                      <td>{provider.providerCode}</td>
                      <td>{provider.sourceType || '-'}</td>
                      <td>
                        endpoint {provider.operationEndpoint ? 'yes' : 'no'}
                        <span className={styles.inlineMeta}>key {provider.apiKeyConfigured ? 'set' : 'missing'}</span>
                      </td>
                      <td>{provider.recordCount ?? 0}</td>
                      <td>{shortHash(provider.rawResponseSha256)}</td>
                      <td className={provider.schemaValidationStatus === 'PASS' ? styles.ok : styles.warnText}>
                        {provider.schemaValidationStatus || '-'}
                      </td>
                      <td>{provider.fallbackReason || '-'}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className={styles.section}>
            <div className={styles.sectionHead}>
              <h2>진단 사용 연결</h2>
              <span>diagnosisId와 evidence record ID 단위로 점수 반영 경로를 확인합니다.</span>
            </div>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>Diagnosis</th>
                    <th>Provider</th>
                    <th>Dimension</th>
                    <th>Source</th>
                    <th>Delta</th>
                    <th>Guarded delta</th>
                    <th>Evidence records</th>
                  </tr>
                </thead>
                <tbody>
                  {(!Array.isArray(proofRun?.scoreWaterfall) || proofRun.scoreWaterfall.length === 0) && (
                    <tr><td colSpan="7" className={styles.empty}>진단 연결 row가 없습니다. 사전 수집 cache run은 진단 실행 시점에 연결됩니다.</td></tr>
                  )}
                  {proofRun?.scoreWaterfall?.slice(0, 12).map((row, index) => (
                    <tr key={`${row.diagnosisId || 'cache'}-${row.providerCode}-${row.dimension}-${index}`}>
                      <td>{row.diagnosisId || 'daily cache'}</td>
                      <td>{row.providerCode}</td>
                      <td>{row.dimension || '-'}</td>
                      <td>{row.sourceType || '-'}</td>
                      <td>{row.publicDataDelta ?? 0}</td>
                      <td>{row.guardedDelta ?? 0}</td>
                      <td>{compactEvidenceIds(row.evidenceRecordIds)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className={styles.section}>
            <div className={styles.sectionHead}>
              <h2>날짜별 수집 결과</h2>
              <span>최근 run {runs.length}건 기준</span>
            </div>
            <div className={styles.tableWrap}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>수집일</th>
                    <th>전체 수집 대상</th>
                    <th>호출 provider</th>
                    <th>성공</th>
                    <th>캐시</th>
                    <th>실패</th>
                    <th>외부 데이터</th>
                    <th>사용 가능 여부</th>
                  </tr>
                </thead>
                <tbody>
                  {dailyGroups.length === 0 && (
                    <tr><td colSpan="8" className={styles.empty}>아직 수집 run이 없습니다.</td></tr>
                  )}
                  {dailyGroups.map((group) => (
                    <tr key={group.date}>
                      <td>{group.date}</td>
                      <td>제품 {group.productCount}개</td>
                      <td>{group.providers.join(', ') || '-'}</td>
                      <td className={styles.ok}>live {group.liveCount}건</td>
                      <td>cache {group.cacheCount}건</td>
                      <td className={group.fallbackCount > 0 ? styles.warnText : ''}>fallback {group.fallbackCount}건</td>
                      <td>RSS/API {group.externalCount}건</td>
                      <td>
                        <span className={`${styles.badge} ${group.diagnosisUsable ? styles.badgeOk : styles.badgeWarn}`}>
                          {group.diagnosisUsable ? '진단 반영 가능' : '확인 필요'}
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </section>

          <section className={styles.section}>
            <div className={styles.sectionHead}>
              <h2>실패 사유</h2>
              <span>API key, operation endpoint, timeout 추적</span>
            </div>
            <div className={styles.reasonList}>
              {failureReasons.length === 0 && <div className={styles.empty}>최근 실패 사유가 없습니다.</div>}
              {failureReasons.map((reason) => (
                <div key={reason} className={styles.reason}>{reason}</div>
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

function errorMessage(result) {
  return result.status === 'rejected' ? result.reason?.message : ''
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

function collectFailureReasons(runs, records) {
  const reasons = new Set()
  runs.forEach((run) => {
    if (run.errorSummary) reasons.add(run.errorSummary)
  })
  records.forEach((record) => {
    if (record.errorMessage) reasons.add(`${record.provider}: ${record.errorMessage}`)
    if (record.operationEndpoint === false) reasons.add(`${record.provider}: operation endpoint 아님`)
    if (record.apiKeyConfigured === false) reasons.add(`${record.provider}: API key 없음`)
  })
  return Array.from(reasons)
}

function isExternalRecord(record) {
  return record.provider === 'EXPORT_EVIDENCE_FEED' || record.dataFamily === 'EXTERNAL_EVIDENCE'
}

function shortHash(value) {
  if (!value) return '-'
  const hash = String(value).replace('sha256:', '')
  return `sha256:${hash.slice(0, 10)}...`
}

function compactEvidenceIds(value) {
  if (!value) return '-'
  if (Array.isArray(value)) return value.slice(0, 2).join(', ')
  return String(value).replace(/[\[\]"]/g, '').split(',').slice(0, 2).join(', ') || '-'
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace('Z', '').slice(0, 16)
}
