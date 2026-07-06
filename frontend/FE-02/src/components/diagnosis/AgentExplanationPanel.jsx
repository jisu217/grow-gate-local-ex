import { Link } from 'react-router-dom'
import { buildAgentKnowledgeActions, humanAgentDataStatus } from './agentKnowledgePresenter.js'

export default function AgentExplanationPanel({ session, onSourcesClick, onBoardClick }) {
  const candidates = session?.agentKnowledgeCandidates ?? []
  const summary = session?.agentExplanationSummary ?? {}
  const actions = buildAgentKnowledgeActions(session)
  if (!candidates.length) return null

  return (
    <section className="agent-knowledge-panel" aria-label="Agent judgment knowledge loop">
      <div className="agent-knowledge-head">
        <div>
          <span className="agent-knowledge-kicker">Agent Knowledge Loop</span>
          <h2>AI 판단 근거 지식 후보</h2>
        </div>
        <div className="agent-knowledge-summary">
          <span>{summary.candidateCount ?? candidates.length}건 생성</span>
          <span>승인 지식만 재사용</span>
        </div>
      </div>

      <div className="agent-knowledge-grid">
        {candidates.slice(0, 10).map((candidate) => (
          <article key={candidate.id ?? `${candidate.agent}-${candidate.knowledgeType}`} className="agent-knowledge-card">
            <div className="agent-knowledge-card-head">
              <div>
                <span className="agent-knowledge-agent">{candidate.agent}</span>
                <h3>{candidate.title ?? candidate.knowledgeType}</h3>
              </div>
              <span className={`agent-knowledge-status ${candidate.statusTone ?? 'info'}`}>
                {candidate.statusLabel ?? candidate.status ?? '검토 대기'}
              </span>
            </div>

            <dl className="agent-knowledge-facts">
              <dt>결론</dt>
              <dd>{candidate.plainSummary ?? '담당자 검토 필요'}</dd>
              <dt>이유</dt>
              <dd>{candidate.whyItMatters ?? '근거와 한계 확인 필요'}</dd>
              <dt>근거</dt>
              <dd>{join(candidate.evidenceSummary)}</dd>
              <dt>한계</dt>
              <dd>{join(candidate.limitations)}</dd>
              <dt>다음 행동</dt>
              <dd>{join(candidate.nextActions)}</dd>
              <dt>데이터 상태</dt>
              <dd>{dataStatus(candidate.dataStatus)}</dd>
            </dl>

            <div className="agent-knowledge-actions">
              {actions.map((action) => {
                if (!action.enabled) {
                  return <span key={action.label} className="agent-knowledge-action-disabled">{action.disabledLabel}</span>
                }
                if (action.label === '근거 데이터 보기' && onSourcesClick) {
                  return <button key={action.label} type="button" onClick={onSourcesClick}>{action.label}</button>
                }
                if (action.label === '실행보드 보기' && onBoardClick) {
                  return <button key={action.label} type="button" onClick={onBoardClick}>{action.label}</button>
                }
                return <Link key={action.label} to={action.to}>{action.label}</Link>
              })}
            </div>
          </article>
        ))}
      </div>
    </section>
  )
}

function join(value) {
  if (Array.isArray(value) && value.length) return value.slice(0, 4).join(' / ')
  if (value) return String(value)
  return '담당자 확인 필요'
}

function dataStatus(value) {
  return humanAgentDataStatus(value)
}
