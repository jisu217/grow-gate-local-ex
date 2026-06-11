import { Link } from 'react-router-dom'
import styles from './Mypage.module.css'

const USER = {
  name: '홍길동',
  email: 'hong@batterytech.co.kr',
  type: 'company',
  companyName: '경남 배터리테크',
  industry: '전기전자 · 배터리',
}

export default function Mypage() {
  return (
    <div className={styles.wrap}>
      <h1 className={styles.title}>마이페이지</h1>
      <div className={styles.card}>
        <div className={styles.section}>
          <div className={styles.sectionTitle}>계정 정보</div>
          <div className={styles.row}><span className={styles.key}>이름</span><span>{USER.name}</span></div>
          <div className={styles.row}><span className={styles.key}>이메일</span><span>{USER.email}</span></div>

          {USER.type === 'company' && (
            <>
              <div className={styles.row}><span className={styles.key}>기업명</span><span>{USER.companyName}</span></div>
              <div className={styles.row}><span className={styles.key}>업종</span><span>{USER.industry}</span></div>
            </>
          )}
        </div>
        <div className={styles.menuRow}>
          <Link to="/mypage/knowledge" className={styles.menuBtn}>📦 제품 지식 관리</Link>
          <Link to="/diagnosis" className={styles.menuBtn}>📋 진단 이력</Link>
        </div>
      </div>
    </div>
  )
}
