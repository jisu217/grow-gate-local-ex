export default function SourceTrustBar({ summary }) {
  if (!summary) return null
  const collectionRunIds = Array.isArray(summary.collectionRunIds) ? summary.collectionRunIds : []
  const sourceIds = Array.isArray(summary.sourceIds) ? summary.sourceIds : []
  const liveCount = Number(summary.liveCount ?? 0)
  const cacheCount = Number(summary.cacheCount ?? 0)
  const fallbackCount = Number(summary.fallbackCount ?? 0)
  const crawlCount = Number(summary.crawlCount ?? 0)

  return (
    <section className="source-trust-bar" aria-label="Source trust summary">
      <div className="source-trust-main">
        <div>
          <span className="source-trust-kicker">Source Trust</span>
          <strong>{summary.diagnosisId || 'diagnosis'}</strong>
          <span>{summary.createdAt || 'createdAt 확인 대기'}</span>
        </div>
        <p>{summary.motieRelatedSummary || '산업부 관련 contribution 확인 대기'}</p>
      </div>
      <div className="source-trust-counts">
        <TrustCount label="Live" value={liveCount} tone="ok" />
        <TrustCount label="Cache" value={cacheCount} tone="ok" />
        <TrustCount label="Crawl" value={crawlCount} tone="warn" />
        <TrustCount label="Fallback" value={fallbackCount} tone={fallbackCount > 0 ? 'req' : 'ok'} />
      </div>
      <div className="source-trust-meta">
        <span>run {collectionRunIds[0] || '-'}</span>
        <span>{sourceIds.length} source ids</span>
        <span>MOTIE {Number(summary.motieRelatedContribution ?? 0) > 0 ? '+' : ''}{summary.motieRelatedContribution ?? 0}</span>
      </div>
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
