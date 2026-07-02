export default function SourceTrustBar({ summary, onRetry, retryStatus }) {
  if (!summary) return null
  const collectionRunIds = Array.isArray(summary.collectionRunIds) ? summary.collectionRunIds : []
  const sourceIds = Array.isArray(summary.sourceIds) ? summary.sourceIds : []
  const providerHealth = Array.isArray(summary.providerHealth) ? summary.providerHealth : []
  const liveCount = Number(summary.liveCount ?? 0)
  const cacheCount = Number(summary.cacheCount ?? 0)
  const fallbackCount = Number(summary.fallbackCount ?? 0)
  const crawlCount = Number(summary.crawlCount ?? 0)
  const externalFeedCount = Number(summary.externalFeedCount ?? 0)
  const seedCount = Number(summary.seedCount ?? 0)
  const liveSuccessRate = Number(summary.liveSuccessRate ?? 0)
  const retryPayload = summary.retryPayload ?? {}
  const canRetry = retryPayload.hsCode && retryPayload.targetCountry && retryPayload.productName

  return (
    <section className={`source-trust-bar ${liveCount + cacheCount === 0 ? 'needs-review' : ''}`} aria-label="Source trust summary">
      {liveCount + cacheCount === 0 && (
        <div className="source-trust-warning">
          live/cache 공공데이터가 0건입니다. 현재 Gate는 fallback/규칙/보조 근거 기반이므로 담당자 검토와 재수집이 필요합니다.
        </div>
      )}
      <div className="source-trust-layout">
        <div className="source-trust-main">
          <div>
            <span className="source-trust-kicker">Source Trust</span>
            <strong>{summary.diagnosisId || 'diagnosis'}</strong>
            <span>{summary.createdAt || 'createdAt 확인 대기'}</span>
          </div>
          <p>{summary.motieRelatedSummary || '산업부 관련 contribution 확인 대기'}</p>
          <p>최신 수집 {summary.lastCollectedAt || '-'} · 원천 기간 {summary.sourcePeriod || '확인 대기'} · live success {Math.round(liveSuccessRate * 100)}%</p>
        </div>
        <div className="source-trust-counts">
          <TrustCount label="Live" value={liveCount} tone="ok" />
          <TrustCount label="Cache" value={cacheCount} tone="ok" />
          <TrustCount label="Feed" value={externalFeedCount} tone="warn" />
          <TrustCount label="Crawl" value={crawlCount} tone="warn" />
          <TrustCount label="Fallback" value={fallbackCount} tone={fallbackCount > 0 ? 'req' : 'ok'} />
          {seedCount > 0 && <TrustCount label="Seed" value={seedCount} tone="req" />}
        </div>
        <div className="source-trust-actions">
          <button type="button" className="secondary-btn" onClick={onRetry} disabled={!canRetry || retryStatus === 'running'}>
            {retryStatus === 'running' ? '재수집 중' : '공공데이터 재수집'}
          </button>
          {retryStatus && retryStatus !== 'running' && <span>{retryStatus}</span>}
        </div>
      </div>
      <div className="source-trust-meta">
        <span>run {collectionRunIds[0] || '-'}</span>
        <span>{sourceIds.length} source ids</span>
        <span>MOTIE {Number(summary.motieRelatedContribution ?? 0) > 0 ? '+' : ''}{summary.motieRelatedContribution ?? 0}</span>
      </div>
      {providerHealth.length > 0 && (
        <div className="provider-health-strip">
          {providerHealth.slice(0, 6).map((provider) => (
            <div key={provider.provider} className={`provider-health ${provider.status}`}>
              <strong>{provider.provider}</strong>
              <span>L{provider.liveCount ?? 0}/C{provider.cacheCount ?? 0}/F{provider.fallbackCount ?? 0}</span>
              <span>{provider.lastCollectedAt || '-'}</span>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function TrustCount({ label, value, tone }) {
  return (
    <div className={`source-trust-count ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}
