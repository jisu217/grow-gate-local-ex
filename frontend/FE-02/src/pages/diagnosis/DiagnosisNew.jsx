import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './DiagnosisNew.module.css'
import { getMypageProducts, createMypageProduct } from '../../services/mypageService.js'

// 신규 진단 입력 페이지 컴포넌트
export default function DiagnosisNew() {
  const navigate = useNavigate()

  const [formData, setFormData] = useState({
    productName: '',
    hsCode: '',
    targetCountry: '',
    description: '',
  })

  const [errors, setErrors] = useState({})

  // 저장된 제품 불러오기 모달
  const [pickerOpen, setPickerOpen] = useState(false)
  const [savedProducts, setSavedProducts] = useState([])
  const [selectedId, setSelectedId] = useState(null)

  // 직접 입력한 제품을 마이페이지에도 저장할지 (기본 켜짐)
  const [saveToMypage, setSaveToMypage] = useState(true)

  // 마이페이지에 저장한 제품 목록 로드
  useEffect(() => {
    getMypageProducts().then(setSavedProducts)
  }, [])

  // 입력값 변경 처리
  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    if (errors[e.target.name]) setErrors((prev) => ({ ...prev, [e.target.name]: '' }))
  }

  // 저장된 제품 선택 → 입력값 자동 채움
  const pickProduct = (p) => {
    setFormData({
      productName: p.name,
      hsCode: p.hs,
      targetCountry: p.country,
      description: p.description ?? '',
    })
    setSelectedId(p.id)
    setErrors({})
    setPickerOpen(false)
  }

  // 진단 시작 제출 처리
  const handleSubmit = async () => {
    const next = {}
    if (!formData.hsCode.trim())       next.hsCode = 'HS Code를 입력해주세요.'
    if (!formData.targetCountry.trim()) next.targetCountry = '대상 국가를 입력해주세요.'
    if (Object.keys(next).length) { setErrors(next); return }

    // 직접 입력한 신규 제품이고 "내 제품으로 저장"이 켜져 있으면 마이페이지에 추가
    // (저장된 제품을 불러온 경우 selectedId가 있으므로 중복 저장하지 않음)
    if (saveToMypage && !selectedId && formData.productName.trim()) {
      await createMypageProduct({
        name: formData.productName.trim(),
        hs: formData.hsCode.trim(),
        country: formData.targetCountry.trim(),
        description: formData.description.trim(),
      })
    }

    navigate('/diagnosis/diag-001')
  }

  return (
    <div className={styles.wrap}>
      <div className={styles.layout}>

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
              <button className={styles.outlineBtn} onClick={() => setPickerOpen(true)}>
                📂 저장된 제품 지식 불러오기
              </button>
              <span className={styles.knowledgeHint}>
                {selectedId
                  ? '✓ 저장된 제품을 불러왔습니다. 필요하면 입력값을 수정하세요.'
                  : '마이페이지에 등록된 제품 지식을 선택하면 입력값이 자동으로 채워집니다.'}
              </span>
            </div>

            {!selectedId && (
              <label className={styles.saveCheck}>
                <input
                  type="checkbox"
                  checked={saveToMypage}
                  onChange={(e) => setSaveToMypage(e.target.checked)}
                />
                <span>이 제품을 <b>내 제품(마이페이지)</b>에도 저장</span>
              </label>
            )}

            <button className={styles.submitBtn} onClick={handleSubmit}>
              AI 진단 시작 →
            </button>
          </div>
        </div>
      </div>

      {/* 저장된 제품 불러오기 모달 */}
      {pickerOpen && (
        <div className={styles.modal} onClick={(e) => { if (e.target === e.currentTarget) setPickerOpen(false) }}>
          <div className={styles.sheet}>
            <h3 className={styles.sheetTitle}>저장된 제품 불러오기</h3>
            <p className={styles.sheetDesc}>마이페이지에 등록한 제품을 선택하면 입력값이 자동으로 채워집니다.</p>

            <div className={styles.pickList}>
              {savedProducts.length === 0 && (
                <div className={styles.pickEmpty}>저장된 제품이 없습니다. 마이페이지에서 먼저 제품을 등록하세요.</div>
              )}
              {savedProducts.map((p) => (
                <button key={p.id} className={styles.pickItem} onClick={() => pickProduct(p)}>
                  <div>
                    <div className={styles.pickName}>{p.name}</div>
                    <div className={styles.pickMeta}>HS {p.hs} · {p.country} 수출</div>
                  </div>
                  <span className={styles.pickGo}>선택 →</span>
                </button>
              ))}
            </div>

            <div className={styles.sheetFoot}>
              <button className={styles.sheetCancel} onClick={() => setPickerOpen(false)}>닫기</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
