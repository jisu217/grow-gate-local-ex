import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import styles from './Auth.module.css'
import { login } from '../../services/authService.js'

// 로그인 페이지 컴포넌트
export default function Login() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  // 입력값 변경 처리
  const handleChange = (e) => setForm(p => ({ ...p, [e.target.name]: e.target.value }))
  // 로그인 제출 처리
  const handleSubmit = async () => {
    setError('')
    setSubmitting(true)
    try {
      await login(form.email, form.password)
      navigate('/mypage')
    } catch (err) {
      setError(err.status === 429
        ? (err.message || '로그인 실패가 반복되어 잠시 후 다시 시도해주세요.')
        : '이메일 또는 비밀번호가 올바르지 않습니다.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <Link to="/" className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></Link>
        <h2 className={styles.title}>로그인</h2>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="login-email">아이디</label>
          <input id="login-email" className={styles.input} name="email" type="email" value={form.email} onChange={handleChange} placeholder="아이디 또는 이메일 주소" />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="login-password">비밀번호</label>
          <input id="login-password" className={styles.input} name="password" type="password" value={form.password} onChange={handleChange} placeholder="비밀번호" />
        </div>
        {error && <p className={styles.error}>{error}</p>}
        <button className={styles.btn} onClick={handleSubmit} disabled={submitting}>로그인</button>
        <div className={styles.links}>
          계정이 없으신가요? <Link to="/signup">회원가입</Link> 하기
        </div>
      </div>
    </div>
  )
}
