// S10 데이터 출처 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

export default function S10Sources({ pipes }) {
  return (
    <DiagnosisStage
      eyebrow="S10 PUBLIC DATA PIPELINE"
      title="화면별 공공데이터 출처 및 수집 상태"
      right={<button className="s-btn">파이프라인 실행</button>}
    >

      <div className="pipe-grid">
        {pipes.map((pipe) => {
          const status = pipe.status ?? 'fallback'
          const statusText = status === 'live' ? 'Live'
            : status === 'cache' ? 'Cache'
              : status === 'crawl' ? 'Crawl'
              : status === 'heuristic' ? 'Rule'
                : status === 'agent' ? 'Agent'
                  : status === 'user_input' ? 'User'
                    : 'Fallback'
          const chipType = status === 'live' || status === 'cache' ? 'ok'
            : status === 'crawl' || status === 'heuristic' || status === 'agent' || status === 'user_input' ? 'warn'
              : 'req'
          return (
          <div key={pipe.source} className="pipe-card">
            <div className="pipe-source">{pipe.source}</div>

            <div className="pipe-count">{pipe.count}</div>
            <div className="pipe-lbl">{pipe.label}</div>
            {pipe.metricSummary && (
              <div className="pipe-lbl">metric · {pipe.metricSummary}</div>
            )}

            <span className={`status-chip ${chipType}`}>{statusText}</span>
          </div>
          )
        })}

        <div className="pipe-card" style={{ borderStyle: 'dashed', background: '#fafbfe' }}>
          <div className="pipe-source" style={{ color: 'var(--slate-2)' }}>+ 커넥터 추가</div>
          <div style={{ fontSize: 12, color: 'var(--slate-2)', marginTop: 8, lineHeight: 1.5 }}>
            새 공공데이터 소스를 연결합니다
          </div>
          <span className="status-chip warn">대기중</span>
        </div>
      </div>

      <div style={{ fontSize: 11.5, color: 'var(--slate-2)', marginTop: 16, lineHeight: 1.6 }}>
        출처는 live/cache/fallback 공공데이터, crawl 공개 웹, heuristic 규칙 엔진을 구분합니다.
        fallback, crawl, heuristic은 “공공 API 연결됨”으로 해석하지 말고 보강 대상 여부를 확인하십시오.
      </div>
    </DiagnosisStage>
  )
}
