import DiagnosisStage from './DiagnosisStage.jsx'
import MemoBox from '../ui/MemoBox.jsx'

export default function S00Overview({ data }) {
  return (
    <DiagnosisStage
      eyebrow="S00 DIAGNOSIS OVERVIEW"
      title="진단 개요"
      description="경남 배터리테크 · 리튬이온 배터리팩(HS 850760) · 독일/EU 진출 진단을 11단계로 진행합니다."
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
        <b>요약</b> · 시장성은 충분하므로 인증·제품안전·배터리 규제 준비를 우선 진단합니다. UN38.3 운송시험이 긴급 항목으로 감지되었습니다.
      </MemoBox>
    </DiagnosisStage>
  )
}
