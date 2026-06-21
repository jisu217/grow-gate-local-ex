import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import styles from './Auth.module.css'
import { login } from '../../services/authService.js'

// 로그인 페이지 컴포넌트
export default function Login() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ email: '', password: '' })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  // 입력값 변경 처리
  const handleChange = (e) => setForm(p => ({ ...p, [e.target.name]: e.target.value }))
  // 로그인 제출 처리 — 실제 백엔드 호출
  const handleSubmit = async () => {
    setError('')
    setLoading(true)
    try {
      await login(form)            // 백엔드 로그인 + 토큰 저장
      navigate('/mypage')          // 성공 → 마이페이지로
    } catch {
      setError('로그인 실패: 이메일 또는 비밀번호를 확인하세요.')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <div className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></div>
        <h2 className={styles.title}>로그인</h2>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="login-email">아이디</label>
          <input id="login-email" className={styles.input} name="email" type="email" value={form.email} onChange={handleChange} placeholder="아이디 또는 이메일 주소" />
        </div>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="login-password">비밀번호</label>
          <input id="login-password" className={styles.input} name="password" type="password" value={form.password} onChange={handleChange} placeholder="비밀번호" />
        </div>
        {error && <div className={styles.error}>{error}</div>}
        <button className={styles.btn} onClick={handleSubmit} disabled={loading}>
          {loading ? '로그인 중...' : '로그인'}
        </button>
        <div className={styles.links}>
          계정이 없으신가요? <Link to="/signup">회원가입</Link> 하기
        </div>
      </div>
    </div>
  )
}
