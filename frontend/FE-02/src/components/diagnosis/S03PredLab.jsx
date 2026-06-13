// S03 ML 예측 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

export default function S03PredLab({ predictions, features }) {
  return (
    <DiagnosisStage
      eyebrow="S03 XGBOOST PREDICTION LAB"
      title="예측·추정 모델 진단"
      right={<span className="s-tag">XGBoost · deterministic demo</span>}
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
          <div key={feat.label} className="row">
            <div className="lab">{feat.label}</div>

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
