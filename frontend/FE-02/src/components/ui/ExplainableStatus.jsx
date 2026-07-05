import { useId } from 'react'
import { buildStatusExplanation } from '../../utils/statusExplanation.js'

export default function ExplainableStatus({
  status,
  explanation,
  sourceType,
  evidenceCount,
  reason,
  children,
  className = '',
}) {
  const id = useId()
  const info = explanation ?? buildStatusExplanation(status, { sourceType, evidenceCount, reason })
  const actions = Array.isArray(info.nextAction) ? info.nextAction.slice(0, 2) : []

  return (
    <span className={`status-explain ${className}`}>
      <button
        type="button"
        className="status-explain-trigger"
        aria-describedby={id}
        aria-label={`${info.status} 상태 설명 보기`}
      >
        {children ?? <span>{info.status}</span>}
        <span className="status-explain-mark" aria-hidden="true">?</span>
      </button>
      <span id={id} role="tooltip" className={`status-explain-popover ${info.severity}`}>
        <strong>{info.plainSummary}</strong>
        <span><b>판단 근거</b>{info.reason}</span>
        {actions.length > 0 && (
          <span><b>다음 행동</b>{actions.join(' · ')}</span>
        )}
        <span><b>데이터 상태</b>{info.dataStatus}</span>
      </span>
    </span>
  )
}
