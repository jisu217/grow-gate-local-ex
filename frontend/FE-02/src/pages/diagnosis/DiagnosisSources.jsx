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

  const publicSources = sources.filter((source) => ['live', 'cache', 'fallback', 'seed', 'empty'].includes(source.sourceType))
  const reconstructedSources = sources.filter((source) => ['crawl', 'external_feed', 'external_reconstructed'].includes(source.sourceType))
  const ruleSources = sources.filter((source) => source.sourceType === 'heuristic')
  const traceSources = sources.filter((source) => source.sourceType === 'agent')

  return (
    <div className={styles.wrap}>
      <button className={styles.back} onClick={() => navigate(`/diagnosis/${id}`)}>
        ← 진단 결과로 돌아가기
      </button>
      <h1 className={styles.title}>진단 근거 및 출처</h1>
      <p className={styles.sub}>진단 ID: {id}</p>
      <div className={styles.notice}>
        sourceType은 live(실제 API), cache(이전 수집), fallback(장애/미설정 대체), seed(데모 데이터), empty(조회 결과 없음)로 구분합니다. 이용조건이 미확인인 제공자는 원문 링크 제공을 우선하고 요약/재가공/재배포 제한 가능성을 보수적으로 표시합니다.
      </div>

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
      <SourceSection title="재구성 보조근거" items={reconstructedSources} />
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
            <div className={styles.proofGrid}>
              <span className={proofClass(item.operationEndpoint)}>operation endpoint {item.operationEndpoint ? 'yes' : 'no'}</span>
              <span className={proofClass(item.apiKeyConfigured)}>API key {item.apiKeyConfigured ? 'configured' : 'missing'}</span>
              <span>{item.endpointType || item.collectionEvidence?.endpointType || 'endpoint unknown'}</span>
              <span>fetched {formatDateTime(item.fetchedAt || item.retrievedAt)}</span>
            </div>
            {isAuxiliarySource(item.sourceType) && (
              <div className={styles.notice}>보조근거입니다. 공식 공공 API live 증거 수에는 포함하지 않습니다.</div>
            )}
            {(item.licenseName || item.usageWarning || item.officialUrl || item.termsUrl) && (
              <div className={styles.usageBox}>
                <span>license: {item.licenseName || '미확인'}</span>
                <span>storage {item.storageAllowed || 'unknown'} · reprocessing {item.reprocessingAllowed || 'unknown'} · redistribution {item.redistributionAllowed || 'unknown'} · attribution {item.attributionRequired || 'unknown'}</span>
                <span>{item.usageWarning || '공식 이용조건 확인 전 재배포 금지'}</span>
                <span>verified {item.licenseLastVerifiedAt || '미확인'} · <SourceLink href={item.officialUrl}>official</SourceLink> · <SourceLink href={item.termsUrl}>terms</SourceLink></span>
              </div>
            )}
            {(item.metricSummary || item.decisionImpact) && (
              <div className={styles.formula}>
                {item.metricSummary ? `metric: ${item.metricSummary}` : ''}
                {item.metricSummary && item.decisionImpact ? ' · ' : ''}
                {item.decisionImpact ? `impact: ${item.decisionImpact}` : ''}
              </div>
            )}
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

function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ').replace('Z', '').slice(0, 16)
}

function proofClass(value) {
  return value ? styles.proofOk : styles.proofWarn
}

function isAuxiliarySource(sourceType) {
  return ['crawl', 'external_feed', 'external_reconstructed', 'heuristic', 'agent'].includes(sourceType)
}

function SourceLink({ href, children }) {
  if (!href) return <span>{children} 없음</span>
  return <a href={href} target="_blank" rel="noreferrer">{children}</a>
}
