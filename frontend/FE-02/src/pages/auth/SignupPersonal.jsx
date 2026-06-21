import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import styles from './Auth.module.css'
import { signup } from '../../services/authService.js'

// 개인 회원가입 페이지 컴포넌트
export default function SignupPersonal() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name:'', email:'', password:'' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  // 입력값 변경 처리
  const handleChange = (e) => setForm(p => ({...p, [e.target.name]: e.target.value}))

  const FIELDS = [
    { key:'name',     label:'이름',   placeholder:'이름',     type:'text' },
    { key:'email',    label:'이메일', placeholder:'이메일',   type:'email' },
    { key:'password', label:'비밀번호', placeholder:'비밀번호', type:'password' },
  ]

  // 가입 제출 처리
  const handleSubmit = async () => {
    setError('')
    setSubmitting(true)
    try {
      await signup({ type: 'personal', email: form.email, password: form.password, name: form.name })
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
        <Link to="/" className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></Link>
        <h2 className={styles.title}>개인 회원가입</h2>
        {FIELDS.map(f => (
          <div key={f.key} className={styles.field}>
            <label className={styles.label} htmlFor={`signup-personal-${f.key}`}>{f.label}</label>
            <input id={`signup-personal-${f.key}`} className={styles.input} name={f.key} type={f.type} value={form[f.key]} onChange={handleChange} placeholder={f.placeholder} />
          </div>
        ))}
        {error && <p className={styles.error}>{error}</p>}
        <button className={styles.btn} onClick={handleSubmit} disabled={submitting}>가입 완료</button>
      </div>
    </div>
  )
}
