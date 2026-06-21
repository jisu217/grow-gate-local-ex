import { useNavigate, Link } from 'react-router-dom'
import styles from './Home.module.css'

// 파트너 목록
const PARTNERS = ['관세청', 'KOTRA', 'KTL', 'SafetyKorea', 'KIPRIS', '기업마당', 'EU']

// 기능 소개
const FEATURES = [
  {
    no: '01', icon: '🧩', title: '한 번의 진단',
    desc: '제품 정보를 넣으면 수출 가능성·규제 매칭·리콜·특허 위험을 한 화면에서 분석합니다.',
    link: '진단 시작',
  },
  {
    no: '02', icon: '📊', title: '근거가 보이는 데이터',
    desc: '관세청·KOTRA·SafetyKorea·KIPRIS·지원사업 → 어떤 공공데이터에서 나온 결론인지 그대로 보여줍니다.',
    link: '데이터 보기',
  },
  {
    no: '03', icon: '🗓️', title: '30·60·90일 할 일',
    desc: '국내·인증·해외에서 담당자가 언제 무엇을 해야 하는지, 산출물까지 일정별로 받습니다.',
    link: '로드맵 열기',
  },
]

// 수치 통계
const STATS = [
  { v: '7', u: '종', l: '연동 공공데이터 API' },
  { v: '6', u: '건', l: '자동 매칭 규제·인증' },
  { v: '83', u: '/100', l: '데모 수출 준비도' },
  { v: '90', u: '일', l: '실행 로드맵 자동 생성' },
]

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

  return (
    <>
      {/* 배경 레이어 */}
      <div className={styles.bgfx}>
        <div className={`${styles.aurora} ${styles.a1}`} />
        <div className={`${styles.aurora} ${styles.a2}`} />
        <div className={`${styles.aurora} ${styles.a3}`} />
        <div className={styles.bgGrid} />
      </div>

      <div className={styles.page}>

        {/* 내비게이션 */}
        <nav className={styles.nav}>
          <div className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></div>
          <div className={styles.navLinks}>
            <Link to="/" className={styles.active}>홈</Link>
            <Link to="/diagnosis">AI 진단 이력</Link>
            <Link to="/mypage">마이페이지</Link>
            <Link to="/login">로그인</Link>
          </div>
        </nav>

        {/* 히어로 */}
        <section className={styles.hero}>
          <HeroMap />

          {/* 출품 뱃지 */}
          <div className={styles.badge}>
            <span className={styles.badgeIc}>⚡</span>
            <span className={styles.badgeTx}>
              <b>2026 산업부 공공데이터 활용 공모전</b> · 출품작
            </span>
            <span className={styles.badgeAr}>→</span>
          </div>

          <h1 className={styles.h1}>
            수출, 어디서<br />
            막힐지 <span className={styles.hl}>먼저 압니다.</span>
          </h1>

          <p className={styles.lead}>
            제품명과 HS Code만 입력하면, AI가 공공데이터를 연결해<br />
            인증·규제·리콜·지원사업을{' '}
            <b style={{color:'var(--blue)'}}>오늘 할 업무 순서</b>로 정리합니다.
          </p>

          {/* CTA */}
          <div className={styles.heroCta}>
            <button className={`${styles.btn} ${styles.primary}`} onClick={() => navigate('/diagnosis')}>
              3분 만에 진단 시작 →
            </button>
            <button className={`${styles.btn} ${styles.ghost}`} onClick={() => navigate('/diagnosis/diag-001')}>
              데모 결과 먼저 보기
            </button>
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
          <h2 className={styles.secH2}>조회가 아니라, 실행입니다</h2>
          <p className={styles.secSub}>흩어진 공공데이터를 AI가 연결해, 기업이 바로 움직일 수 있는 결정으로 바꿉니다.</p>
          <div className={styles.featGrid}>
            {FEATURES.map(f => (
              <div key={f.no} className={styles.feat}>
                <div className={styles.featNo}>{f.no}</div>
                <div className={styles.featIc}>{f.icon}</div>
                <h3 className={styles.featH3}>{f.title}</h3>
                <p className={styles.featP}>{f.desc}</p>
                <span className={styles.featGo}>{f.link}</span>
              </div>
            ))}
          </div>
        </section>

        {/* 통계 섹션 */}
        <section className={styles.statsSec}>
          <div className={styles.stats}>
            {STATS.map(s => (
              <div key={s.l} className={styles.stat}>
                <div className={styles.statV}>{s.v}<span className={styles.statU}>{s.u}</span></div>
                <div className={styles.statL}>{s.l}</div>
              </div>
            ))}
          </div>
        </section>

        {/* CTA 배너 */}
        <section className={styles.ctaBand}>
          <h2 className={styles.ctaBandH2}>막막한 수출 준비,<br />오늘 첫 단추부터.</h2>
          <p className={styles.ctaBandP}>제품명과 HS Code만 있으면 됩니다. 5분 뒤, 손에 잡히는 실행 리포트가 됩니다.</p>
          <button
            className={`${styles.btn} ${styles.ctaBandBtn}`}
            onClick={() => navigate('/diagnosis')}
          >
            3분 만에 진단 시작 →
          </button>
        </section>

        {/* 푸터 */}
        <footer className={styles.footer}>
          <div className={styles.fBrand}>GROW-GATE <span style={{color:'var(--blue)'}}>Local EX</span></div>
          <div className={styles.fLinks}>
            <a href="/diagnosis">AI 진단 이력</a>
            <a href="#">데이터</a>
            <a href="#">실행 로드맵</a>
            <a href="#">문서</a>
          </div>
          <div className={styles.fNote}>
            2026 산업통상자원부 공공데이터 활용 공모전 · Regulation-to-Market Twin for Regional SMEs ·
            본 결과는 공공데이터 기반 AI 분석이며 최종 판단은 전문가와 함께 하십시오.
          </div>
        </footer>
      </div>
    </>
  )
}
