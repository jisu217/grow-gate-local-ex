// S06 리콜 위험 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

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

            <span className={`recall-badge-pill ${rec.riskClass}`}>{rec.badge}</span>
            <div className="recall-title">{rec.title}</div>

            <div className="recall-sim">{rec.similarity}</div>
            <div className="recall-sim-lbl">유사도 점수</div>

            <div className="recall-checks">
              {rec.checks.map((check) => (
                <div key={check} className="recall-check">{check}</div>
              ))}
            </div>
          </div>
        ))}
      </div>
    </DiagnosisStage>
  )
}
