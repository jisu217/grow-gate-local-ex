import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './Auth.module.css'
import { signup } from '../../services/authService.js'

// 개인 회원가입 페이지 컴포넌트
export default function SignupPersonal() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name:'', email:'', password:'' })
  const [error, setError] = useState('')
  // 입력값 변경 처리
  const handleChange = (e) => setForm(p => ({...p, [e.target.name]: e.target.value}))

  // 가입 완료 — 실제 백엔드 호출
  const handleSubmit = async () => {
    setError('')
    try {
      await signup({
        type: 'personal',
        email: form.email,
        password: form.password,
        name: form.name,
      })
      alert('회원가입 완료! 로그인해주세요.')
      navigate('/login')
    } catch (e) {
      setError('회원가입 실패: ' + e.message)
    }
  }

  const FIELDS = [
    { key:'name',     label:'이름',   placeholder:'이름',     type:'text' },
    { key:'email',    label:'이메일', placeholder:'이메일',   type:'email' },
    { key:'password', label:'비밀번호', placeholder:'비밀번호', type:'password' },
  ]

  return (
    <div className={styles.wrap}>
      <div className={styles.card}>
        <div className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></div>
        <h2 className={styles.title}>개인 회원가입</h2>
        {FIELDS.map(f => (
          <div key={f.key} className={styles.field}>
            <label className={styles.label} htmlFor={`signup-personal-${f.key}`}>{f.label}</label>
            <input id={`signup-personal-${f.key}`} className={styles.input} name={f.key} type={f.type} value={form[f.key]} onChange={handleChange} placeholder={f.placeholder} />
          </div>
        ))}
        {error && <div className={styles.error}>{error}</div>}
        <button className={styles.btn} onClick={handleSubmit}>가입 완료</button>
      </div>
    </div>
  )
}
