import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './Auth.module.css'

export default function SignupPersonal() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ name:'', userId:'', password:'' })
  const handleChange = (e) => setForm(p => ({...p, [e.target.name]: e.target.value}))

  const FIELDS = [
    { key:'name',     label:'이름',   placeholder:'이름',              type:'text' },
    { key:'userId',   label:'아이디', placeholder:'아이디 또는 이메일', type:'text' },
    { key:'password', label:'비밀번호', placeholder:'비밀번호',         type:'password' },
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
        <button className={styles.btn} onClick={() => navigate('/login')}>가입 완료</button>
      </div>
    </div>
  )
}
