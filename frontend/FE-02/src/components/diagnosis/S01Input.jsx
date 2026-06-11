import DiagnosisStage from './DiagnosisStage.jsx'
import MemoBox from '../ui/MemoBox.jsx'

export default function S01Input({ data }) {
  return (
    <DiagnosisStage
      eyebrow="S01 INPUT"
      title="제품 입력"
      description="진단에 사용된 기업·제품 정보입니다."
    >

      <div className="pred-grid" style={{ gridTemplateColumns: 'repeat(2,1fr)' }}>
        {data.map((item) => (
          <div key={item.key} className="pred">

            <div className="ph"><span>{item.key}</span></div>

            <div className="pt" style={{ fontSize: 20, marginTop: 10 }}>{item.val}</div>
          </div>
        ))}
      </div>
      <MemoBox style={{ marginTop: 14 }}>
        <b>제품 특성</b> · 배터리 탑재 · 산업재 · 디지털 기능 일부 → AI Planner가 5단계 분석 계획을 생성했습니다.
      </MemoBox>
    </DiagnosisStage>
  )
}
