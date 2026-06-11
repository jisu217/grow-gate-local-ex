import { useNavigate } from 'react-router-dom'
import styles from './Auth.module.css'

export default function Signup() {
  const navigate = useNavigate()
  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <div className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></div>
        <h2 className={styles.title}>회원가입</h2>
        <p className={styles.sub}>계정 유형을 선택하세요.</p>
        <div className={styles.typeRow}>
          <button className={styles.typeBtn} onClick={() => navigate('/signup/personal')}>👤 개인 계정</button>
          <button className={styles.typeBtn} onClick={() => navigate('/signup/company')}>🏢 기업 계정</button>
        </div>
      </div>
    </div>
  )
}
