export default function GateDecisionHero({ gateDecision, impactEstimate, confidencePanel, sourceTrustSummary }) {
  if (!gateDecision) return null

  const status = gateDecision.status ?? 'CONDITIONAL_GO'
  const className = `gate-hero ${status.toLowerCase().replaceAll('_', '-')}`
  const mustFix = Array.isArray(gateDecision.mustFixBeforeQuote) ? gateDecision.mustFixBeforeQuote : []
  const fallbackCount = Number(sourceTrustSummary?.fallbackCount ?? confidencePanel?.fallbackSourceCount ?? 0)

  return (
    <section className={className}>
      <div className="gate-main">
        <div className="gate-label">수출 Gate 판단</div>
        <div className="gate-status">{gateDecision.label ?? status}</div>
        <p>{gateDecision.oneLineReason ?? '견적 제시 전 source trace와 담당자 근거 확인이 필요합니다.'}</p>
      </div>

      {fallbackCount > 0 && (
        <div className="gate-warning">
          공공데이터 일부가 fallback입니다. 바이어 회신 또는 견적 제시 전 담당자 검토가 필요합니다.
        </div>
      )}

      <div className="gate-grid">
        <div>
          <span>다음 가능 단계</span>
          <b>{gateDecision.allowedNextStep ?? '바이어 1차 회신'}</b>
        </div>
        <div>
          <span>막히는 단계</span>
          <b>{gateDecision.blockedStep ?? '견적 제시'}</b>
        </div>
        <div>
          <span>신뢰도</span>
          <b>{confidencePanel?.level ?? 'MEDIUM'}</b>
          <small>
            live {confidencePanel?.liveSourceCount ?? 0} · cache {confidencePanel?.cacheSourceCount ?? 0} · fallback {confidencePanel?.fallbackSourceCount ?? 0}
          </small>
        </div>
        <div>
          <span>예상 영향</span>
          <b>{impactEstimate?.delayRiskDays ?? 7}일 지연 위험</b>
          <small>{Number(impactEstimate?.avoidableCostKrw ?? 0).toLocaleString()}원 회피 가능 비용 추정</small>
        </div>
      </div>

      {mustFix.length > 0 && (
        <div className="gate-fix">
          <span>견적 전 필수 확인</span>
          <ul>
            {mustFix.slice(0, 4).map((item) => <li key={item}>{item}</li>)}
          </ul>
        </div>
      )}
    </section>
  )
}
