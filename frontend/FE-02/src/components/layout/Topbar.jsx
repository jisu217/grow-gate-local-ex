// 진단 결과 상단 바
import { Link } from 'react-router-dom'
import styles from './Topbar.module.css'

export default function Topbar({ currentStepName }) {
  return (

    <div className={styles.topbar}>

      <div className={styles.crumb}>
        수출 Gate 판단 · <b>{currentStepName}</b>
      </div>

      <div className={styles.search}>
        <span style={{ color: 'var(--slate-2)' }}>⌕</span>
        <input placeholder="규제·인증·지원사업 검색" />
      </div>

      <Link to="/mypage" className={styles.mypage}>
        마이페이지
      </Link>

      <div className={styles.live}>
        <span className={styles.blip} />
        DEMO LIVE
      </div>
    </div>
  )
}
