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
        <b>FTO 사전 탐색</b> · 본 결과는 공개 특허·상표 데이터와 입력 정보를 기반으로 한 1차 위험 신호입니다. 특허 침해 여부, 실시 가능성, 권리 유효성에 대한 법률 판단이 아니며, 최종 의사결정 전 변리사 또는 법률 전문가 검토가 필요합니다.
      </MemoBox>
      <MemoBox style={{ marginTop: 10 }}>
        <b>AI 보조 해석</b> · BMS·방열 키워드는 전문가 검토가 필요한 후보 항목입니다. 지원사업 매칭은 원문 공고와 이용조건을 확인한 뒤 담당자가 신청 여부를 결정해야 합니다.
      </MemoBox>
    </DiagnosisStage>
  )
}
