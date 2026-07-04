import { Link, useLocation, useNavigate } from 'react-router-dom'
import styles from './TopNav.module.css'
import { getAuthUser, logout } from '../../services/authService.js'

// 공통 고정 상단 내비게이션 바
// - 모든 주요 화면(홈·Gate 판단·마이페이지·입력 등)에서 재사용
// - 결과 뷰어(사이드바 레이아웃)에는 넣지 않음
export default function TopNav() {
  const navigate = useNavigate()
  const { pathname } = useLocation()
  const user = getAuthUser() // 로그인 정보 (없으면 null)

  // 현재 경로 기준 활성 메뉴 판별
  const isHome = pathname === '/'
  const isDiagnosis = pathname.startsWith('/diagnosis')
  const isPublicData = pathname.startsWith('/public-data')
  const isMypage = pathname.startsWith('/mypage')

  const handleLogout = async () => {
    await logout()
    navigate('/', { replace: true })
  }

  return (
    <nav className={styles.bar}>
      <div className={styles.inner}>
        <Link to="/" className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></Link>

        <div className={styles.links}>
          <Link to="/" className={`${styles.link} ${isHome ? styles.active : ''}`}>홈</Link>
          <Link to="/diagnosis" className={`${styles.link} ${isDiagnosis ? styles.active : ''}`}>Gate 판단</Link>
          <Link to="/public-data/daily-refresh" className={`${styles.link} ${isPublicData ? styles.active : ''}`}>데이터 수집</Link>
          <Link to="/mypage" className={`${styles.link} ${isMypage ? styles.active : ''}`}>마이페이지</Link>

          {user
            ? <button className={styles.auth} onClick={handleLogout}>로그아웃</button>
            : <Link to="/login" className={styles.auth}>로그인</Link>}

          {/* Gate 판단 시작: 제품등록+바이어 문의 입력을 겸하는 단일 진입점 */}
          <Link to="/diagnosis/new" className={styles.cta}>Gate 판단</Link>
        </div>
      </div>
    </nav>
  )
}
