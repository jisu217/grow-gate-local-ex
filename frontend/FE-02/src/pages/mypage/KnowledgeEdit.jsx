import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import styles from './KnowledgeForm.module.css'
import { usePermission } from '../../hooks/usePermission.js'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import { getProductKnowledge, saveProductKnowledge } from '../../services/productService.js'
import { COUNTRY_OPTIONS, normalizeTargetCountry } from '../../utils/country.js'

const CATEGORIES = ['전기전자', '전자부품', '기계·장비', '화학·소재', '식품·의료', '기타']

// 제품 지식 수정 페이지 컴포넌트
export default function KnowledgeEdit() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const { can } = usePermission()

  const [form, setForm] = useState({ productName: '', hsCode: '', targetCountry: 'DE', category: '', description: '' })
  const [pageState, setPageState] = useState('loading')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    setPageState('loading')
    getProductKnowledge(productId)
      .then((data) => {
        setForm({
          productName: data.name,
          hsCode: data.hsCode ?? '',
          targetCountry: normalizeTargetCountry(data.targetCountry),
          category: data.category,
          description: data.description,
        })
        setPageState('ok')
      })
      .catch(() => setPageState('error'))
  }, [productId])

  if (!can('knowledge:edit')) {
    return (
      <div className={styles.wrap}>
        <button className={styles.back} onClick={() => navigate(`/mypage/knowledge/${productId}`)}>
          ← 제품 상세로 돌아가기
        </button>
        <StatusMessage
          type="permission"
          message="이 회사 제품 지식을 수정할 권한이 없습니다."
        />
      </div>
    )
  }

  if (pageState === 'loading') return (
    <div className={styles.wrap}>
      <StatusMessage type="loading" message="제품 지식을 불러오는 중입니다." />
    </div>
  )
  if (pageState === 'error') return (
    <div className={styles.wrap}>
      <button className={styles.back} onClick={() => navigate(`/mypage/knowledge/${productId}`)}>
        ← 제품 상세로 돌아가기
      </button>
      <StatusMessage type="error" message="제품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요." />
    </div>
  )

  // 입력값 변경 처리
  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  // 수정 저장 처리
  const handleSave = async () => {
    const hsCode = form.hsCode.trim()
    if (!form.productName.trim()) { setError('제품명은 필수입니다.'); return }
    if (!/^\d{6,10}$/.test(hsCode)) { setError('HS Code는 숫자 6~10자리로 입력해주세요.'); return }

    setSaving(true)
    setError('')
    try {
      await saveProductKnowledge(productId, {
        ...form,
        hsCode,
        targetCountry: normalizeTargetCountry(form.targetCountry),
      })
      navigate(`/mypage/knowledge/${productId}`)
    } catch {
      setError('저장에 실패했습니다. 잠시 후 다시 시도해주세요.')
      setSaving(false)
    }
  }

  return (
    <div className={styles.wrap}>
      <button className={styles.back} onClick={() => navigate(`/mypage/knowledge/${productId}`)}>
        ← 제품 상세로 돌아가기
      </button>
      <h1 className={styles.title}>제품 정보 수정</h1>

      <div className={styles.card}>
        <div className={styles.field}>
          <label className={styles.label} htmlFor="edit-productName">제품명 *</label>
          <input
            id="edit-productName"
            className={styles.input}
            name="productName"
            value={form.productName}
            onChange={handleChange}
          />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="edit-hsCode">HS Code *</label>
          <input
            id="edit-hsCode"
            className={styles.input}
            name="hsCode"
            value={form.hsCode}
            onChange={handleChange}
            placeholder="예: 850760"
            inputMode="numeric"
          />
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="edit-targetCountry">수출 대상국</label>
          <select
            id="edit-targetCountry"
            className={styles.input}
            name="targetCountry"
            value={form.targetCountry}
            onChange={handleChange}
          >
            {COUNTRY_OPTIONS.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="edit-category">카테고리</label>
          <select
            id="edit-category"
            className={styles.input}
            name="category"
            value={form.category}
            onChange={handleChange}
          >
            {CATEGORIES.map((c) => (
              <option key={c} value={c}>{c}</option>
            ))}
          </select>
        </div>

        <div className={styles.field}>
          <label className={styles.label} htmlFor="edit-description">제품 설명</label>
          <textarea
            id="edit-description"
            className={styles.textarea}
            name="description"
            value={form.description}
            onChange={handleChange}
            rows={5}
          />
        </div>

        <div style={{
          fontSize: 12, color: 'var(--slate-2)',
          background: 'var(--blue-soft)', border: '1px solid var(--blue-line)',
          borderRadius: 10, padding: '10px 14px', marginBottom: 8
        }}>
          💡 저장하면 변경 이력(Audit Log)이 자동으로 기록됩니다.
        </div>

        {error && <p className={styles.errorMsg} role="alert">{error}</p>}

        <button className={styles.submitBtn} onClick={handleSave} disabled={saving}>
          {saving ? '저장 중…' : '수정 저장'}
        </button>
      </div>
    </div>
  )
}
