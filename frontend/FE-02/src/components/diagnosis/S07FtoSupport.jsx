// S07 FTO·지원사업 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'
import MemoBox from '../ui/MemoBox.jsx'

export default function S07FtoSupport({ patents, supports }) {
  return (
    <DiagnosisStage
      eyebrow="S07 FTO & SUPPORT"
      title="특허 위험지도와 지원사업"
      description="기술 리스크와 비용 부담을 함께 줄여주는 실행 화면입니다."
    >

      <div className="fto-split">

        <div>
          <div className="fto-title">Patent Risk Map</div>
          {patents.map((p) => (
            <div key={p.name} className="patent-card">

              <span className={`patent-badge ${p.riskClass}`}>{p.badge}</span>
              <div className="patent-name">{p.name}</div>
              <div className="patent-sub">{p.sub}</div>
            </div>
          ))}
        </div>

        <div>
          <div className="fto-title">Support Programs · 5 Matched</div>
          {supports.map((s) => (
            <div key={s.name} className="support-card">
              <div>
                <div className="support-name">{s.name}</div>
                <div className="support-org">{s.org}</div>
              </div>

              <span className={`deadline-chip ${s.isGreen ? 'g' : ''}`}>{s.dday}</span>
            </div>
          ))}
        </div>
      </div>

      <MemoBox style={{ marginTop: 14 }}>
        <b>AI 제안</b> · BMS·방열 키워드는 회피설계 검토 필요. 마감 임박한 해외규격인증 지원(D-14)을 즉시 신청 준비하세요.
      </MemoBox>
    </DiagnosisStage>
  )
}
