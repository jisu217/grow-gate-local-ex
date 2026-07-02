import { useEffect, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import styles from './DiagnosisSources.module.css'
import { getDiagnosisSourceEntries } from '../../services/diagnosisService.js'
import { getPublicDataDashboard } from '../../services/publicDataService.js'

// 진단 근거 출처 페이지 컴포넌트
export default function DiagnosisSources() {

  const { id } = useParams()
  const navigate = useNavigate()
  const [sources, setSources] = useState([])
  const [dashboard, setDashboard] = useState(null)
  const [loadError, setLoadError] = useState('')

  useEffect(() => {
    let cancelled = false
    async function load() {
      try {
        const [sourceEntries, dashboardData] = await Promise.all([
          getDiagnosisSourceEntries(id),
          getPublicDataDashboard(),
        ])
        if (cancelled) return
        setSources(sourceEntries)
        setDashboard(dashboardData)
        setLoadError('')
      } catch (err) {
        if (!cancelled) setLoadError(err.message || '출처 데이터를 불러오지 못했습니다.')
      }
    }
    load()
    return () => { cancelled = true }
  }, [id])

  const publicSources = sources.filter((source) => ['live', 'cache', 'fallback'].includes(source.sourceType))
  const ruleSources = sources.filter((source) => source.sourceType === 'heuristic')
  const traceSources = sources.filter((source) => source.sourceType === 'agent')

  return (
    <div className={styles.wrap}>
      <button className={styles.back} onClick={() => navigate(`/diagnosis/${id}`)}>
        ← 진단 결과로 돌아가기
      </button>
      <h1 className={styles.title}>진단 근거 및 출처</h1>
      <p className={styles.sub}>진단 ID: {id}</p>

      {loadError && <div className={styles.error}>{loadError}</div>}

      {dashboard && (
        <div className={styles.dashboard}>
          <div className={styles.metric}>
            <span>Fallback ratio</span>
            <b>{Math.round((dashboard.fallbackRatio ?? 0) * 100)}%</b>
          </div>
          <div className={styles.metric}>
            <span>Average confidence</span>
            <b>{Math.round((dashboard.averageConfidence ?? 0) * 100)}%</b>
          </div>
          <div className={styles.metric}>
            <span>MOTIE-related records</span>
            <b>{dashboard.motieRelatedRecordCount ?? 0}</b>
          </div>
          <div className={styles.metric}>
            <span>Newest fetched</span>
            <b>{formatDate(dashboard.newestFetchedAt)}</b>
          </div>
        </div>
      )}

      <SourceSection title="공공데이터 근거" items={publicSources} />
      <SourceSection title="규칙 엔진 근거" items={ruleSources} />
      <SourceSection title="Workflow trace" items={traceSources} />

      {dashboard?.providers?.length > 0 && (
        <div className={styles.section}>
          <div className={styles.sectionTitle}>Provider freshness dashboard</div>
          {dashboard.providers.map((provider) => (
            <div key={provider.provider} className={styles.providerRow}>
              <div>
                <div className={styles.itemTitle}>{provider.provider} · {provider.agency}</div>
                <div className={styles.meta}>
                  {provider.dataFamily} · {provider.usedFor} · refresh {provider.refreshCycleHours}h
                </div>
              </div>
              <div className={styles.rightMeta}>
                <span>{provider.recordCount} records</span>
                <span>{Math.round((provider.fallbackRatio ?? 0) * 100)}% fallback</span>
                <span>{provider.operationEndpointSeen ? 'operation endpoint' : 'no operation evidence'}</span>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function SourceSection({ title, items }) {
  return (
    <div className={styles.section}>
      <div className={styles.sectionTitle}>{title}</div>
      {items.length === 0 && <div className={styles.empty}>표시할 출처가 없습니다.</div>}
      {items.map((item) => (
        <div key={`${item.sourceId}-${item.recordId}`} className={styles.item}>
          <div>
            <span className={styles.itemTitle}>{item.provider || item.datasetName}</span>
            <div className={styles.meta}>
              {item.sourceType} · {item.usedFor} · confidence {Math.round((item.confidence ?? 0) * 100)}%
              {item.motieRelated ? ' · 산업부/산하기관' : ' · 타 기관'}
            </div>
            {item.scoreFormula && <div className={styles.formula}>{item.scoreFormula}</div>}
          </div>
          <span className={styles.itemDate}>
            {item.scoreContribution > 0 ? '+' : ''}{item.scoreContribution ?? 0}점
          </span>
        </div>
      ))}
    </div>
  )
}

function formatDate(value) {
  if (!value) return '-'
  return String(value).slice(0, 10)
}
