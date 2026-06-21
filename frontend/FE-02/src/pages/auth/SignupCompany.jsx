import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './Auth.module.css'
import { signup } from '../../services/authService.js'

// 입력 필드
const FIELDS = [
  { key:'name',           label:'담당자명',  placeholder:'담당자 이름' },
  { key:'email',          label:'이메일',    placeholder:'이메일', type:'email' },
  { key:'password',       label:'비밀번호',  placeholder:'비밀번호', type:'password' },
  { key:'companyName',    label:'기업명',    placeholder:'기업명' },
  { key:'businessNumber', label:'사업자번호', placeholder:'사업자번호' },
  { key:'industry',       label:'업종',      placeholder:'업종' },
]

// 기업 회원가입 페이지 컴포넌트
export default function SignupCompany() {
  const navigate = useNavigate()
  const [form, setForm] = useState(Object.fromEntries(FIELDS.map(f => [f.key,''])))
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  // 입력값 변경 처리
  const handleChange = (e) => setForm(p => ({...p, [e.target.name]: e.target.value}))

  // 가입 제출 처리
  const handleSubmit = async () => {
    setError('')
    setSubmitting(true)
    try {
      await signup({
        type: 'company',
        email: form.email,
        password: form.password,
        name: form.name,
        companyName: form.companyName,
        businessNumber: form.businessNumber,
        industry: form.industry,
      })
      navigate('/login')
    } catch {
      setError('회원가입에 실패했습니다. 입력 정보를 확인해주세요.')
    } finally {
      setSubmitting(false)
    }
  }

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
        {error && <p className={styles.error}>{error}</p>}
        <button className={styles.btn} onClick={handleSubmit} disabled={submitting}>가입 완료</button>
      </div>
    </div>
  )
}
