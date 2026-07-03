// S10 데이터 출처 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

export default function S10Sources({ data, sourceTrustSummary, onRetry, retryStatus }) {
  const pipes = Array.isArray(data) ? data : data?.pipes ?? []
  const timeline = Array.isArray(data?.timeline) ? data.timeline : []
  const retrieval = data?.evidenceRetrieval ?? null
  const guardrails = data?.guardrails ?? null
  const ledger = Array.isArray(data?.ledger) ? data.ledger : []
  const topK = Array.isArray(retrieval?.topK) ? retrieval.topK : []
  const providerHealth = Array.isArray(sourceTrustSummary?.providerHealth) ? sourceTrustSummary.providerHealth : []
  const liveCount = Number(sourceTrustSummary?.liveCount ?? 0)
  const cacheCount = Number(sourceTrustSummary?.cacheCount ?? 0)
  const operationEndpointLiveCount = Number(sourceTrustSummary?.operationEndpointLiveCount ?? 0)
  const apiKeyConfiguredLiveCount = Number(sourceTrustSummary?.apiKeyConfiguredLiveCount ?? 0)

  return (
    <DiagnosisStage
      eyebrow="S10 EVIDENCE WORKFLOW"
      title="출처, 근거 검색, 지식화 타임라인"
      description="재수집은 제품/진단 생성 또는 관리자 public-data refresh에서 수행되며, 이 화면은 결과 근거를 읽기 전용으로 검증합니다."
    >

      <div className="pipe-grid">
        {pipes.map((pipe) => {
          const status = pipe.status ?? 'fallback'
          const statusText = status === 'live' ? 'Live'
            : status === 'cache' ? 'Cache'
            : status === 'crawl' ? 'Crawl'
              : status === 'external_feed' ? 'Feed'
                : status === 'seed' ? 'Seed'
              : status === 'heuristic' ? 'Rule'
                : status === 'agent' ? 'Trace'
                  : status === 'user_input' ? 'User'
                    : 'Fallback'
          const chipType = status === 'live' || status === 'cache' ? 'ok'
            : status === 'crawl' || status === 'external_feed' || status === 'heuristic' || status === 'agent' || status === 'user_input' ? 'warn'
              : 'req'
          return (
          <div key={pipe.source} className="pipe-card">
            <div className="pipe-source">{pipe.source}</div>

            <div className="pipe-count">{pipe.count}</div>
            <div className="pipe-lbl">{pipe.label}</div>
            {pipe.metricSummary && (
              <div className="pipe-lbl">metric · {pipe.metricSummary}</div>
            )}

            <span className={`status-chip ${chipType}`}>{statusText}</span>
          </div>
          )
        })}

        <div className="pipe-card" style={{ borderStyle: 'dashed', background: '#fafbfe' }}>
          <div className="pipe-source" style={{ color: 'var(--slate-2)' }}>+ 커넥터 추가</div>
          <div style={{ fontSize: 12, color: 'var(--slate-2)', marginTop: 8, lineHeight: 1.5 }}>
            새 공공데이터 소스를 연결합니다
          </div>
          <span className="status-chip warn">대기중</span>
        </div>
      </div>

      <div className="evidence-workflow-grid">
        <section className="pipe-card" style={{ minHeight: 160 }}>
          <div className="pipe-source">Source-bound evidence retrieval</div>
          <div style={{ fontSize: 12, color: 'var(--slate-2)', lineHeight: 1.55, marginTop: 6 }}>
            {retrieval?.claimBoundary || 'DB 지식과 source trace 안에서만 근거 chunk를 찾습니다.'}
          </div>
          <div style={{ display: 'grid', gap: 8, marginTop: 12 }}>
            {topK.length === 0 ? (
              <div className="pipe-lbl">top-k 근거 chunk 없음</div>
            ) : topK.slice(0, 4).map((chunk) => (
              <div key={chunk.chunkId} style={{ borderTop: '1px solid var(--line)', paddingTop: 8 }}>
                <div style={{ fontSize: 12, fontWeight: 700 }}>{chunk.displayRole || chunk.category}</div>
                <div className="pipe-lbl">{chunk.sourceId || chunk.chunkId} · score {chunk.score}</div>
                <div className="pipe-lbl">{chunk.usedSentence}</div>
              </div>
            ))}
          </div>
        </section>

        <section className="pipe-card" style={{ minHeight: 160 }}>
          <div className="pipe-source">Evidence guardrails</div>
          <div className="pipe-count">{guardrails?.verdict || 'pending'}</div>
          <div className="pipe-lbl">unsupported claims · {guardrails?.unsupportedClaimCount ?? 0}</div>
          <div className="pipe-lbl">fallback positive contribution · {guardrails?.fallbackPositiveContributionCount ?? 0}</div>
          <div className="pipe-lbl">official live/cache records · {guardrails?.officialLiveOrCacheRecordCount ?? 0}</div>
        </section>
      </div>

      <div className="source-health-panel">
        <div>
          <div className="pipe-source">Provider health & retry</div>
          <div className="pipe-lbl">
            lastCollectedAt {sourceTrustSummary?.lastCollectedAt || '-'} · sourcePeriod {sourceTrustSummary?.sourcePeriod || '확인 대기'} · liveSuccessRate {Math.round(Number(sourceTrustSummary?.liveSuccessRate ?? 0) * 100)}%
          </div>
          {liveCount + cacheCount === 0 && (
            <div className="pipe-lbl req-text">live/cache 공공데이터 0건 - fallback 원인 확인 후 재수집이 필요합니다.</div>
          )}
          {liveCount + cacheCount > 0 && operationEndpointLiveCount === 0 && (
            <div className="pipe-lbl req-text">operation endpoint 증거 0건 - catalog/신청 페이지 기반 응답은 공식 API live 증거로 표시하지 않습니다.</div>
          )}
          {operationEndpointLiveCount > 0 && apiKeyConfiguredLiveCount === 0 && (
            <div className="pipe-lbl req-text">API key configured 증거 0건 - 공개 feed/cache와 인증키 기반 공공 API를 구분해 확인하십시오.</div>
          )}
        </div>
        <button type="button" className="secondary-btn" onClick={onRetry} disabled={!onRetry || retryStatus === 'running'}>
          {retryStatus === 'running' ? '재수집 중' : '공공데이터 재수집'}
        </button>
        {retryStatus && retryStatus !== 'running' && <div className="pipe-lbl">{retryStatus}</div>}
        <div className="provider-health-list">
          {providerHealth.length === 0 ? (
            <div className="pipe-lbl">provider health 대기</div>
          ) : providerHealth.map((provider) => (
            <div key={provider.provider} className={`provider-health-row ${provider.status}`}>
              <strong>{provider.provider}</strong>
              <span>
                L{provider.liveCount ?? 0} / C{provider.cacheCount ?? 0} / F{provider.fallbackCount ?? 0}
                {Number(provider.externalFeedCount ?? 0) > 0 ? ` / Feed${provider.externalFeedCount}` : ''}
                {Number(provider.crawlCount ?? 0) > 0 ? ` / Crawl${provider.crawlCount}` : ''}
              </span>
              <span>
                operation {provider.operationEndpoint ? 'yes' : 'no'} · key {provider.apiKeyConfigured ? 'set' : 'missing'}
                <small>{provider.endpointType || 'endpoint unknown'}</small>
              </span>
              <span>
                {provider.lastCollectedAt || '-'}
                <small>{provider.failureReason || 'failure 없음'}</small>
              </span>
            </div>
          ))}
        </div>
      </div>

      <div style={{ marginTop: 18 }}>
        <div style={{ fontSize: 13, fontWeight: 800, marginBottom: 8 }}>Score contribution waterfall ledger</div>
        <div className="ledger-table-wrap">
          <table className="ledger-table">
            <thead>
              <tr>
                <th>Source</th>
                <th>API proof</th>
                <th>Raw metric</th>
                <th>Normalized metric</th>
                <th>Raw contribution</th>
                <th>Confidence</th>
                <th>Adjusted contribution</th>
                <th>Final score delta</th>
              </tr>
            </thead>
            <tbody>
              {ledger.length === 0 ? (
                <tr>
                  <td colSpan="8">ledger 대기 - backend scoreContributionLedger가 없습니다.</td>
                </tr>
              ) : ledger.slice(0, 10).map((row) => (
                <tr key={`${row.sourceId}-${row.usedFor}`}>
                  <td>
                    <strong>{row.provider}</strong>
                    <span>{row.sourceType} · {row.usedFor}</span>
                  </td>
                  <td>
                    <strong>{row.operationEndpoint ? 'operation endpoint' : 'no operation proof'}</strong>
                    <span>API key {row.apiKeyConfigured ? 'configured' : 'not configured'} · {row.endpointType || 'unknown'}</span>
                    <span>{row.fetchedAt || '-'}</span>
                  </td>
                  <td>{compactMetric(row.rawMetric)}</td>
                  <td>{row.metricSummary || compactMetric(row.normalizedMetric)}</td>
                  <td>{formatDelta(row.rawContribution)}</td>
                  <td>{formatConfidence(row.confidence)}</td>
                  <td>{formatDelta(row.adjustedContribution)}</td>
                  <td>{formatDelta(row.finalScoreDelta)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <div style={{ marginTop: 18 }}>
        <div style={{ fontSize: 13, fontWeight: 800, marginBottom: 8 }}>Export knowledge timeline</div>
        <div style={{ display: 'grid', gap: 8 }}>
          {timeline.length === 0 ? (
            <div className="pipe-card">
              <div className="pipe-source">타임라인 대기</div>
              <div className="pipe-lbl">진단 완료 후 sourceId, fetchedAt, sourceType 기반 이벤트가 표시됩니다.</div>
            </div>
          ) : timeline.slice(0, 6).map((event) => (
            <div key={event.eventId} className="pipe-card evidence-timeline-row">
              <div>
                <div className="pipe-source">{event.eventType}</div>
                <span className={`status-chip ${event.sourceType === 'live' || event.sourceType === 'cache' ? 'ok' : event.sourceType === 'fallback' ? 'req' : 'warn'}`}>
                  {event.sourceType}
                </span>
              </div>
              <div>
                <div style={{ fontSize: 12, fontWeight: 700 }}>{event.provider} · {event.usedFor}</div>
                <div className="pipe-lbl">{event.periodLabel}</div>
                <div className="pipe-lbl">{event.summary}</div>
              </div>
              <div className="pipe-count">{Number(event.scoreImpact ?? 0) > 0 ? '+' : ''}{event.scoreImpact ?? 0}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ fontSize: 11.5, color: 'var(--slate-2)', marginTop: 16, lineHeight: 1.6 }}>
        출처는 live/cache/fallback 공공데이터, crawl 공개 웹, external_feed 재구성 자료, heuristic 규칙 엔진을 구분합니다.
        official API 증거는 live/cache이면서 operation endpoint와 API key configured evidence가 있는 경우에만 인정합니다.
        fallback, crawl, external_feed, heuristic은 “공공 API 연결됨”으로 해석하지 말고 보강 대상 여부를 확인하십시오.
      </div>
    </DiagnosisStage>
  )
}

function formatDelta(value) {
  const number = Number(value ?? 0)
  return `${number > 0 ? '+' : ''}${number}`
}

function formatConfidence(value) {
  const number = Number(value ?? 0)
  if (number > 1) return `${Math.round(number)}%`
  return `${Math.round(number * 100)}%`
}

function compactMetric(metric) {
  if (!metric || typeof metric !== 'object') return '-'
  return Object.entries(metric).slice(0, 3).map(([key, value]) => `${key}=${value}`).join(', ')
}
