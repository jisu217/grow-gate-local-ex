import { humanProviderName, humanStatusLabel } from './sourceEvidencePresenter.js'

export default function SourceTrustBar({ summary, onRetry, retryStatus }) {
  if (!summary) return null
  const collectionRunIds = Array.isArray(summary.collectionRunIds) ? summary.collectionRunIds : []
  const sourceIds = Array.isArray(summary.sourceIds) ? summary.sourceIds : []
  const providerHealth = Array.isArray(summary.providerHealth) ? summary.providerHealth : []
  const liveCount = Number(summary.liveCount ?? 0)
  const cacheCount = Number(summary.cacheCount ?? 0)
  const fallbackCount = Number(summary.fallbackCount ?? 0)
  const emptyCount = Number(summary.emptyCount ?? 0)
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
      {seedCount > 0 && (
        <div className="source-trust-warning seed">
          synthetic_demo_not_real_customer Seed {seedCount}건은 시연용 합성 근거이며 live 공공 API proof로 계산하지 않습니다.
        </div>
      )}
      <div className="source-trust-layout">
        <div className="source-trust-main">
          <div>
            <span className="source-trust-kicker">근거 신뢰 상태</span>
            <strong>{summary.diagnosisId || 'diagnosis'}</strong>
            <span>{summary.createdAt || 'createdAt 확인 대기'}</span>
          </div>
          <p>{summary.motieRelatedSummary || '산업부 관련 contribution 확인 대기'}</p>
          <p>최신 수집 {summary.lastCollectedAt || '-'} · 원천 기간 {summary.sourcePeriod || '확인 대기'} · 수집 성공률 {Math.round(liveSuccessRate * 100)}%</p>
        </div>
        <div className="source-trust-counts">
          <TrustCount label="실시간 성공" value={liveCount} tone="ok" />
          <TrustCount label="저장 데이터" value={cacheCount} tone="ok" />
          <TrustCount label="공개자료 참고" value={externalFeedCount} tone="warn" />
          <TrustCount label="페이지 수집" value={crawlCount} tone="warn" />
          <TrustCount label="API 설정 필요" value={emptyCount} tone={emptyCount > 0 ? 'warn' : 'ok'} />
          <TrustCount label="대체 근거" value={fallbackCount} tone={fallbackCount > 0 ? 'req' : 'ok'} />
          {seedCount > 0 && <TrustCount label="데모 데이터" value={seedCount} tone="req" />}
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
        <span>{sourceIds.length}개 출처 ID</span>
        <span>MOTIE {Number(summary.motieRelatedContribution ?? 0) > 0 ? '+' : ''}{summary.motieRelatedContribution ?? 0}</span>
      </div>
      {providerHealth.length > 0 && (
        <div className="provider-health-strip">
          {providerHealth.slice(0, 6).map((provider) => (
            <div key={provider.provider} className={`provider-health ${provider.status}`}>
              <strong>{humanProviderName(provider.provider)}</strong>
              <span>L{provider.liveCount ?? 0}/C{provider.cacheCount ?? 0}/E{provider.emptyCount ?? 0}/F{provider.fallbackCount ?? 0}</span>
              <span>{provider.sourceQualityStatus ? humanStatusLabel(provider.sourceQualityStatus) : provider.lastCollectedAt || '-'}</span>
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
