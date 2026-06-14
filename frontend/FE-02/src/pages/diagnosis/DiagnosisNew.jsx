import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './DiagnosisNew.module.css'

const STEPS = [
  '제품·기업 입력', '대상국 선택', '공공데이터 수집',
  'ML 예측 실행', '시장성 분석', '규제·인증 분석',
  '리콜 위험 분석', 'FTO·지원 분석', '실행 보드 생성',
  '리포트 생성', '완료',
]

// 신규 진단 입력 페이지 컴포넌트
export default function DiagnosisNew() {
  const navigate = useNavigate()

  const [formData, setFormData] = useState({
    productName: '',
    hsCode: '',
    targetCountry: '',
    description: '',
  })

  const [activeStep] = useState(0)
  const [errors, setErrors] = useState({})

  // 입력값 변경 처리
  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    if (errors[e.target.name]) setErrors((prev) => ({ ...prev, [e.target.name]: '' }))
  }

  // 진단 시작 제출 처리
  const handleSubmit = () => {
    const next = {}
    if (!formData.hsCode.trim())       next.hsCode = 'HS Code를 입력해주세요.'
    if (!formData.targetCountry.trim()) next.targetCountry = '대상 국가를 입력해주세요.'
    if (Object.keys(next).length) { setErrors(next); return }
    navigate('/diagnosis/diag-001')
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.layout}>

        <aside className={styles.rail}>
          <div className={styles.railTitle}>진단 단계</div>
          {STEPS.map((step, i) => (
            <div
              key={step}
              className={`${styles.railItem} ${i === activeStep ? styles.active : ''} ${i < activeStep ? styles.done : ''}`}
            >
              <span className={styles.railNo}>{String(i + 1).padStart(2, '0')}</span>
              <span className={styles.railNm}>{step}</span>

              {i < activeStep && <span className={styles.railCk}>✓</span>}
            </div>
          ))}
        </aside>

        <div className={styles.formArea}>
          <div className={styles.formCard}>
            <div className={styles.formEyebrow}>S01 INPUT</div>
            <h2 className={styles.formTitle}>제품 정보 입력</h2>
            <p className={styles.formDesc}>진단할 제품과 기업 정보를 입력하세요.</p>

            <div className={styles.fields}>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="productName">제품명 *</label>
                <input
                  id="productName"
                  className={styles.input}
                  name="productName"
                  value={formData.productName}
                  onChange={handleChange}
                  placeholder="예: 리튬이온 배터리팩"
                />
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="hsCode">HS Code *</label>
                <input
                  id="hsCode"
                  className={`${styles.input} ${errors.hsCode ? styles.inputError : ''}`}
                  name="hsCode"
                  value={formData.hsCode}
                  onChange={handleChange}
                  placeholder="예: 850760"
                  aria-describedby={errors.hsCode ? 'hsCode-error' : undefined}
                />
                {errors.hsCode && <span id="hsCode-error" className={styles.fieldError}>{errors.hsCode}</span>}
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="targetCountry">대상국 *</label>
                <input
                  id="targetCountry"
                  className={`${styles.input} ${errors.targetCountry ? styles.inputError : ''}`}
                  name="targetCountry"
                  value={formData.targetCountry}
                  onChange={handleChange}
                  placeholder="예: 독일 / EU"
                  aria-describedby={errors.targetCountry ? 'targetCountry-error' : undefined}
                />
                {errors.targetCountry && <span id="targetCountry-error" className={styles.fieldError}>{errors.targetCountry}</span>}
              </div>
              <div className={styles.field}>
                <label className={styles.label} htmlFor="description">제품 설명</label>
                <textarea
                  id="description"
                  className={styles.textarea}
                  name="description"
                  value={formData.description}
                  onChange={handleChange}
                  placeholder="제품의 주요 특성, 용도, 인증 현황 등을 입력하세요."
                  rows={4}
                />
              </div>
            </div>

            <div className={styles.knowledgeBtn}>
              <button className={styles.outlineBtn}>
                📂 저장된 제품 지식 불러오기
              </button>
              <span className={styles.knowledgeHint}>
                마이페이지에 등록된 제품 지식을 선택하면 입력값이 자동으로 채워집니다.
              </span>
            </div>

            <button className={styles.submitBtn} onClick={handleSubmit}>
              AI 진단 시작 →
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
