import DiagnosisStage from './DiagnosisStage.jsx'

export default function S09Report({ scoreBars }) {
  return (
    <DiagnosisStage
      eyebrow="S09 EXPORT READINESS REPORT"
      title="분석 결과 요약 · 항목별 점수"
      right={<button className="s-btn">PDF 내보내기 ↓</button>}
    >

      <div className="rep">

        <div className="doc" style={{ border: '1px solid var(--line)' }}>

          <p className="ai-summary">
            경남 배터리테크의 리튬이온 배터리팩(HS 850760)은 독일/EU 시장 진출 기준 종합 준비도{' '}
            <b>83점 (HIGH)</b>으로 평가됩니다. UN38.3 운송시험 미비가 가장 긴급한 리스크이며, CE/EMC 및 EU 배터리 여권은 병행 준비가 필요합니다.
            KOTRA 해외규격인증 지원사업 신청 마감(D-14)이 임박해 즉시 착수를 권고합니다.
          </p>

          <div className="highlight-list">
            <div className="hl-item"><b>최우선:</b> UN38.3 시험 즉시 의뢰 → KTL 경남지원센터 연결 추천</div>
            <div className="hl-item"><b>규제:</b> EU 배터리 여권 초안 착수 → 12주 소요 예상, 지금 시작해야 Q3 수출 가능</div>
            <div className="hl-item"><b>특허:</b> 삼성SDI BMS 특허 고위험 → FTO 전문가 검토 1~2주 내 착수 권고</div>
            <div className="hl-item"><b>지원:</b> 5개 매칭 중 KOTRA 수출바우처 + 해외규격인증 동시 신청 가능</div>
          </div>

          <div className="guardrail">
            ⚠ 모든 규제 답변에는 추천·최신성·전문가 확인 여부를 함께 확인하십시오. 본 결과는 공공데이터 기반 AI 분석이며, 최종 판단은 인증 전문가와 함께 하십시오.
          </div>
        </div>

        <div className="side">
          <div className="sb-list">
            {scoreBars.map((item) => (
              <div key={item.name} className="sb-item">

                <div className="sb-top">
                  <span className="sb-name">{item.name}</span>
                  <span className="sb-val">
                    {item.value}
                    <small style={{ color: 'var(--slate-2)', fontSize: 11 }}>/100</small>
                  </span>
                </div>

                <div className="sb-track">
                  <div
                    className="sb-fill"
                    style={{
                      width: `${item.value}%`,
                      ...(item.amber ? { background: 'var(--amber)' } : {}),
                    }}
                  />
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </DiagnosisStage>
  )
}
