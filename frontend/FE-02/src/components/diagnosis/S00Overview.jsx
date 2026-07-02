// S00 진단 개요 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'
import MemoBox from '../ui/MemoBox.jsx'

export default function S00Overview({ data }) {
  return (
    <DiagnosisStage
      eyebrow="S00 DIAGNOSIS OVERVIEW"
      title="진단 개요"
      description="제품 정보, HS Code, 대상국, 바이어 문의를 기준으로 Gate 판단과 source trace를 진행합니다."
    >

      <div className="pred-grid">
        {data.map((item) => (
          <div key={item.title} className="pred">
            <div className="pt">{item.title}</div>

            <div className="pv">
              {item.value}<span>{item.unit}</span>
            </div>
            <div className="pd">{item.desc}</div>
          </div>
        ))}
      </div>
      <MemoBox>
        <b>요약</b> · 이 화면은 자동 확정 판단이 아니라 견적 전 1차 리스크 체크입니다. 상세 근거는 source trace와 action board에서 확인하세요.
      </MemoBox>
    </DiagnosisStage>
  )
}
