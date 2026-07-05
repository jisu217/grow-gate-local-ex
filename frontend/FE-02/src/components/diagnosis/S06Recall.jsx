// S06 리콜 위험 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'
import ExplainableStatus from '../ui/ExplainableStatus.jsx'

export default function S06Recall({ recalls }) {
  return (
    <DiagnosisStage
      eyebrow="S06 RECALL COPILOT"
      title="위해원인 클러스터와 시험 전 체크"
      description="유사 리콜을 설계 보완·시험 항목·FMEA 초안으로 전환합니다."
    >

      <div className="recall-grid">
        {recalls.map((rec) => (

          <div key={rec.title} className={`recall-card ${rec.riskClass}`}>

            <ExplainableStatus status={rec.badge} explanation={rec.statusExplanation}>
              <span className={`recall-badge-pill ${rec.riskClass}`}>{rec.badge}</span>
            </ExplainableStatus>
            <div className="recall-title">{rec.title}</div>

            <div className="recall-sim">{rec.similarity}</div>
            <div className="recall-sim-lbl">유사도 점수</div>

            <div className="recall-checks">
              {rec.checks.map((check, index) => (
                <div key={check} className="recall-check">
                  <ExplainableStatus status={check} explanation={rec.checkExplanations?.[index]}>
                    <span>{check}</span>
                  </ExplainableStatus>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </DiagnosisStage>
  )
}
