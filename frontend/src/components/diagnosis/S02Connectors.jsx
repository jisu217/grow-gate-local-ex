import DiagnosisStage from './DiagnosisStage.jsx'

export default function S02Connectors({ data }) {
  return (
    <DiagnosisStage
      eyebrow="S02 DATA CONNECTORS"
      title="공공데이터 호출 오케스트레이션"
      description="무역·국가·인증·리콜·특허·지원사업 데이터를 같은 실행 단위로 정규화하고 근거 ID를 연결합니다."
    >
      {data.map((conn) => (

        <div key={conn.no} className="conn">
          <span className="no">{conn.no}</span>
          <span className="txt">
            <span className="nm">{conn.nm}</span>
            <span className="meta">{conn.meta}</span>
          </span>
          <span className="dot" />
        </div>
      ))}
    </DiagnosisStage>
  )
}
