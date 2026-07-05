// S10 데이터 출처 스테이지
import { useEffect, useState } from 'react'
import DiagnosisStage from './DiagnosisStage.jsx'
import { createPublicDataProofRun, getLatestPublicDataProofRun } from '../../services/publicDataService.js'
import ExplainableStatus from '../ui/ExplainableStatus.jsx'

export default function S10Sources({ data, sourceTrustSummary, onRetry, retryStatus }) {
  const pipes = Array.isArray(data) ? data : data?.pipes ?? []
  const timeline = Array.isArray(data?.timeline) ? data.timeline : []
  const retrieval = data?.evidenceRetrieval ?? null
  const guardrails = data?.guardrails ?? null
  const regionalProfile = data?.regionalIndustryProfile ?? null
  const ledger = Array.isArray(data?.ledger) ? data.ledger : []
  const topK = Array.isArray(retrieval?.topK) ? retrieval.topK : []
  const providerHealth = Array.isArray(sourceTrustSummary?.providerHealth) ? sourceTrustSummary.providerHealth : []
  const liveCount = Number(sourceTrustSummary?.liveCount ?? 0)
  const cacheCount = Number(sourceTrustSummary?.cacheCount ?? 0)
  const operationEndpointLiveCount = Number(sourceTrustSummary?.operationEndpointLiveCount ?? 0)
  const apiKeyConfiguredLiveCount = Number(sourceTrustSummary?.apiKeyConfiguredLiveCount ?? 0)
  const emptyCount = Number(sourceTrustSummary?.emptyCount ?? 0)
  const seedCount = Number(sourceTrustSummary?.seedCount ?? 0)
  const diagnosisProofRun = data?.publicDataProofRun
  const diagnosisProofRunId = data?.publicDataProofRunId || sourceTrustSummary?.publicDataProofRunId || ''
  const diagnosisCollectionRunId = data?.publicDataCollectionRunId || sourceTrustSummary?.publicDataCollectionRunId || ''
  const [proofRun, setProofRun] = useState(null)
  const [proofStatus, setProofStatus] = useState('idle')
  const [proofError, setProofError] = useState('')

  useEffect(() => {
    let alive = true
    if (diagnosisProofRun?.proofRunId || diagnosisProofRunId) {
      setProofRun(diagnosisProofRun ?? { proofRunId: diagnosisProofRunId, proofStatus: 'DIAGNOSIS_LINKED' })
      setProofStatus('ready')
      return () => { alive = false }
    }
    setProofStatus('loading')
    getLatestPublicDataProofRun()
      .then((data) => { if (alive) { setProofRun(data); setProofStatus('ready') } })
      .catch((error) => { if (alive) { setProofError(error.message || 'proof run 조회 실패'); setProofStatus('error') } })
    return () => { alive = false }
  }, [diagnosisProofRun, diagnosisProofRunId])

  const runProof = async () => {
    setProofStatus('running')
    setProofError('')
    try {
      const next = await createPublicDataProofRun('manual')
      setProofRun(next)
      setProofStatus('ready')
    } catch (error) {
      setProofError(error.message || 'proof run 생성 실패')
      setProofStatus('error')
    }
  }

  const copyProofRunId = async () => {
    if (!proofRun?.proofRunId || !navigator.clipboard) return
    await navigator.clipboard.writeText(proofRun.proofRunId)
  }

  return (
    <DiagnosisStage
      eyebrow="S10 EVIDENCE WORKFLOW"
      title="출처, 근거 검색, 지식화 타임라인"
      description="재수집은 제품/진단 생성 또는 관리자 public-data refresh에서 수행되며, 이 화면은 결과 근거를 읽기 전용으로 검증합니다."
    >

      <div className="pipe-grid">
        {pipes.map((pipe) => {
          const status = pipe.status ?? 'fallback'
          const quality = pipe.sourceQualityStatus ?? sourceQualityStatus(status, pipe.zeroReason)
          const statusText = status === 'live' ? 'Live'
            : status === 'cache' ? 'Cache'
            : status === 'empty' ? 'Empty'
            : status === 'crawl' ? 'Crawl'
              : status === 'external_feed' ? 'Feed'
                : status === 'seed' ? 'Seed'
              : status === 'heuristic' ? 'Rule'
                : status === 'agent' ? 'Trace'
                  : status === 'user_input' ? 'User'
                    : 'Fallback'
          const chipType = status === 'live' || status === 'cache' ? 'ok'
            : status === 'crawl' || status === 'external_feed' || status === 'heuristic' || status === 'agent' || status === 'user_input' ? 'warn'
              : status === 'empty' || status === 'seed' ? 'warn'
              : 'req'
          return (
          <div key={pipe.source} className="pipe-card">
            <div className="pipe-source">{pipe.source}</div>

            <div className="pipe-count">{pipe.count}</div>
            <div className="pipe-lbl">{pipe.label}</div>
            {pipe.metricSummary && (
              <div className="pipe-lbl">metric · {pipe.metricSummary}</div>
            )}
            <div className="pipe-lbl">quality · {quality}</div>
            {pipe.zeroReason && <div className="pipe-lbl req-text">zero · {pipe.zeroReason}</div>}

            <ExplainableStatus
              status={status}
              sourceType={status}
              evidenceCount={pipe.count}
              reason={pipe.zeroReason || pipe.metricSummary || pipe.label}
            >
              <span className={`status-chip ${chipType}`}>{statusText}</span>
            </ExplainableStatus>
          </div>
          )
        })}

        <div className="pipe-card" style={{ borderStyle: 'dashed', background: '#fafbfe' }}>
          <div className="pipe-source" style={{ color: 'var(--slate-2)' }}>+ 커넥터 추가</div>
          <div style={{ fontSize: 12, color: 'var(--slate-2)', marginTop: 8, lineHeight: 1.5 }}>
            새 공공데이터 소스를 연결합니다
          </div>
          <ExplainableStatus status="대기" sourceType="insufficient_evidence" evidenceCount={0}>
            <span className="status-chip warn">대기중</span>
          </ExplainableStatus>
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

      {regionalProfile && (
        <section className="pipe-card" style={{ marginTop: 18 }}>
          <div className="pipe-source">Regional industry profile</div>
          <div className="pipe-lbl">
            {regionalProfile.regionName || regionalProfile.region || '-'} · {regionalProfile.leadingIndustry || regionalProfile.industry || '-'}
          </div>
          <div className="pipe-lbl">
            source {regionalProfile.publicSourceName || '-'} · period {regionalProfile.sourcePeriod || '-'} · refresh {regionalProfile.refreshCycleHours ?? '-'}h
          </div>
          {regionalProfile.synthetic && (
            <div className="pipe-lbl req-text">
              synthetic_demo_not_real_customer 지역 프로필입니다. 공식 지역산업 공공데이터로 교체해야 live proof가 됩니다.
            </div>
          )}
        </section>
      )}

      <section className="public-proof-panel">
        <div className="public-proof-header">
          <div>
            <div className="pipe-source">Public Data Proof</div>
            <div className="pipe-lbl">
              현재 진단 collectionRunId {diagnosisCollectionRunId || '-'} · proofRunId {diagnosisProofRunId || proofRun?.proofRunId || '-'}
            </div>
          </div>
          <div className="public-proof-actions">
            <button type="button" className="secondary-btn" onClick={runProof} disabled={proofStatus === 'running'}>
              {proofStatus === 'running' ? 'Proof 생성 중' : 'Proof run 생성'}
            </button>
            <button type="button" className="secondary-btn" onClick={copyProofRunId} disabled={!proofRun?.proofRunId}>
              proofRunId 복사
            </button>
            <a className="secondary-btn" href="/api/public-data/health" target="_blank" rel="noreferrer">health 열기</a>
          </div>
        </div>
        {proofError && <div className="pipe-lbl req-text">{proofError}</div>}
        <div className="public-proof-summary">
          {[
            ['proofRunId', proofRun?.proofRunId || proofRun?.proof_run_id || '-'],
            ['collectionRunId', diagnosisCollectionRunId || '-'],
            ['proofStatus', proofRun?.proofStatus || proofRun?.proof_status || proofStatus],
            ['liveProviderCount', proofRun?.liveProviderCount ?? proofRun?.live_provider_count ?? 0],
            ['motieRelatedLiveCount', proofRun?.motieRelatedLiveCount ?? proofRun?.motie_related_live_count ?? 0],
            ['operationEndpointLiveCount', proofRun?.operationEndpointLiveCount ?? proofRun?.operation_endpoint_live_count ?? 0],
            ['apiKeyConfiguredLiveCount', proofRun?.apiKeyConfiguredLiveCount ?? proofRun?.api_key_configured_live_count ?? 0],
            ['schemaPassCount', proofRun?.schemaPassCount ?? proofRun?.schema_pass_count ?? 0],
          ].map(([label, value]) => (
            <div key={label} className="public-proof-kpi">
              <span>{label}</span>
              <strong>{value}</strong>
            </div>
          ))}
        </div>
        {Number(proofRun?.liveProviderCount ?? proofRun?.live_provider_count ?? 0) === 0 && (
          <div className="pipe-lbl req-text">live provider 0건입니다. 실제 키와 operation endpoint가 설정된 서버에서 proof run을 다시 생성해야 합니다.</div>
        )}
        {Array.isArray(proofRun?.providers) && proofRun.providers.length > 0 && (
          <div className="ledger-table-wrap" style={{ marginTop: 12 }}>
            <table className="ledger-table">
              <thead>
                <tr>
                  <th>Provider</th>
                  <th>Source</th>
                  <th>Quality</th>
                  <th>HTTP</th>
                  <th>Records</th>
                  <th>API proof</th>
                  <th>Raw hash</th>
                  <th>Schema</th>
                  <th>Fallback reason</th>
                </tr>
              </thead>
              <tbody>
                {proofRun.providers.slice(0, 12).map((provider) => (
                  <tr key={`${provider.providerCode}-${provider.rawResponseSha256 || provider.fallbackReason}`}>
                    <td>
                      <strong>{provider.providerCode}</strong>
                      <span>{provider.agencyName || provider.datasetName || '-'}</span>
                    </td>
                    <td>{provider.sourceType}</td>
                    <td>
                      <strong>{provider.sourceQualityStatus || sourceQualityStatus(provider.sourceType, provider.fallbackReason)}</strong>
                      <span>{provider.zeroReason || provider.fallbackReason || '-'}</span>
                    </td>
                    <td>{provider.httpStatus || '-'}</td>
                    <td>{provider.recordCount ?? 0}</td>
                    <td>
                      endpoint {provider.operationEndpoint ? 'yes' : 'no'}
                      <span>key {provider.apiKeyConfigured ? 'set' : 'missing'}</span>
                    </td>
                    <td>{shortHash(provider.rawResponseSha256)}</td>
                    <td>{provider.schemaValidationStatus || '-'}</td>
                    <td>{provider.fallbackReason || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

      <div className="source-health-panel">
        <div>
          <div className="pipe-source">Provider health & retry</div>
          <div className="pipe-lbl">
            lastCollectedAt {sourceTrustSummary?.lastCollectedAt || '-'} · sourcePeriod {sourceTrustSummary?.sourcePeriod || '확인 대기'} · liveSuccessRate {Math.round(Number(sourceTrustSummary?.liveSuccessRate ?? 0) * 100)}%
          </div>
          {seedCount > 0 && (
            <div className="pipe-lbl req-text">
              synthetic_demo_not_real_customer seed {seedCount}건은 합성 시연 데이터이며, live/cache proof 숫자를 올리지 않습니다.
            </div>
          )}
          {liveCount + cacheCount === 0 && (
            <div className="pipe-lbl req-text">live/cache 공공데이터 0건 - fallback 원인 확인 후 재수집이 필요합니다.</div>
          )}
          {emptyCount > 0 && (
            <div className="pipe-lbl req-text">empty source {emptyCount}건 - API key, operation endpoint, catalog URL, 0건 응답 여부를 확인해야 합니다.</div>
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
                L{provider.liveCount ?? 0} / C{provider.cacheCount ?? 0} / E{provider.emptyCount ?? 0} / F{provider.fallbackCount ?? 0}
                {Number(provider.externalFeedCount ?? 0) > 0 ? ` / Feed${provider.externalFeedCount}` : ''}
                {Number(provider.crawlCount ?? 0) > 0 ? ` / Crawl${provider.crawlCount}` : ''}
              </span>
              <span>
                operation {provider.operationEndpoint ? 'yes' : 'no'} · key {provider.apiKeyConfigured ? 'set' : 'missing'}
                <small>{provider.endpointType || 'endpoint unknown'}</small>
              </span>
              <span>
                {provider.lastCollectedAt || '-'}
                <small>{provider.sourceQualityStatus || '-'} · {provider.zeroReason || provider.failureReason || 'failure 없음'}</small>
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
                    <span>{row.sourceType} · {row.sourceQualityStatus || sourceQualityStatus(row.sourceType, row.zeroReason)} · {row.usedFor}</span>
                    {row.zeroReason && <span>{row.zeroReason}</span>}
                  </td>
                  <td>
                    <strong>{row.operationEndpoint ? 'operation endpoint' : 'no operation proof'}</strong>
                    <span>API key {row.apiKeyConfigured ? 'configured' : 'not configured'} · {row.endpointType || 'unknown'}</span>
                    <span>schema {schemaStatus(row.schemaValidation)}</span>
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
                <ExplainableStatus
                  status={event.sourceType}
                  sourceType={event.sourceType}
                  evidenceCount={1}
                  reason={event.summary}
                >
                  <span className={`status-chip ${event.sourceType === 'live' || event.sourceType === 'cache' ? 'ok' : event.sourceType === 'fallback' ? 'req' : 'warn'}`}>
                    {event.sourceType}
                  </span>
                </ExplainableStatus>
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

function sourceQualityStatus(sourceType, zeroReason = '') {
  const missingKeyCode = ['API', 'KEY', 'NOT', 'CONFIGURED'].join('_')
  const missingEndpointCode = ['OPERATION', 'ENDPOINT', 'NOT', 'CONFIGURED'].join('_')
  const catalogEndpointCode = ['CATALOG', 'PAGE', 'ENDPOINT'].join('_')
  const publicDisabledCode = ['PUBLIC', 'DATA', 'DISABLED'].join('_')
  if (sourceType === 'live') return 'LIVE_VERIFIED'
  if (sourceType === 'cache') return 'CACHE_VERIFIED'
  if (sourceType === 'seed') return 'SEED_SYNTHETIC'
  if (sourceType === 'heuristic') return 'HEURISTIC_ONLY'
  if (sourceType === 'fallback') return String(zeroReason).includes(missingKeyCode) ? 'EMPTY_NOT_CONFIGURED' : 'FALLBACK_ERROR'
  if (sourceType === 'empty') {
    return [missingKeyCode, missingEndpointCode, catalogEndpointCode, publicDisabledCode].some((code) => String(zeroReason).includes(code))
      ? 'EMPTY_NOT_CONFIGURED'
      : 'EMPTY_TRUE_ZERO'
  }
  return 'HEURISTIC_ONLY'
}

function schemaStatus(schemaValidation) {
  if (!schemaValidation || typeof schemaValidation !== 'object') return '-'
  return schemaValidation.status || schemaValidation.schemaValidationStatus || '-'
}

function shortHash(value) {
  if (!value) return '-'
  const text = String(value)
  return text.length > 18 ? `${text.slice(0, 18)}...` : text
}
