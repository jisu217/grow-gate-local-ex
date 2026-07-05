import { useNavigate } from 'react-router-dom'
import styles from './Home.module.css'
import TopNav from '../../components/layout/TopNav.jsx'

// 파트너 목록
const PARTNERS = ['관세청', 'KOTRA', 'KTL', 'SafetyKorea', 'KIPRIS', '기업마당', 'EU']

// 기능 소개
const FEATURES = [
  {
    no: '01', icon: '01', title: '한 번의 Gate 판단',
    desc: '제품 정보, HS Code, 대상국, 바이어 문의를 기준으로 견적 전 진행 가능 여부를 한 화면에서 점검합니다.',
    link: 'Gate 판단',
  },
  {
    no: '02', icon: '02', title: '근거가 보이는 데이터',
    desc: '관세청·KOTRA·SafetyKorea·KIPRIS·지원사업의 live/cache/fallback 출처를 진단 결과에 남깁니다.',
    link: '데이터 보기',
  },
  {
    no: '03', icon: '03', title: 'Gate 통과 할 일',
    desc: '이번 주, 견적 전, 출하 전 담당자가 무엇을 확인해야 하는지 산출물 단위로 받습니다.',
    link: '업무 보드',
  },
]

// 수치 통계
const STATS = [
  { v: '5', u: '개', l: '공공데이터 provider 상태 추적' },
  { v: '6', u: '축', l: '시장·규제·리콜·FTO·지원 점수' },
  { v: '100', u: '%', l: '출처 trace 포함 Gate 판단' },
  { v: '3', u: '단계', l: '이번 주·견적 전·출하 전 업무화' },
]

const READINESS = [
  { k: '공공데이터', v: '관세청·KOTRA·KATS·KIPRIS·지원사업 connector와 cache/fallback 기록' },
  { k: '서비스 구현', v: 'React 메인 화면에서 Spring 진단 생성·상태 갱신·결과 리포트 흐름 연결' },
  { k: '운영 근거', v: 'JWT 검증, 권한 검사, audit pagination, soft delete, retry/backoff 반영' },
  { k: '제출 산출물', v: '변경사항·평가 대응·미완료 범위를 공개 문서로 분리 정리' },
]

const BEFORE_AFTER = [
  { before: '담당자가 포털을 따로 열어 HS Code, 리콜, 특허, 지원사업을 수작업 확인', after: '제품·바이어 문의 기준으로 live/cache/fallback 출처와 규칙 근거를 한 결과에 연결' },
  { before: '점수는 나오지만 견적을 보내도 되는지 판단하기 어려움', after: 'GO / CONDITIONAL GO / HOLD / STOP으로 견적 전 Gate를 먼저 제시' },
  { before: '자료 누락을 알게 되어도 담당자별 다음 행동이 모호함', after: '이번 주·견적 전·출하 전 업무, 산출물, 담당 역할을 함께 생성' },
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
            {READINESS.map(item => (
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
          <p className={styles.secSub}>흩어진 공공데이터 수집 상태와 제품 정보를 연결해, 기업이 검토할 실행 후보로 정리합니다.</p>
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

        <section className={styles.compareSec}>
          <div className={styles.secEyebrow}>Before / After</div>
          <h2 className={styles.secH2}>기존 체크리스트와 다른 점</h2>
          <div className={styles.compareGrid}>
            {BEFORE_AFTER.map((row) => (
              <div key={row.before} className={styles.compareRow}>
                <div>
                  <span>Before</span>
                  <p>{row.before}</p>
                </div>
                <div>
                  <span>After</span>
                  <p>{row.after}</p>
                </div>
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
            <a href="/diagnosis">수출 Gate 판단</a>
            <a href="#">데이터</a>
            <a href="#">업무 보드</a>
            <a href="#">문서</a>
          </div>
          <div className={styles.fNote}>
            2026 산업통상자원부 공공데이터 활용 공모전 · Regulation-to-Market Twin for Regional SMEs ·
            본 결과는 공공데이터 수집 신호와 규칙 기반 점검 결과이며 최종 판단은 전문가와 함께 하십시오.
          </div>
        </footer>
      </div>
    </>
  )
}
