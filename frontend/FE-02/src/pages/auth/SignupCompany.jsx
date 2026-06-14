import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './Auth.module.css'

// 입력 필드
const FIELDS = [
  { key:'managerName', label:'담당자명',  placeholder:'담당자 이름' },
  { key:'userId',      label:'아이디',    placeholder:'아이디 또는 이메일' },
  { key:'password',    label:'비밀번호',  placeholder:'비밀번호', type:'password' },
  { key:'companyName', label:'기업명',    placeholder:'기업명' },
  { key:'bizNo',       label:'사업자번호', placeholder:'사업자번호' },
  { key:'industry',    label:'업종',      placeholder:'업종' },
]

// 기업 회원가입 페이지 컴포넌트
export default function SignupCompany() {
  const navigate = useNavigate()
  const [form, setForm] = useState(Object.fromEntries(FIELDS.map(f => [f.key,''])))
  // 입력값 변경 처리
  const handleChange = (e) => setForm(p => ({...p, [e.target.name]: e.target.value}))

  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <div className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></div>
        <h2 className={styles.title}>기업 회원가입</h2>
        {FIELDS.map(f => (
          <div key={f.key} className={styles.field}>
            <label className={styles.label} htmlFor={`signup-company-${f.key}`}>{f.label}</label>
            <input id={`signup-company-${f.key}`} className={styles.input} name={f.key} type={f.type||'text'} value={form[f.key]} onChange={handleChange} placeholder={f.placeholder} />
          </div>
        ))}
        <button className={styles.btn} onClick={() => navigate('/login')}>가입 완료</button>
      </div>
    </div>
  )
}
