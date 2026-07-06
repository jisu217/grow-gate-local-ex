import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styles from './Home.module.css'
import TopNav from '../../components/layout/TopNav.jsx'
import { getPublicDataDashboard } from '../../services/publicDataService.js'
import {
  HOME_BEFORE_AFTER,
  HOME_FEATURES,
  HOME_READINESS,
  PARTNERS,
  buildHomeStats,
  buildHomeStatsFallback,
} from './homePresenter.js'

// 수출 네트워크 SVG 맵 컴포넌트
function HeroMap() {
  return (
    <svg className={styles.heromap} viewBox="0 0 720 560" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="g" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0" stopColor="#3a78ff" />
          <stop offset="1" stopColor="#3ad99a" />
        </linearGradient>
      </defs>
      <ellipse className={styles.blob} cx="200" cy="210" rx="160" ry="82" />
      <ellipse className={styles.blob} cx="500" cy="250" rx="180" ry="100" />
      <ellipse className={styles.blob} cx="450" cy="430" rx="130" ry="66" />
      <path className={styles.dash} d="M500 250 C 380 110, 260 150, 200 195" />
      <path className={styles.dash} style={{animationDelay:'.5s'}} d="M500 250 C 340 230, 250 250, 170 252" />
      <path className={styles.dash} style={{animationDelay:'1s'}}  d="M500 250 C 500 340, 480 400, 450 420" />
      <path className={styles.dash} style={{animationDelay:'1.5s'}} d="M500 250 C 580 300, 620 350, 640 372" />
      <circle className={`${styles.node} ${styles.hub}`} cx="500" cy="250" r="9" />
      <circle className={styles.node} cx="200" cy="195" r="6" />
      <circle className={styles.node} cx="170" cy="252" r="6" />
      <circle className={styles.node} cx="450" cy="420" r="6" />
      <circle className={styles.node} cx="640" cy="372" r="6" />
      <text className={styles.lbl} x="514" y="246">KOREA</text>
      <text className={styles.lbl} x="170" y="183">GERMANY</text>
      <text className={styles.lbl} x="142" y="272">US</text>
      <text className={styles.lbl} x="422" y="442">ASEAN</text>
      <text className={styles.lbl} x="606" y="394">JAPAN</text>
    </svg>
  )
}

// 랜딩 홈 페이지 컴포넌트
export default function Home() {
  const navigate = useNavigate()
  const [activeFeature, setActiveFeature] = useState(null)
  const [statsState, setStatsState] = useState('loading')
  const [stats, setStats] = useState(() => buildHomeStatsFallback())
  const dialogRef = useRef(null)
  const lastActiveRef = useRef(null)

  useEffect(() => {
    let alive = true
    getPublicDataDashboard()
      .then((dashboard) => {
        if (!alive) return
        setStats(buildHomeStats(dashboard))
        setStatsState('ready')
      })
      .catch(() => {
        if (!alive) return
        setStats(buildHomeStatsFallback())
        setStatsState('error')
      })
    return () => {
      alive = false
    }
  }, [])

  useEffect(() => {
    if (!activeFeature) return undefined
    lastActiveRef.current = document.activeElement
    const dialog = dialogRef.current
    const closeButton = dialog?.querySelector('button')
    closeButton?.focus()

    const onKeyDown = (event) => {
      if (event.key === 'Escape') closeFeature()
      if (event.key !== 'Tab' || !dialog) return
      const focusables = dialog.querySelectorAll('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])')
      if (!focusables.length) return
      const first = focusables[0]
      const last = focusables[focusables.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }

    window.addEventListener('keydown', onKeyDown)
    return () => {
      window.removeEventListener('keydown', onKeyDown)
      lastActiveRef.current?.focus?.()
    }
  }, [activeFeature])

  const openFeature = (feature) => setActiveFeature(feature)
  const closeFeature = () => setActiveFeature(null)

  return (
    <>
      {/* 배경 레이어 */}
      <div className={styles.bgfx}>
        <div className={`${styles.aurora} ${styles.a1}`} />
        <div className={`${styles.aurora} ${styles.a2}`} />
        <div className={`${styles.aurora} ${styles.a3}`} />
        <div className={styles.bgGrid} />
      </div>

      {/* 공통 고정 내비게이션 */}
      <TopNav />

      <div className={styles.page}>

        {/* 히어로 */}
        <section className={styles.hero}>
          <HeroMap />

          {/* 출품 뱃지 */}
          <div className={styles.badge}>
            <span className={styles.badgeIc}>EX</span>
            <span className={styles.badgeTx}>
              <b>2026 산업부 공공데이터 활용 공모전</b> · 출품작
            </span>
            <span className={styles.badgeAr}>심사용 메인</span>
          </div>

          <h1 className={styles.h1}>
            지역 제조기업의<br />
            수출 막힘을 <span className={styles.hl}>근거로 풉니다.</span>
          </h1>

          <p className={styles.lead}>
            제품명과 HS Code를 입력하면 공공데이터 수집 상태, 제품 정보, 인증·특허·리콜 신호를 연결해
            <b style={{color:'var(--blue)'}}> Gate 판단·근거·업무 보드</b>로 정리합니다.
          </p>

          {/* CTA — 시작 동선은 '진단 시작'(제품 입력) 하나로 통일 */}
          <div className={styles.heroCta}>
            <button className={`${styles.btn} ${styles.primary}`} onClick={() => navigate('/diagnosis/new')}>
              제품 등록하고 Gate 판단 →
            </button>
            <button className={`${styles.btn} ${styles.ghost}`} onClick={() => navigate('/diagnosis')}>
              내 진단 이력 보기
            </button>
          </div>

          <div className={styles.readiness}>
            {HOME_READINESS.map(item => (
              <div key={item.k} className={styles.readyItem}>
                <span className={styles.readyKey}>{item.k}</span>
                <span className={styles.readyValue}>{item.v}</span>
              </div>
            ))}
          </div>
        </section>

        {/* 파트너 바 */}
        <div className={styles.trust}>
          <span className={styles.trustT}>연동 공공데이터 ∨</span>
          {PARTNERS.map(p => <span key={p} className={styles.trustSrc}>{p}</span>)}
        </div>

        {/* 기능 섹션 */}
        <section className={styles.sec}>
          <div className={styles.secEyebrow}>How it works</div>
          <h2 className={styles.secH2}>조회가 아니라, 실행 근거입니다</h2>
          <p className={styles.secSub}>공공데이터 상태와 제품 정보를 연결해, 기업이 바로 검토할 실행 후보로 정리합니다.</p>
          <div className={styles.featGrid}>
            {HOME_FEATURES.map(f => (
              <button key={f.no} type="button" className={styles.feat} onClick={() => openFeature(f)}>
                <div className={styles.featNo}>{f.no}</div>
                <div className={styles.featIc}>{f.icon}</div>
                <h3 className={styles.featH3}>{f.title}</h3>
                <p className={styles.featP}>{f.description}</p>
                <span className={styles.detailHint}>{f.actionLabel}</span>
              </button>
            ))}
          </div>
        </section>

        <section className={styles.compareSec}>
          <div className={styles.secEyebrow}>Before / After</div>
          <h2 className={styles.secH2}>기존 체크리스트와 다른 점</h2>
          <div className={styles.compareGrid}>
            {HOME_BEFORE_AFTER.map((row, index) => (
              <div key={row.before} className={styles.compareRow}>
                <div className={styles.beforeCard}>
                  <span>Before</span>
                  <p>{row.before}</p>
                </div>
                <div className={styles.flowArrow} aria-hidden="true">→</div>
                <div className={styles.afterCard}>
                  <span>After</span>
                  <p>{row.after}</p>
                  <div className={styles.afterBadges}>
                    {row.afterOutputs.map((output) => (
                      <span key={`${index}-${output}`}>{output}</span>
                    ))}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* 통계 섹션 */}
        <section className={styles.statsSec}>
          <div className={styles.stats}>
            {stats.map(s => (
              <div key={s.label} className={`${styles.stat} ${styles[s.tone] ?? ''}`}>
                <div className={styles.statV}>{statsState === 'loading' ? '확인 중' : s.value}</div>
                <div className={styles.statL}>{s.label}</div>
                <div className={styles.statHelp}>
                  {statsState === 'loading' ? '최근 수집 상태 확인 중' : s.helper}
                </div>
              </div>
            ))}
          </div>
        </section>

        {/* CTA 배너 */}
        <section className={styles.ctaBand}>
          <h2 className={styles.ctaBandH2}>막막한 수출 준비,<br />오늘 첫 단추부터.</h2>
          <p className={styles.ctaBandP}>제품명, HS Code, 바이어 문의만 있으면 됩니다. 견적 전 확인할 Gate와 업무 보드가 나옵니다.</p>
          <button
            className={`${styles.btn} ${styles.ctaBandBtn}`}
            onClick={() => navigate('/diagnosis/new')}
          >
            Gate 판단 시작 →
          </button>
        </section>

        {/* 푸터 */}
        <footer className={styles.footer}>
          <div className={styles.fBrand}>GROW-GATE <span style={{color:'var(--blue)'}}>Local EX</span></div>
          <div className={styles.fLinks}>
            <Link to="/diagnosis">수출 Gate 판단</Link>
            <Link to="/public-data/daily-refresh">공공데이터</Link>
            <Link to="/mypage">마이페이지</Link>
          </div>
          <div className={styles.fNote}>
            2026 산업통상자원부 공공데이터 활용 공모전 · Regulation-to-Market Twin for Regional SMEs ·
            본 결과는 공공데이터 수집 신호와 규칙 기반 점검 결과이며 담당자 의사결정은 전문가 검토와 함께 하십시오.
          </div>
        </footer>
      </div>
      {activeFeature && (
        <div className={styles.modalLayer} onMouseDown={closeFeature}>
          <div
            ref={dialogRef}
            className={styles.featureDialog}
            role="dialog"
            aria-modal="true"
            aria-labelledby="feature-dialog-title"
            onMouseDown={(event) => event.stopPropagation()}
          >
            <button type="button" className={styles.modalClose} onClick={closeFeature} aria-label="닫기">×</button>
            <span className={styles.modalKicker}>자세히 보기</span>
            <h3 id="feature-dialog-title">{activeFeature.modal.title}</h3>
            <p className={styles.modalLead}>{activeFeature.modal.description}</p>
            <dl className={styles.modalFacts}>
              <dt>입력값</dt>
              <dd>{activeFeature.modal.input}</dd>
              <dt>결과물</dt>
              <dd>{activeFeature.modal.result}</dd>
              <dt>연결 화면</dt>
              <dd>{activeFeature.modal.screen}</dd>
              <dt>주의</dt>
              <dd>{activeFeature.modal.caution}</dd>
            </dl>
            <div className={styles.modalActions}>
              <button type="button" className={`${styles.btn} ${styles.primary}`} onClick={() => navigate('/diagnosis/new')}>
                Gate 판단 시작
              </button>
              <button type="button" className={`${styles.btn} ${styles.ghost}`} onClick={closeFeature}>
                닫기
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
