// S09 최종 리포트 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

export default function S09Report({ scoreBars, summary, onDownload, fastApiUsed }) {
  return (
    <DiagnosisStage
      eyebrow="S09 EXPORT READINESS REPORT"
      title="분석 결과 요약 · 항목별 점수"
      right={<button className="s-btn" onClick={onDownload}>Markdown 다운로드 ↓</button>}
    >

      <div className="rep">

        <div className="doc" style={{ border: '1px solid var(--line)' }}>

          <p className="ai-summary">
            {summary || '백엔드 진단 결과를 기반으로 수출 준비도 리포트를 생성했습니다.'}
          </p>

          <div className="highlight-list">
            <div className="hl-item"><b>산출물:</b> {fastApiUsed ? '선택형 report bridge 결과를 병합한 Markdown 바이어 대응 메모입니다.' : 'Spring 로컬 생성기의 Markdown 바이어 대응 메모입니다.'}</div>
            <div className="hl-item"><b>보관:</b> PDF가 필요하면 다운로드한 Markdown을 HTML 미리보기 또는 브라우저 인쇄로 보관합니다.</div>
            <div className="hl-item"><b>근거:</b> 점수별 source trace와 fallback 여부를 함께 확인할 수 있습니다.</div>
            <div className="hl-item"><b>구분:</b> 공공데이터 원문 근거, 정규화/필터링 결과, AI 보조 해석, 사람이 최종 확인해야 하는 항목을 분리해 확인하십시오.</div>
            <div className="hl-item"><b>실행:</b> 30·60·90일 보드의 태스크를 기준으로 담당자와 산출물을 배정하십시오.</div>
          </div>

          <div className="guardrail">
            본 내용은 자동 생성된 의사결정 보조 정보이며, 법률/인증/수출 실행 여부의 최종 판단이 아닙니다. fallback/seed는 live 공공 API 결과처럼 표시하지 않으며, 최종 판단은 인증 전문가·변리사·법률 전문가와 함께 하십시오.
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
