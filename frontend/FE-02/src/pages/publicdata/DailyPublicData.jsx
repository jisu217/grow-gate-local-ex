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
  const [copiedKey, setCopiedKey] = useState('')

  const load = async () => {
    setPageState('loading')
    try {
      const [dashboardData, runData, recordData] = await Promise.all([
        getPublicDataDashboard(),
        getPublicDataRuns(),
        getPublicDataRecords(),
      ])
      setDashboard(dashboardData)
      setRuns(runData?.runs ?? [])
      setRecords(recordData?.records ?? [])
      try {
        const proofData = await getLatestPublicDataProofRun()
        setProofRun(proofData)
        setProofState('ready')
        setProofError('')
      } catch (error) {
        setProofRun(null)
        setProofState('error')
        setProofError(error.message || 'proof run 조회에 실패했습니다.')
      }
      setPageState('ok')
    } catch {
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
  const failureReasons = collectFailureReasons(runs, records).slice(0, 6)

  const liveCount = dashboard?.successCount ?? 0
  const cacheCount = dashboard?.cacheHitCount ?? 0
  const fallbackCount = dashboard?.fallbackCount ?? 0
  const missingKeyCount = new Set(
    records.filter((record) => record.apiKeyConfigured === false).map((record) => record.provider),
  ).size
  const proofStatusValue = proofRun?.proofStatus ?? proofRun?.proof_status ?? proofState
  const proofRunIdValue = proofRun?.proofRunId ?? proofRun?.proof_run_id ?? '-'
  const schemaPass = proofRun?.schemaPassCount ?? proofRun?.schema_pass_count ?? 0
  const schemaFail = proofRun?.schemaFailCount ?? proofRun?.schema_fail_count ?? 0
  const maxGuarded = Math.max(
    1,
    ...(proofRun?.scoreWaterfall ?? []).map((row) => Math.abs(row.guardedDelta ?? 0)),
  )

  const banner = buildBanner({ liveCount, fallbackCount, missingKeyCount, diagnosisUsable: latestRun?.diagnosisUsable })

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

  const handleCopyEvidence = (key, value) => {
    const text = Array.isArray(value) ? value.join(', ') : String(value ?? '').replace(/[\[\]"]/g, '')
    if (!text || text === '-') return
    if (navigator?.clipboard?.writeText) navigator.clipboard.writeText(text)
    setCopiedKey(key)
    setTimeout(() => setCopiedKey((current) => (current === key ? '' : current)), 1200)
  }

  return (
    <main className={styles.wrap}>
      <div className={styles.head}>
        <div>
          <h1 className={styles.title}>일일 공공데이터 수집</h1>
          <p className={styles.sub}>매일 08:00 KST · 최근 활성 제품 최대 30개 · 공공데이터 + 허용된 RSS/API feed</p>
        </div>
        {canRunManualCollection && (
          <div className={styles.actions}>
            <button className={styles.secondaryBtn} onClick={handleProofRun} disabled={proofState === 'running'}>
              {proofState === 'running' ? 'Proof 생성 중' : 'Proof run 생성'}
            </button>
            <button className={styles.primaryBtn} onClick={handleRefresh} disabled={refreshState === 'running'}>
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
          {banner && (
            <div className={`${styles.banner} ${banner.tone === 'ok' ? styles.bannerOk : styles.bannerWarn}`} role="status">
              <span className={styles.bannerDot} aria-hidden="true" />
              <div>
                <b>{banner.title}</b>
                <span className={styles.bannerSub}>{banner.message}</span>
              </div>
            </div>
          )}

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

          {/* 1. 수집 현황 */}
          <section className={styles.block}>
            <p className={styles.blockLabel}>
              수집 현황
              <span className={styles.blockMeta}>· {formatDateTime(latestRun?.startedAt ?? dashboard?.newestFetchedAt)}</span>
            </p>
            <div className={styles.metrics}>
              <Metric label="성공" value={`${liveCount}`} unit="건 live" tone="ok" />
              <Metric label="캐시" value={`${cacheCount}`} unit="건 cache" tone="accent" />
              <Metric label="실패" value={`${fallbackCount}`} unit="건 fallback" tone="warn" />
              <Metric label="외부 데이터" value={`${externalCount}`} unit="건 RSS/API" />
              <Metric label="최근 수집 대상" value={`${runs.length}`} unit="개" />
              <Metric label="사용 가능 여부" value={latestRun?.availabilityLabel ?? '확인 필요'} tone={latestRun?.diagnosisUsable ? 'ok' : 'warn'} />
              <Metric label="호출 provider" value={`${providerNames.length}`} unit="개" detail={providerNames.join(', ')} />
            </div>

            <div className={styles.card}>
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
                        <td data-label="수집일">{group.date}</td>
                        <td data-label="전체 수집 대상">제품 {group.productCount}개</td>
                        <td data-label="호출 provider">{group.providers.join(', ') || '-'}</td>
                        <td data-label="성공" className={styles.ok}>live {group.liveCount}건</td>
                        <td data-label="캐시">cache {group.cacheCount}건</td>
                        <td data-label="실패" className={group.fallbackCount > 0 ? styles.warnText : ''}>fallback {group.fallbackCount}건</td>
                        <td data-label="외부 데이터">RSS/API {group.externalCount}건</td>
                        <td data-label="사용 가능 여부">
                          <span className={`${styles.pill} ${group.diagnosisUsable ? styles.pillOk : styles.pillWarn}`}>
                            {group.diagnosisUsable ? '진단 반영 가능' : '확인 필요'}
                          </span>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          {/* 2. Proof 검증 */}
          <section className={styles.block}>
            <p className={styles.blockLabel}>수집 증거 Proof</p>
            <div className={`${styles.card} ${styles.proofCard}`}>
              <div className={styles.proofHead}>
                <div className={styles.proofHeadLeft}>
                  <span className={`${styles.pill} ${proofStatusValue === 'PASS' ? styles.pillOk : styles.pillDanger}`}>
                    {proofStatusValue}
                  </span>
                  <span className={styles.proofId}>{proofRunIdValue}</span>
                </div>
                <div className={styles.proofMeta}>
                  schema pass {schemaPass} · <span className={schemaFail > 0 ? styles.warnText : ''}>fail {schemaFail}</span>
                </div>
              </div>
              <p className={styles.policy}>
                {proofRun?.judgeProofPolicy ?? 'raw hash와 schema validation으로 live/cache proof를 확인합니다.'}
              </p>

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
                        <td data-label="Provider">{provider.providerCode}</td>
                        <td data-label="Source">{provider.sourceType || '-'}</td>
                        <td data-label="API proof">
                          <div className={styles.badges}>
                            <span className={`${styles.pill} ${provider.operationEndpoint ? styles.pillOk : styles.pillWarn}`}>
                              endpoint {provider.operationEndpoint ? 'yes' : 'no'}
                            </span>
                            <span className={`${styles.pill} ${provider.apiKeyConfigured ? styles.pillOk : styles.pillDanger}`}>
                              key {provider.apiKeyConfigured ? 'set' : 'missing'}
                            </span>
                          </div>
                        </td>
                        <td data-label="Records">{provider.recordCount ?? 0}</td>
                        <td data-label="Raw response hash">{shortHash(provider.rawResponseSha256)}</td>
                        <td data-label="Schema validation" className={provider.schemaValidationStatus === 'PASS' ? styles.ok : styles.warnText}>
                          {provider.schemaValidationStatus || '-'}
                        </td>
                        <td data-label="Fallback reason">{provider.fallbackReason || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </section>

          {/* 3. 진단 사용 연결 */}
          <section className={styles.block}>
            <p className={styles.blockLabel}>진단 사용 연결</p>
            <div className={styles.card}>
              <div className={styles.tableWrap}>
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>Diagnosis</th>
                      <th>Provider</th>
                      <th>Dimension</th>
                      <th>Source</th>
                      <th>Δ 점수</th>
                      <th>Guarded</th>
                      <th>Evidence</th>
                    </tr>
                  </thead>
                  <tbody>
                    {(!Array.isArray(proofRun?.scoreWaterfall) || proofRun.scoreWaterfall.length === 0) && (
                      <tr><td colSpan="7" className={styles.empty}>진단 연결 row가 없습니다. 사전 수집 cache run은 진단 실행 시점에 연결됩니다.</td></tr>
                    )}
                    {proofRun?.scoreWaterfall?.slice(0, 12).map((row, index) => {
                      const rowKey = `${row.diagnosisId || 'cache'}-${row.providerCode}-${row.dimension}-${index}`
                      const guarded = row.guardedDelta ?? 0
                      return (
                        <tr key={rowKey}>
                          <td data-label="Diagnosis">{row.diagnosisId || 'daily cache'}</td>
                          <td data-label="Provider">{row.providerCode}</td>
                          <td data-label="Dimension">{row.dimension || '-'}</td>
                          <td data-label="Source">{row.sourceType || '-'}</td>
                          <td data-label="Δ 점수">{row.publicDataDelta ?? 0}</td>
                          <td data-label="Guarded">
                            <div className={styles.delta}>
                              <span className={styles.deltaNum}>{guarded > 0 ? `+${guarded}` : guarded}</span>
                              <span
                                className={`${styles.bar} ${guarded < 0 ? styles.barDown : guarded > 0 ? styles.barUp : ''}`}
                                style={{ width: `${deltaWidth(guarded, maxGuarded)}%` }}
                              />
                            </div>
                          </td>
                          <td data-label="Evidence">
                            <button
                              type="button"
                              className={styles.ev}
                              onClick={() => handleCopyEvidence(rowKey, row.evidenceRecordIds)}
                              title="클릭 시 전체 Evidence ID 복사"
                            >
                              {copiedKey === rowKey ? '복사됨 ✓' : compactEvidenceIds(row.evidenceRecordIds)}
                            </button>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            </div>
            <div className={styles.legend}>
              <span>Δ 점수 · Guarded delta는 가드레일 적용 후 실제 반영값</span>
              <span>Evidence ID 클릭 시 전체 복사</span>
            </div>
          </section>

          {/* 4. 실패 사유 */}
          <section className={styles.block}>
            <p className={styles.blockLabel}>
              실패 사유
              <span className={styles.blockMeta}>· API key · operation endpoint · timeout 추적</span>
            </p>
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

function Metric({ label, value, unit, detail, tone }) {
  return (
    <div className={styles.metric}>
      <span className={styles.metricLabel}>{label}</span>
      <b className={`${styles.metricValue} ${tone === 'ok' ? styles.ok : tone === 'warn' ? styles.warnText : tone === 'accent' ? styles.accent : ''}`}>
        {value}
        {unit && <em className={styles.metricUnit}> {unit}</em>}
      </b>
      {detail && <small className={styles.metricDetail}>{detail}</small>}
    </div>
  )
}

function buildBanner({ liveCount, fallbackCount, missingKeyCount, diagnosisUsable }) {
  if (liveCount === 0) {
    const keyPart = missingKeyCount > 0 ? ` · provider ${missingKeyCount}개 API 키 미설정` : ''
    return {
      tone: 'warn',
      title: `오늘 라이브 수집 ${liveCount}건${keyPart}`,
      message: diagnosisUsable
        ? '캐시 데이터로 진단은 정상 반영됩니다. 라이브 전환하려면 provider별 API 키를 등록하세요.'
        : '라이브·캐시 모두 부족합니다. provider 설정과 수동 재수집을 확인하세요.',
    }
  }
  if (fallbackCount > 0) {
    return {
      tone: 'warn',
      title: `라이브 수집 ${liveCount}건 · fallback ${fallbackCount}건`,
      message: '일부 provider가 fallback으로 처리됐습니다. 아래 실패 사유를 확인하세요.',
    }
  }
  return {
    tone: 'ok',
    title: `라이브 수집 ${liveCount}건 정상`,
    message: '모든 provider가 라이브로 수집됐습니다. 진단에 최신 데이터가 반영됩니다.',
  }
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

function deltaWidth(value, max) {
  const denom = Math.max(1, max)
  return Math.min(100, Math.round((Math.abs(value) / denom) * 100))
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
