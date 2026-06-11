import DiagnosisStage from './DiagnosisStage.jsx'
import MemoBox from '../ui/MemoBox.jsx'

export default function S04Market({ markets }) {
  return (
    <DiagnosisStage
      eyebrow="S04 MARKET SIGNAL"
      title="시장별 진출 기회"
      description="시장성 점수는 규제·인증 실행순서를 정하기 위한 첫 번째 신호로 사용합니다."
    >

      <div className="market-grid3">
        {markets.map((mkt) => (
          <div key={mkt.flag} className="mkt-card">
            <div className="mkt-flag">{mkt.flag}</div>

            <div className="mkt-score-big">{mkt.score}</div>
            <div className="mkt-score-lbl">/100 Opportunity Score</div>

            <span className={`mkt-grade ${mkt.grade}`}>{mkt.gradeLabel}</span>
            <div className="mkt-stats">
              <div className="mkt-stat">
                <div className="v">{mkt.marketSize}</div>
                <div className="l">시장 규모</div>
              </div>
              <div className="mkt-stat">
                <div className="v">{mkt.growth}</div>
                <div className="l">연 성장률</div>
              </div>
            </div>
          </div>
        ))}
      </div>
      <MemoBox style={{ marginTop: 14 }}>
        <b>다음 액션</b> · 독일/EU 유망도 최상. 유통 채널보다 인증·제품안전 서류 준비가 선행되어야 합니다.
      </MemoBox>
    </DiagnosisStage>
  )
}
