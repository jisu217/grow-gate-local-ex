import { useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import styles from './KnowledgeForm.module.css'
import { usePermission } from '../../hooks/usePermission.js'
import StatusMessage from '../../components/ui/StatusMessage.jsx'

const INITIAL = {
  productName: '리튬이온 배터리팩',
  category: '전기전자',
  description: 'UN38.3 인증 추진 중인 리튬이온 배터리팩. 독일/EU 수출 목표.',
}

const CATEGORIES = ['전기전자', '전자부품', '기계·장비', '화학·소재', '식품·의료', '기타']

// 제품 지식 수정 페이지 컴포넌트
export default function KnowledgeEdit() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const { can } = usePermission()

  const [form, setForm] = useState({ ...INITIAL })

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

  // 입력값 변경 처리
  const handleChange = (e) =>
    setForm((prev) => ({ ...prev, [e.target.name]: e.target.value }))

  // 수정 저장 처리
  const handleSave = () => {

    navigate(`/mypage/knowledge/${productId}`)
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

        <button className={styles.submitBtn} onClick={handleSave}>
          수정 저장
        </button>
      </div>
    </div>
  )
}
