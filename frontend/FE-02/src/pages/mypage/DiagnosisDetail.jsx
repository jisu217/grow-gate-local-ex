import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import styles from './DiagnosisDetail.module.css'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import { getDiagnosisList, getDiagnosisResult } from '../../services/diagnosisService.js'

// 진단 점수 축(영어) → 한글 라벨
const AREA_LABELS = {
  Market: '시장성',
  Regulation: '규제·인증',
  Recall: '리콜 위험',
  FTO: '특허·FTO',
  Support: '지원사업',
}
// 진단 점수 축 → 대표 공공데이터 출처 (설명용)
const AREA_SOURCES = {
  Market: '관세청 수출입무역통계 · KOTRA 시장 인사이트',
  Regulation: 'SafetyKorea 제품 안전인증 정보',
  Recall: 'SafetyKorea 리콜 정보',
  FTO: 'KIPRIS 특허/FTO',
  Support: '기업마당·수출바우처 지원사업',
}

// 종합 점수로 등급 추정 (백엔드 trustGrade가 없을 때)
function toGrade(score) {
  if (score >= 80) return 'A'
  if (score >= 60) return 'B'
  return 'C'
}

// 진단 점수 근거 상세 — "왜 이 점수인지"를 영역별 감점(100-점수)으로 설명
export default function DiagnosisDetail() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const [state, setState] = useState('loading') // loading | empty | error | ok
  const [data, setData] = useState(null)
  const [animate, setAnimate] = useState(false)

  useEffect(() => {
    let alive = true
    ;(async () => {
      try {
        const list = await getDiagnosisList()
        // 이 제품의 진단만 추려 최신순 정렬 (완료된 진단 우선)
        const runs = list
          .filter((d) => d.productId === productId)
          .sort((a, b) => String(b.requestedAt ?? '').localeCompare(String(a.requestedAt ?? '')))
        const target = runs.find((d) => d.status === 'done') ?? runs[0]
        if (!target) { if (alive) setState('empty'); return }

        const result = await getDiagnosisResult(target.id)
        if (!alive) return
        setData({
          runId: target.id,
          name: target.productName,
          country: target.targetMarket,
          result,
        })
        setState('ok')
        requestAnimationFrame(() => alive && setAnimate(true))
      } catch {
        if (alive) setState('error')
      }
    })()
    return () => { alive = false }
  }, [productId])

  if (state === 'loading') {
    return (
      <div className={styles.wrap}>
        <StatusMessage type="loading" message="진단 점수 근거를 불러오는 중입니다." />
      </div>
    )
  }

  if (state === 'error') {
    return (
      <div className={styles.wrap}>
        <button className={styles.back} onClick={() => navigate('/mypage')}>← 마이페이지</button>
        <StatusMessage type="error" message="점수 근거를 불러오지 못했습니다. 잠시 후 다시 시도해주세요." />
      </div>
    )
  }

  // 이 제품의 진단이 아직 없는 경우
  if (state === 'empty') {
    return (
      <div className={styles.wrap}>
        <button className={styles.back} onClick={() => navigate('/mypage')}>← 마이페이지</button>
        <div className={styles.empty}>
          <p className={styles.emptyText}>아직 이 제품의 Gate 판단 결과가 없어요.<br />먼저 판단을 실행하면 점수 근거를 볼 수 있어요.</p>
          <button className={styles.emptyBtn} onClick={() => navigate('/diagnosis/new')}>Gate 판단 시작하기 →</button>
        </div>
      </div>
    )
  }

  const { runId, name, country, result } = data
  const rawScore = result.session?.diagnosisScore
  const overall = typeof rawScore === 'number' ? rawScore : (rawScore?.overall ?? 0)
  const grade = result.session?.trustGrade ?? toGrade(overall)
  const summary = result.stages?.s09?.summary || '공공데이터와 제품 지식을 종합해 산출한 수출 준비도 점수입니다.'
  const missingDocs = (result.session?.missingFields ?? []).filter(Boolean)

  // 영역별 점수 → 감점(100-점수) 계산, 감점 큰 순 정렬
  const bars = (result.stages?.s09?.scoreBars ?? []).map((b) => ({
    key: b.name,
    label: AREA_LABELS[b.name] ?? b.name,
    score: b.value,
    loss: Math.max(0, 100 - b.value),
  }))
  const barsSorted = [...bars].sort((a, b) => b.loss - a.loss)
  const totalLost = Math.round(100 - overall)
  const top = barsSorted[0]

  return (
    <div className={styles.wrap}>
      <div className={styles.topbar}>
        <button className={styles.back} onClick={() => navigate('/mypage')}>← 마이페이지</button>
        <span className={styles.crumb}>/ 점수 근거</span>
      </div>

      <div className={styles.card}>
        {/* 점수 */}
        <div className={styles.head}>
          <div>
            <div className={styles.metaTop}>{name}{country ? ` · ${country} 수출` : ''}</div>
            <div className={styles.big}>{overall}<small> / 100</small></div>
          </div>
          <div className={styles.right}>
            <span className={styles.grade}>등급 {grade}</span>
            <p className={styles.summary}>{summary}</p>
          </div>
        </div>

        {/* 한 줄 판단 */}
        {top && (
          <div className={styles.verdict}>
            <span className={styles.icn}>ⓘ</span>
            <p>
              100점 중 <b>약 {totalLost}점</b>이 빠졌어요. 가장 크게 깎은 항목은{' '}
              <b>{top.label} (−{top.loss.toFixed(0)}점)</b> — 점수가 낮은 영역일수록 더 많이 깎여요.
            </p>
          </div>
        )}

        {/* 영역별 점수 — 감점 큰 순 */}
        <div className={styles.bd}>
          <div className={styles.bdTitle}>
            <span>영역별 점수 · 감점 큰 순 정렬</span>
            <span className={styles.note}>위에 있을수록 점수를 많이 깎은 항목</span>
          </div>

          {barsSorted.map((b, i) => {
            const weak = b.score < 70
            const barColor = weak ? 'var(--blue)' : '#C3CCDA'
            return (
              <div key={b.key} className={styles.row}>
                <div className={styles.name}>
                  <span className={styles.lkey}>
                    {b.label}{i === 0 && b.loss > 0 && <em className={styles.impact}>가장 큰 원인</em>}
                  </span>
                  <span className={styles.lsub}>현재 {b.score}점</span>
                </div>
                <div className={styles.track}>
                  <div className={styles.fill} style={{ width: animate ? `${b.score}%` : 0, background: barColor }} />
                </div>
                <div className={styles.loss}>−{b.loss.toFixed(0)}<small>점</small></div>
              </div>
            )
          })}

          <div className={styles.legend}>
            <span><i style={{ background: 'var(--blue)' }} />보완 우선 (낮은 점수)</span>
            <span><i style={{ background: '#C3CCDA' }} />충족</span>
          </div>
        </div>
      </div>

      {/* 근거 데이터 — 영역별 점수가 어떤 공공데이터로 매겨졌나 */}
      <div className={styles.srcCard}>
        <div className={styles.srcTitle}>이 점수는 어떤 데이터로 매겼나</div>
        {barsSorted.map((b) => (
          <div key={b.key} className={styles.srcRow}>
            <span className={styles.srcArea}>{b.label}</span>
            <span className={styles.srcName}>📊 {AREA_SOURCES[b.key] ?? '공공데이터 기반 산출'}</span>
          </div>
        ))}
      </div>

      {/* 점수 올리는 법 */}
      {missingDocs.length > 0 && (
        <div className={styles.improve}>
          <div className={styles.improveTitle}>이 점수를 올리려면</div>
          <p className={styles.improveDesc}>아래 정보를 보완하면 규제·인증과 문서 완성도 점수가 올라가요.</p>
          <div className={styles.docs}>
            {missingDocs.map((d) => <span key={d} className={styles.doc}>{d}</span>)}
          </div>
          <Link to={`/mypage/knowledge/${productId}`} className={styles.improveLink}>📦 제품 지식에서 보완하기 →</Link>
        </div>
      )}

      {/* 전체 진단으로 */}
      <div className={styles.more}>
        <div className={styles.moreT}>
          상세 근거가 더 궁금하다면
          <span>리스크별 판단 이유와 출처를 전체 Gate 판단에서 볼 수 있어요.</span>
        </div>
        <button className={styles.moreBtn} onClick={() => navigate(`/diagnosis/${runId}`)}>
          Gate 판단 전체 결과 보기 →
        </button>
      </div>

      <p className={styles.disclaimer}>
        2026 산업통상부 공공데이터 활용 공모전 · 본 결과는 공공데이터 수집 신호와 규칙 기반 Gate 판단이며 최종 판단은 전문가와 함께 하십시오.
      </p>
    </div>
  )
}
