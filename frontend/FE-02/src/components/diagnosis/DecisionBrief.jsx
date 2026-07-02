import { buildDecisionBrief } from '../../services/diagnosisService.js'

export default function DecisionBrief({ session, stages }) {
  const brief = buildDecisionBrief(session, stages)

  return (
    <section className="decision-brief" aria-label="Gate decision action brief">
      <div className="decision-brief-head">
        <div>
          <span className="decision-brief-kicker">Decision Brief</span>
          <h2>{brief.gateLabel}</h2>
          <p>{brief.gateReason}</p>
        </div>
        <div className="decision-source-mini">
          <span>live {brief.sourceCounts.live}</span>
          <span>cache {brief.sourceCounts.cache}</span>
          <span className={brief.fallbackReviewRequired ? 'danger' : ''}>fallback {brief.sourceCounts.fallback}</span>
        </div>
      </div>

      {brief.fallbackReviewRequired && (
        <div className="decision-warning">
          담당자 검토 필요 · {brief.fallbackMessage}
        </div>
      )}

      <div className="decision-brief-grid">
        <div className="decision-panel">
          <h3>핵심 위험 3개</h3>
          <ol>
            {brief.topRisks.map((risk) => (
              <li key={risk.label}>
                <span className={`risk-dot ${risk.level}`}>{risk.level}</span>
                <p>{risk.label}</p>
              </li>
            ))}
          </ol>
        </div>

        <div className="decision-panel">
          <h3>다음 행동 3개</h3>
          <div className="action-strip">
            {brief.nextActions.map((action) => (
              <div key={action.bucket} className="decision-action">
                <span>{action.bucket}</span>
                <strong>{action.title}</strong>
                <small>{action.owner} · {action.output}</small>
              </div>
            ))}
          </div>
        </div>
      </div>
    </section>
  )
}
