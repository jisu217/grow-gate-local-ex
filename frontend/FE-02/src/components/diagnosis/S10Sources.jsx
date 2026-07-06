// S10 데이터 출처 스테이지
import { useEffect, useState } from 'react'
import DiagnosisStage from './DiagnosisStage.jsx'
import { createPublicDataProofRun, getLatestPublicDataProofRun } from '../../services/publicDataService.js'
import {
  buildEvidenceWorkflowView,
  humanProviderName,
  humanStatusLabel,
} from './sourceEvidencePresenter.js'

export default function S10Sources({ data, sourceTrustSummary, onRetry, retryStatus }) {
  const pipes = Array.isArray(data) ? data : data?.pipes ?? []
  const ledger = Array.isArray(data?.ledger) ? data.ledger : []
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

  const workflow = buildEvidenceWorkflowView({
    pipes,
    ledger,
    sourceTrustSummary,
    retrieval: data?.evidenceRetrieval ?? null,
    guardrails: data?.guardrails ?? null,
    timeline: Array.isArray(data?.timeline) ? data.timeline : [],
    proofRun: diagnosisProofRun ?? proofRun,
    agentKnowledgeCandidates: data?.agentKnowledgeCandidates ?? [],
  })
  const regionalProfile = data?.regionalIndustryProfile ?? null
  const providerHealth = Array.isArray(sourceTrustSummary?.providerHealth) ? sourceTrustSummary.providerHealth : []
  const canRetry = Boolean(sourceTrustSummary?.retryPayload?.hsCode && sourceTrustSummary?.retryPayload?.targetCountry && sourceTrustSummary?.retryPayload?.productName)

  return (
    <DiagnosisStage
      eyebrow="S10 EVIDENCE WORKFLOW"
      title="출처, 근거 검색, 지식화 타임라인"
      description="재수집은 제품/진단 생성 또는 관리자 public-data refresh에서 수행되며, 이 화면은 결과 근거를 읽기 전용으로 검증합니다."
    >
      <section className="evidence-overview-panel" aria-label="근거 상태 한눈에 보기">
        <div className="evidence-overview-copy">
          <span className="source-trust-kicker">근거 상태 한눈에 보기</span>
          <strong>{workflow.summary.sentence}</strong>
          <p>마지막 수집 시각 {workflow.summary.lastCollectedAt || '확인 대기'}</p>
        </div>
        <div className="evidence-overview-kpis">
          <EvidenceKpi label="총 근거 수" value={workflow.summary.totalEvidenceCount} tone="neutral" />
          <EvidenceKpi label="바로 사용 가능" value={workflow.summary.readyCount} tone="ok" />
          <EvidenceKpi label="담당자 확인 필요" value={workflow.summary.needsReviewCount} tone="warn" />
          <EvidenceKpi label="API 설정 필요" value={workflow.summary.needsConfigurationCount} tone="req" />
          <EvidenceKpi label="지식화 후보" value={workflow.summary.knowledgeCandidateCount} tone="info" />
        </div>
        <div className="evidence-overview-actions">
          <button type="button" className="secondary-btn" onClick={onRetry} disabled={!canRetry || retryStatus === 'running'}>
            {retryStatus === 'running' ? '재수집 중' : '공공데이터 재수집'}
          </button>
          {retryStatus && retryStatus !== 'running' && <span>{retryStatus}</span>}
        </div>
      </section>

      <div className="evidence-group-list">
        <EvidenceGroup group={workflow.groups.ready} emptyText="바로 사용할 수 있는 live/cache 근거가 아직 없습니다." />
        <EvidenceGroup group={workflow.groups.needsReview} emptyText="담당자 확인이 필요한 근거가 없습니다." />
        <EvidenceGroup group={workflow.groups.needsConfiguration} emptyText="API 설정이 필요한 근거가 없습니다." />
        <EvidenceGroup group={workflow.groups.knowledge} emptyText="아직 지식화 후보가 없습니다." />
      </div>

      <section className="knowledge-timeline-panel" aria-label="Export knowledge timeline">
        <div className="evidence-section-head">
          <div>
            <span className="source-trust-kicker">지식화 타임라인</span>
            <h3>근거가 제품 지식으로 쌓이는 흐름</h3>
          </div>
          <span className="evidence-muted">승인 전 후보는 Gate를 직접 바꾸지 않습니다.</span>
        </div>
        <div className="knowledge-timeline-steps">
          {workflow.timeline.steps.map((step, index) => (
            <div key={step.label} className={`knowledge-step ${step.tone}`}>
              <span className="knowledge-step-index">{index + 1}</span>
              <strong>{step.label}</strong>
              <em>{step.statusLabel}</em>
            </div>
          ))}
        </div>
      </section>

      {regionalProfile && (
        <section className="evidence-human-panel">
          <div className="pipe-source">지역 산업 근거</div>
          <p>
            {regionalProfile.regionName || regionalProfile.region || '지역 확인 대기'}의 {regionalProfile.leadingIndustry || regionalProfile.industry || '산업 정보'}를
            보조 근거로 사용했습니다.
          </p>
          <span>원천 {regionalProfile.publicSourceName || '확인 대기'} · 기준 기간 {regionalProfile.sourcePeriod || '확인 대기'}</span>
          {regionalProfile.synthetic && (
            <strong className="req-text">데모 지역 프로필입니다. 공식 지역산업 공공데이터로 교체해야 live proof가 됩니다.</strong>
          )}
        </section>
      )}

      <section className="evidence-human-panel">
        <div className="evidence-section-head">
          <div>
            <span className="source-trust-kicker">공공데이터 수집 증명</span>
            <h3>수집 실행과 검증 상태</h3>
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
        <p>
          현재 진단의 수집 실행은 {diagnosisCollectionRunId || '확인 대기'} 상태입니다.
          실시간 제공기관 {proofRun?.liveProviderCount ?? proofRun?.live_provider_count ?? 0}건,
          형식 검증 통과 {proofRun?.schemaPassCount ?? proofRun?.schema_pass_count ?? 0}건을 확인했습니다.
        </p>
      </section>

      <details className="technical-details-panel">
        <summary>상세 기술 정보</summary>
        <p>아래 정보는 개발자와 심사위원 확인용입니다. 기본 업무 화면에서는 raw provider code, metric, sourceId를 숨깁니다.</p>

        <div className="technical-card-grid">
          {workflow.groups.technical.cards.map((card) => (
            <div key={card.id} className="technical-card">
              <strong>{card.title}</strong>
              <dl>
                {card.technicalRows.map((row) => (
                  <div key={`${card.id}-${row.label}`}>
                    <dt>{row.label}</dt>
                    <dd>{row.value}</dd>
                  </div>
                ))}
              </dl>
            </div>
          ))}
        </div>

        <TechnicalProviderHealth providerHealth={providerHealth} />
        <TechnicalLedger ledger={ledger} />
        <TechnicalProofRun
          proofRun={proofRun}
          diagnosisCollectionRunId={diagnosisCollectionRunId}
          proofStatus={proofStatus}
        />
      </details>
    </DiagnosisStage>
  )
}

function EvidenceKpi({ label, value, tone }) {
  return (
    <div className={`source-trust-count ${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function EvidenceGroup({ group, emptyText }) {
  return (
    <section className="evidence-group">
      <div className="evidence-section-head">
        <div>
          <h3>{group.title}</h3>
          <p>{group.description}</p>
        </div>
        <span>{group.cards.length}건</span>
      </div>
      {group.cards.length === 0 ? (
        <div className="evidence-empty">{emptyText}</div>
      ) : (
        <div className="evidence-card-grid">
          {group.cards.map((card) => <EvidenceCard key={card.id} card={card} />)}
        </div>
      )}
    </section>
  )
}

function EvidenceCard({ card }) {
  return (
    <article className={`evidence-card ${card.scoreTone}`}>
      <div className="evidence-card-head">
        <div>
          <h4>{card.title}</h4>
          {card.agency && <span>{card.agency}</span>}
        </div>
        <strong className={`evidence-score ${card.scoreTone}`}>{card.scoreLabel}</strong>
      </div>
      <dl className="evidence-card-body">
        <EvidenceFact label="이 근거가 말해주는 것" value={card.whatItMeans} />
        <EvidenceFact label="Gate 판단에 준 영향" value={card.gateImpact} />
        <EvidenceFact label="현재 상태" value={card.currentStatus} />
        <EvidenceFact label="담당자가 할 일" value={card.nextAction} />
        <EvidenceFact label="지식화 상태" value={<span className={`knowledge-badge ${card.statusTone || card.scoreTone}`}>{card.knowledgeStatus}</span>} />
      </dl>
    </article>
  )
}

function EvidenceFact({ label, value }) {
  return (
    <>
      <dt>{label}</dt>
      <dd>{value}</dd>
    </>
  )
}

function TechnicalProviderHealth({ providerHealth }) {
  if (!providerHealth.length) return null
  return (
    <div className="technical-section">
      <h4>Provider health</h4>
      <div className="provider-health-list">
        {providerHealth.map((provider) => (
          <div key={provider.provider} className={`provider-health-row ${provider.status}`}>
            <strong>{humanProviderName(provider.provider)}</strong>
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
              <small>{provider.sourceQualityStatus ? humanStatusLabel(provider.sourceQualityStatus) : '-'} · {provider.zeroReason || provider.failureReason || 'failure 없음'}</small>
            </span>
          </div>
        ))}
      </div>
    </div>
  )
}

function TechnicalLedger({ ledger }) {
  return (
    <div className="technical-section">
      <h4>Score contribution waterfall ledger</h4>
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
            ) : ledger.slice(0, 12).map((row) => (
              <tr key={`${row.sourceId}-${row.usedFor}`}>
                <td>
                  <strong>{humanProviderName(row.provider)}</strong>
                  <span>{row.provider} · {row.sourceType} · {row.sourceQualityStatus || '-'}</span>
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
  )
}

function TechnicalProofRun({ proofRun, diagnosisCollectionRunId, proofStatus }) {
  const providers = Array.isArray(proofRun?.providers) ? proofRun.providers : []
  return (
    <div className="technical-section">
      <h4>Public Data Proof</h4>
      <div className="public-proof-summary">
        {[
          ['proofRunId', proofRun?.proofRunId || proofRun?.proof_run_id || '-'],
          ['collectionRunId', diagnosisCollectionRunId || '-'],
          ['proofStatus', proofRun?.proofStatus || proofRun?.proof_status || proofStatus],
          ['liveProviderCount', proofRun?.liveProviderCount ?? proofRun?.live_provider_count ?? 0],
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
      {providers.length > 0 && (
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
              {providers.map((provider) => (
                <tr key={provider.providerCode}>
                  <td>
                    <strong>{humanProviderName(provider.providerCode)}</strong>
                    <span>{provider.providerCode}</span>
                  </td>
                  <td>{provider.sourceType || '-'}</td>
                  <td>{provider.sourceQualityStatus || '-'}</td>
                  <td>{provider.httpStatus ?? '-'}</td>
                  <td>{provider.recordCount ?? 0}</td>
                  <td>
                    <span>operation {provider.operationEndpoint ? 'yes' : 'no'}</span>
                    <span>key {provider.apiKeyConfigured ? 'set' : 'missing'}</span>
                  </td>
                  <td>{shortHash(provider.rawResponseHash)}</td>
                  <td>{schemaStatus(provider.schemaValidation)}</td>
                  <td>{provider.fallbackReason || '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
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

function schemaStatus(schemaValidation) {
  if (!schemaValidation || typeof schemaValidation !== 'object') return '-'
  return schemaValidation.status || schemaValidation.schemaValidationStatus || '-'
}

function shortHash(value) {
  if (!value) return '-'
  const text = String(value)
  return text.length > 18 ? `${text.slice(0, 18)}...` : text
}
