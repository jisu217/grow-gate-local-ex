import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import styles from './DiagnosisDetail.module.css'
import { getMypageProduct } from '../../services/mypageService.js'
import { BREAKDOWN_SOURCES } from '../../mock/mypageData.js'

// 진단 점수 근거 상세 — "왜 이 점수인지"를 영역별 감점(비중×부족분)으로 설명
export default function DiagnosisDetail() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const [product, setProduct] = useState(undefined) // undefined=로딩, null=없음
  const [animate, setAnimate] = useState(false)

  useEffect(() => {
    getMypageProduct(productId).then((p) => {
      setProduct(p ?? null)
      requestAnimationFrame(() => setAnimate(true))
    })
  }, [productId])

  if (product === undefined) return null

  // 진단 결과(상세)가 없는 경우
  if (!product || !product.detail) {
    return (
      <div className={styles.wrap}>
        <Link to="/mypage" className={styles.back}>← 마이페이지</Link>
        <div className={styles.empty}>
          아직 이 제품의 진단 결과가 없어요.<br />마이페이지에서 AI 진단을 먼저 실행해주세요.
        </div>
      </div>
    )
  }

  const { detail, score, name, hs, country, runId } = product
  const { grade, summary, breakdown, missingDocs } = detail

  // 영역별 '감점' = 비중 × 부족분(100-점수) → 종합 점수가 100에서 깎인 양
  // 감점 큰 순으로 정렬하면 "점수를 깎은 진짜 원인"이 한눈에 보인다
  const lost = breakdown.map((b) => ({ ...b, loss: (b.weight * (100 - b.score)) / 100 }))
  const lostSorted = [...lost].sort((a, b) => b.loss - a.loss)
  const totalLost = Math.round(100 - score)
  const top = lostSorted[0]

  return (
    <div className={styles.wrap}>
      {/* 상단 */}
      <div className={styles.topbar}>
        <Link to="/mypage" className={styles.back}>← 마이페이지</Link>
        <span className={styles.crumb}>/ 점수 근거</span>
      </div>

      <div className={styles.card}>
        {/* 점수 */}
        <div className={styles.head}>
          <div>
            <div className={styles.metaTop}>{name} · HS {hs} · {country} 수출</div>
            <div className={styles.big}>{score}<small> / 100</small></div>
          </div>
          <div className={styles.right}>
            <span className={styles.grade}>등급 {grade}</span>
            <p className={styles.summary}>{summary}</p>
          </div>
        </div>

        {/* 한 줄 판단: 100점에서 몇 점이 빠졌고, 주범이 무엇인지 */}
        <div className={styles.verdict}>
          <span className={styles.icn}>ⓘ</span>
          <p>
            100점 중 <b>약 {totalLost}점</b>이 빠졌어요. 가장 크게 깎은 항목은{' '}
            <b>{top.key} (−{top.loss.toFixed(1)}점)</b> — 비중 큰 영역의 점수가 낮을수록 더 많이 깎여요.
          </p>
        </div>

        {/* 감점 분해 — 어디서 점수가 빠졌나 (감점 큰 순) */}
        <div className={styles.bd}>
          <div className={styles.bdTitle}>
            <span>영역별 점수 · 감점 큰 순 정렬</span>
            <span className={styles.note}>위에 있을수록 점수를 많이 깎은 항목</span>
          </div>

          {lostSorted.map((b, i) => {
            const weak = b.score < 78
            const barColor = weak ? '#3B5BFE' : '#C3CCDA'
            return (
              <div key={b.key} className={styles.row}>
                <div className={styles.name}>
                  <span className={styles.lkey}>
                    {b.key}{i === 0 && <em className={styles.impact}>가장 큰 원인</em>}
                  </span>
                  <span className={styles.lsub}>비중 {b.weight}% · 현재 {b.score}점</span>
                </div>
                <div className={styles.track}>
                  <div className={styles.fill} style={{ width: animate ? `${b.score}%` : 0, background: barColor }} />
                </div>
                <div className={styles.loss}>−{b.loss.toFixed(1)}<small>점</small></div>
              </div>
            )
          })}

          <div className={styles.legend}>
            <span><i style={{ background: '#3B5BFE' }} />보완 우선 (낮은 점수)</span>
            <span><i style={{ background: '#C3CCDA' }} />충족</span>
          </div>
        </div>
      </div>

      {/* 근거 데이터 — 영역별 점수가 어떤 공공데이터로 매겨졌나 (감점 큰 순) */}
      <div className={styles.srcCard}>
        <div className={styles.srcTitle}>이 점수는 어떤 데이터로 매겼나</div>
        {lostSorted.map((b) => (
          <div key={b.key} className={styles.srcRow}>
            <span className={styles.srcArea}>{b.key}</span>
            <span className={styles.srcName}>📊 {BREAKDOWN_SOURCES[b.key]}</span>
          </div>
        ))}
      </div>

      {/* 점수 올리는 법 (보완 → 행동 연결) */}
      <div className={styles.improve}>
        <div className={styles.improveTitle}>이 점수를 올리려면</div>
        <p className={styles.improveDesc}>아래 문서를 보완하면 규제·인증과 문서완성도 점수가 올라가요.</p>
        <div className={styles.docs}>
          {missingDocs.map((d) => <span key={d} className={styles.doc}>{d}</span>)}
        </div>
        <Link to="/mypage/knowledge" className={styles.improveLink}>📦 제품 지식 관리에서 보완하기 →</Link>
      </div>

      {/* 전체 진단으로 */}
      {runId && (
        <div className={styles.more}>
          <div className={styles.moreT}>
            상세 근거가 더 궁금하다면
            <span>리스크별 판단 이유와 출처를 전체 AI 진단에서 볼 수 있어요.</span>
          </div>
          <button className={styles.moreBtn} onClick={() => navigate(`/diagnosis/${runId}`)}>
            AI 진단 결과 보기 →
          </button>
        </div>
      )}

      <p className={styles.disclaimer}>
        2026 산업통상부 공공데이터 활용 공모전 · 본 결과는 공공데이터 기반 AI 분석이며 최종 판단은 전문가와 함께 하십시오.
      </p>
    </div>
  )
}
