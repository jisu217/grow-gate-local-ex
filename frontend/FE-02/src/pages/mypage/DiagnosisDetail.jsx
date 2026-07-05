import { useState, useEffect } from 'react'
import { useParams, useNavigate, Link } from 'react-router-dom'
import styles from './DiagnosisDetail.module.css'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import { getDiagnosisList, getDiagnosisResult, buildDecisionBrief } from '../../services/diagnosisService.js'

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

// 점수 → 보완 우선순위
function toPriority(score) {
  if (score < 60) return 'high'
  if (score < 75) return 'medium'
  return 'ok'
}
const PRIORITY_META = {
  high: { label: '보완 우선', badge: 'pcHigh', fill: '#E8543E' },
  medium: { label: '검토 권장', badge: 'pcMed', fill: '#F1A100' },
  ok: { label: '충족', badge: 'pcOk', fill: '#12B886' },
}

// 판정 상태 → 색상 클래스
function gateClassOf(status) {
  const s = String(status ?? '').toUpperCase()
  if (s.includes('NO')) return 'gateNo'
  if (s.includes('GO') && !s.includes('CONDITION')) return 'gateGo'
  return 'gateCond'
}

// 판단 근거 · 보완 우선순위 — "왜 이 판정이고, 무엇을 먼저 고칠지"
export default function DiagnosisDetail() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const [state, setState] = useState('loading') // loading | empty | error | ok
  const [data, setData] = useState(null)
  const [animate, setAnimate] = useState(false)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let alive = true
    ;(async () => {
      try {
        const list = await getDiagnosisList()
        const runs = list
          .filter((d) => d.productId === productId)
          .sort((a, b) => String(b.requestedAt ?? '').localeCompare(String(a.requestedAt ?? '')))
        const target = runs.find((d) => d.status === 'done') ?? runs[0]
        if (!target) { if (alive) setState('empty'); return }

        const result = await getDiagnosisResult(target.id)
        if (!alive) return
        setData({ runId: target.id, name: target.productName, country: target.targetMarket, result })
        setState('ok')
        requestAnimationFrame(() => alive && setAnimate(true))
      } catch {
        if (alive) setState('error')
      }
    })()
    return () => { alive = false }
  }, [productId, reloadKey])

  if (state === 'loading') {
    return (
      <div className={styles.wrap}>
        <StatusMessage type="loading" message="판단 근거를 불러오는 중입니다." />
      </div>
    )
  }

  if (state === 'error') {
    return (
      <div className={styles.wrap}>
        <button className={styles.back} onClick={() => navigate('/mypage')}>← 마이페이지</button>
        <StatusMessage
          type="error"
          message="판단 근거를 불러오지 못했습니다. 네트워크를 확인하고 다시 시도해주세요."
          onAction={() => setReloadKey((k) => k + 1)}
          onSecondary={() => navigate('/mypage')}
          secondaryLabel="마이페이지로"
        />
      </div>
    )
  }

  if (state === 'empty') {
    return (
      <div className={styles.wrap}>
        <button className={styles.back} onClick={() => navigate('/mypage')}>← 마이페이지</button>
        <div className={styles.empty}>
          <p className={styles.emptyText}>아직 이 제품의 Gate 판단 결과가 없어요.<br />먼저 판단을 실행하면 근거를 볼 수 있어요.</p>
          <button className={styles.emptyBtn} onClick={() => navigate('/diagnosis/new')}>Gate 판단 시작하기 →</button>
        </div>
      </div>
    )
  }

  const { runId, name, country, result } = data
  const session = result.session ?? {}
  const stages = result.stages ?? {}
  const brief = buildDecisionBrief(session, stages)

  const rawScore = session.diagnosisScore
  const overall = typeof rawScore === 'number' ? rawScore : (rawScore?.overall ?? 0)
  const gateCls = gateClassOf(session.gateDecision?.status)

  // 영역별 점수 → 보완 우선순위 카드 (낮은 점수 = 우선순위 높음)
  const areas = (stages.s09?.scoreBars ?? []).map((b) => ({
    key: b.name,
    label: AREA_LABELS[b.name] ?? b.name,
    score: b.value,
    priority: toPriority(b.value),
  }))
  const areasSorted = [...areas].sort((a, b) => a.score - b.score)

  // 가장 먼저 고칠 것: 견적 전 필수 → 없으면 보완 우선 영역
  const mustFix = Array.isArray(session.gateDecision?.mustFixBeforeQuote)
    ? session.gateDecision.mustFixBeforeQuote.filter(Boolean)
    : []
  const firstFix = mustFix.length
    ? mustFix
    : areasSorted.filter((a) => a.priority === 'high').map((a) => `${a.label} 보완 (${a.score}점)`)

  return (
    <div className={styles.wrap}>
      <div className={styles.topbar}>
        <button className={styles.back} onClick={() => navigate('/mypage')}>← 마이페이지</button>
        <span className={styles.crumb}>/ 판단 근거 · 보완 우선순위</span>
      </div>

      {/* 1. 현재 판단 요약 */}
      <div className={styles.summaryCard}>
        <div className={styles.metaTop}>{name}{country ? ` · ${country} 수출` : ''}</div>
        <div className={styles.gateRow}>
          <span className={`${styles.gateBadge} ${styles[gateCls]}`}>{brief.gateLabel}</span>
          <span className={styles.refScore}>참고 점수 <b>{overall}</b>/100</span>
        </div>
        <p className={styles.gateReason}>{brief.gateReason}</p>
      </div>

      {/* 2. 가장 먼저 고칠 것 */}
      {firstFix.length > 0 && (
        <div className={styles.firstFix}>
          <div className={styles.firstFixTitle}>⚠ 가장 먼저 고칠 것 (견적 전)</div>
          <div className={styles.firstFixList}>
            {firstFix.slice(0, 3).map((item, i) => (
              <div key={i} className={styles.firstFixItem}>
                <span className={styles.firstFixNum}>{i + 1}</span>
                <span>{item}</span>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* 3. 영역별 보완 우선순위 */}
      <div className={styles.sectionHead}>영역별 보완 우선순위 <span className={styles.hint}>점수 낮은 영역부터</span></div>
      <div className={styles.priorityGrid}>
        {areasSorted.map((a) => {
          const meta = PRIORITY_META[a.priority]
          return (
            <div key={a.key} className={`${styles.priorityCard} ${a.priority === 'high' ? styles.high : ''}`}>
              <div className={styles.pcTop}>
                <span className={styles.pcLabel}>{a.label}</span>
                <span className={`${styles.pcBadge} ${styles[meta.badge]}`}>{meta.label}</span>
              </div>
              <div className={styles.pcTrack}>
                <div className={styles.pcFill} style={{ width: animate ? `${a.score}%` : 0, background: meta.fill }} />
              </div>
              <div className={styles.pcScore}>현재 {a.score}점</div>
            </div>
          )
        })}
      </div>

      {/* 4. 항목별 근거 데이터 */}
      <div className={styles.sectionHead}>이 판단은 어떤 데이터로 나왔나 <span className={styles.hint}>영역별 공공데이터 출처</span></div>
      <div className={styles.srcCard}>
        {areasSorted.map((a) => (
          <div key={a.key} className={styles.srcRow}>
            <span className={styles.srcArea}>{a.label}</span>
            <span className={styles.srcName}>{AREA_SOURCES[a.key] ?? '공공데이터 기반 산출'}</span>
          </div>
        ))}
      </div>

      {/* 5·6. 다음 단계 */}
      <div className={styles.sectionHead}>다음 단계</div>
      <div className={styles.nextGrid}>
        <Link to={`/mypage/knowledge/${productId}`} className={styles.nextCard}>
          <div className={styles.nextCardTitle}>제품 정보 보완하기</div>
          <p className={styles.nextCardDesc}>보완 우선 영역의 인증·FTO·바이어 요구사항을 제품 정보에 채우면 다음 판단이 좋아져요.</p>
          <span className={styles.nextCardArr}>제품 정보로 이동 →</span>
        </Link>
        <button type="button" className={styles.nextCard} onClick={() => navigate(`/diagnosis/${runId}`)}>
          <div className={styles.nextCardTitle}>전체 판단 결과 보기</div>
          <p className={styles.nextCardDesc}>리스크별 판단 이유·출처·실행 로드맵을 전체 판단 결과에서 확인할 수 있어요.</p>
          <span className={styles.nextCardArr}>전체 결과 보기 →</span>
        </button>
      </div>

      <p className={styles.disclaimer}>
        2026 산업통상부 공공데이터 활용 공모전 · 본 결과는 공공데이터 기반 AI 분석이며 최종 판단은 전문가와 함께 하십시오.
      </p>
    </div>
  )
}
