// S03 규칙 점수 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

export default function S03PredLab({ predictions, features }) {
  return (
    <DiagnosisStage
      eyebrow="S03 RULE SCORING LAB"
      title="근거 기반 점수 산식 진단"
      right={<span className="s-tag">Rule rubric · LLM assisted</span>}
    >

      <div className="pred-grid">
        {predictions.map((pred) => (
          <div key={pred.title} className="pred">

            <div className="ph">
              <span>{pred.head}</span>
              <span>{pred.acc}</span>
            </div>
            <div className="pt">{pred.title}</div>

            <div className="pv">
              {pred.value}<span>{pred.unit}</span>
            </div>

            <div className="pbar">
              <i style={{ width: `${pred.bar}%` }} />
            </div>
            <div className="pd">{pred.desc}</div>
          </div>
        ))}
      </div>

      <div className="fi">
        <h3>통합 Feature Importance</h3>
        {features.map((feat) => (
          <div key={feat.key ?? feat.label} className="row">
            <div className="lab">
              <strong>{feat.label}</strong>
              <span className="feature-meta">
                {feat.sourceType ?? 'unknown'} · evidence {feat.evidenceCount ?? 0}건
              </span>
              {feat.description && <span className="feature-desc">{feat.description}</span>}
            </div>

            <div className="tk">
              <i style={{ width: `${feat.value}%` }} />
            </div>
            <div className="val">{feat.value}</div>
          </div>
        ))}
      </div>
    </DiagnosisStage>
  )
}
