import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './KnowledgeForm.module.css'
import { createProduct } from '../../services/productService.js'

const CATEGORIES = ['전기전자', '전자부품', '기계·장비', '화학·소재', '식품·의료', '기타']

// 신규 제품 등록 페이지 컴포넌트
export default function KnowledgeNew() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ productName: '', category: '', description: '' })
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  // 입력값 변경 처리
  const handleChange = (e) => {
    setError('')
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))
  }

  // 제품 등록 제출 처리
  const handleSubmit = async () => {
    if (!form.productName.trim()) {
      setError('제품명은 필수 항목입니다. 입력해주세요.')
      return
    }
    setSaving(true)
    setError('')
    try {
      const created = await createProduct(form)
      navigate(`/mypage/knowledge/${created.id}`)
    } catch {
      setError('제품 등록에 실패했습니다. 잠시 후 다시 시도해주세요.')
      setSaving(false)
    }
  }

  return (
    <div className={styles.wrap}>
      <button className={styles.back} onClick={() => navigate('/mypage/knowledge')}>
        ← 제품 지식 관리
      </button>
      <h1 className={styles.title}>신규 제품 등록</h1>

      <div className={styles.card}>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="new-productName">제품명 *</label>
          <input
            id="new-productName"
            className={`${styles.input} ${error ? styles.inputError : ''}`}
            name="productName"
            value={form.productName}
            onChange={handleChange}
            placeholder="예: 리튬이온 배터리팩"
            aria-describedby={error ? 'new-productName-error' : undefined}
            aria-invalid={!!error}
          />
          {error && <p id="new-productName-error" className={styles.errorMsg} role="alert">{error}</p>}
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="new-category">카테고리</label>
          <select
            id="new-category"
            className={styles.input}
            name="category"
            value={form.category}
            onChange={handleChange}
          >
            <option value="">선택하세요</option>
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="new-description">제품 설명</label>
          <textarea
            id="new-description"
            className={styles.textarea}
            name="description"
            value={form.description}
            onChange={handleChange}
            placeholder="제품의 주요 특성, 용도, 현재 인증 현황 등을 입력하세요."
            rows={5}
          />
        </div>

        <button className={styles.submitBtn} onClick={handleSubmit} disabled={saving}>
          {saving ? '등록 중…' : '제품 등록 완료'}
        </button>
      </div>
    </div>
  )
}
