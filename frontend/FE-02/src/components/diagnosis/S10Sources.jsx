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
        {pipes.map((pipe) => (
          <div key={pipe.source} className="pipe-card">
            <div className="pipe-source">{pipe.source}</div>

            <div className="pipe-count">{pipe.count}</div>
            <div className="pipe-lbl">{pipe.label}</div>

            <span className="status-chip ok">Connected</span>
          </div>
        ))}

        <div className="pipe-card" style={{ borderStyle: 'dashed', background: '#fafbfe' }}>
          <div className="pipe-source" style={{ color: 'var(--slate-2)' }}>+ 커넥터 추가</div>
          <div style={{ fontSize: 12, color: 'var(--slate-2)', marginTop: 8, lineHeight: 1.5 }}>
            새 공공데이터 소스를 연결합니다
          </div>
          <span className="status-chip warn">대기중</span>
        </div>
      </div>

      <div style={{ fontSize: 11.5, color: 'var(--slate-2)', marginTop: 16, lineHeight: 1.6 }}>
        출처 · 관세청 · KTL Certinfo · SafetyKorea · KOTRA · KIPRIS · 기업마당 · EU RAPEX.
        모든 규제 답변에는 추천·최신성·전문가 확인 여부를 함께 확인하십시오.
      </div>
    </DiagnosisStage>
  )
}
