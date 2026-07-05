// S05 규제 매트릭스 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'
import Pill from '../ui/Pill.jsx'
import ExplainableStatus from '../ui/ExplainableStatus.jsx'

export default function S05RegMatrix({ regulations }) {
  return (
    <DiagnosisStage
      eyebrow="S05 REGULATION PASSPORT"
      title="규제·인증 우선순위 매트릭스"
      description="제품·대상국에 적용되는 규제를 우선순위·기간·예상비용과 함께 정렬합니다."
    >

      <div className="reg-head-row">
        <span>규제·인증</span>
        <span>시장</span>
        <span>우선순위</span>
        <span>상태</span>
        <span>기간</span>
        <span>예상비용</span>
      </div>

      {regulations.map((reg) => {

        const dim = (v) => (v === '—' ? ' dim' : '')
        return (
          <div key={reg.nm} className="reg-row">

            <div>
              <div className="reg-name">{reg.nm}</div>
              <div className="reg-mkt">{reg.mkt}</div>
            </div>

            <div>
              <Pill type={reg.market[0]} style={{ fontSize: 10 }}>{reg.market[1]}</Pill>
            </div>

            <div>
              <ExplainableStatus status={reg.priority[1]} explanation={reg.priorityExplanation}>
                <Pill type={reg.priority[0]}>{reg.priority[1]}</Pill>
              </ExplainableStatus>
            </div>

            <div>
              <ExplainableStatus status={reg.status[1]} explanation={reg.statusExplanation}>
                <Pill type={reg.status[0]}>{reg.status[1]}</Pill>
              </ExplainableStatus>
            </div>

            <div className={`num${dim(reg.duration)}`}>{reg.duration}</div>

            <div className={`num${dim(reg.cost)}`}>
              {reg.cost === '—' || reg.cost === '-'
                ? reg.cost
                : (
                  <ExplainableStatus status={reg.cost} explanation={reg.costExplanation}>
                    <span>{reg.cost}</span>
                  </ExplainableStatus>
                )}
            </div>
          </div>
        )
      })}
    </DiagnosisStage>
  )
}
