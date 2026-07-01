import { useState, useEffect, useMemo } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './DiagnosisNew.module.css'
import { getMypageProducts, createMypageProduct } from '../../services/mypageService.js'
import { runDiagnosis } from '../../services/diagnosisService.js'
import { normalizeTargetCountry } from '../../utils/country.js'

const PICKER_PAGE_SIZE = 5
const PICKER_PAGE_WINDOW = 5

function normalizeSearchValue(value) {
  return String(value ?? '').toLowerCase().replace(/\s+/g, '')
}

function normalizeHsCode(value) {
  return String(value ?? '').replace(/[^\d]/g, '')
}

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
  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [pickerLoadError, setPickerLoadError] = useState('')

  // 저장된 제품 불러오기 모달
  const [pickerOpen, setPickerOpen] = useState(false)
  const [savedProducts, setSavedProducts] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const [pickerKeyword, setPickerKeyword] = useState('')
  const [pickerPage, setPickerPage] = useState(1)

  // 직접 입력한 제품을 마이페이지에도 저장할지 (기본 켜짐)
  const [saveToMypage, setSaveToMypage] = useState(true)

  // 마이페이지에 저장한 제품 목록 로드
  useEffect(() => {
    getMypageProducts()
      .then((products) => {
        setSavedProducts(products)
        setPickerLoadError('')
      })
      .catch((err) => {
        setSavedProducts([])
        setPickerLoadError(err.message || '저장된 제품을 불러오지 못했습니다. 로그인 상태와 서버 연결을 확인해주세요.')
      })
  }, [])

  const filteredProducts = useMemo(() => {
    const keyword = normalizeSearchValue(pickerKeyword)
    if (!keyword) return savedProducts

    return savedProducts.filter((product) => {
      const haystack = normalizeSearchValue([
        product.name,
        product.hs,
        product.country,
        product.description,
      ].join(' '))
      return haystack.includes(keyword)
    })
  }, [pickerKeyword, savedProducts])

  const pickerTotalPages = Math.max(1, Math.ceil(filteredProducts.length / PICKER_PAGE_SIZE))
  const safePickerPage = Math.min(pickerPage, pickerTotalPages)
  const pickerStart = (safePickerPage - 1) * PICKER_PAGE_SIZE
  const pagedProducts = filteredProducts.slice(pickerStart, pickerStart + PICKER_PAGE_SIZE)
  const shouldShowPagination = filteredProducts.length > PICKER_PAGE_SIZE
  const visiblePickerPages = useMemo(() => {
    if (!shouldShowPagination) return []

    const windowStart = Math.floor((safePickerPage - 1) / PICKER_PAGE_WINDOW) * PICKER_PAGE_WINDOW + 1
    const windowEnd = Math.min(windowStart + PICKER_PAGE_WINDOW - 1, pickerTotalPages)
    return Array.from({ length: windowEnd - windowStart + 1 }, (_, index) => windowStart + index)
  }, [safePickerPage, pickerTotalPages, shouldShowPagination])

  // 입력값 변경 처리
  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    if (errors[e.target.name]) setErrors((prev) => ({ ...prev, [e.target.name]: '' }))
    if (submitError) setSubmitError('')
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

  const openPicker = () => {
    setPickerKeyword('')
    setPickerPage(1)
    setPickerOpen(true)
  }

  const handlePickerSearch = (e) => {
    setPickerKeyword(e.target.value)
    setPickerPage(1)
  }

  // 진단 시작 제출 처리
  const handleSubmit = async () => {
    const next = {}
    const hsCode = normalizeHsCode(formData.hsCode)
    const targetCountry = normalizeTargetCountry(formData.targetCountry)
    const productName = formData.productName.trim()

    if (!productName) next.productName = '제품명을 입력해주세요.'
    if (!hsCode) next.hsCode = 'HS Code를 입력해주세요.'
    else if (!/^\d{6,10}$/.test(hsCode)) next.hsCode = 'HS Code는 숫자 6~10자리로 입력해주세요.'
    if (!formData.targetCountry.trim()) next.targetCountry = '대상 국가를 입력해주세요.'
    if (Object.keys(next).length) { setErrors(next); return }

    setSubmitting(true)
    setSubmitError('')
    let createdProduct = null

    try {
      let productId = selectedId
      if (saveToMypage && !productId) {
        const created = await createMypageProduct({
          name: productName,
          hs: hsCode,
          country: targetCountry,
          description: formData.description.trim(),
        })
        productId = created.id
        createdProduct = created
        setSavedProducts((prev) => prev.some((product) => product.id === created.id) ? prev : [...prev, created])
      }

      const result = await runDiagnosis({
        productId,
        productName,
        hsCode,
        targetCountry,
        description: formData.description.trim(),
      })
      navigate(`/diagnosis/${result.id}`)
    } catch (err) {
      const savedNotice = createdProduct
        ? `제품 "${createdProduct.name}"은 마이페이지에 저장됐지만 진단 생성이 실패했습니다. 저장된 제품을 선택해 다시 실행할 수 있습니다. `
        : ''
      setSubmitError(savedNotice + (err.message || '진단 실행에 실패했습니다. 입력값과 서버 상태를 확인해주세요.'))
    } finally {
      setSubmitting(false)
    }
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
                  className={`${styles.input} ${errors.productName ? styles.inputError : ''}`}
                  name="productName"
                  value={formData.productName}
                  onChange={handleChange}
                  placeholder="예: 리튬이온 배터리팩"
                  aria-describedby={errors.productName ? 'productName-error' : undefined}
                />
                {errors.productName && <span id="productName-error" className={styles.fieldError}>{errors.productName}</span>}
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
              <button className={styles.outlineBtn} onClick={openPicker}>
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

            {submitError && <div className={styles.fieldError}>{submitError}</div>}

            <button className={styles.submitBtn} onClick={handleSubmit} disabled={submitting}>
              {submitting ? '진단 요청 중…' : 'AI 진단 시작 →'}
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

            <div className={styles.pickSearch}>
              <span className={styles.pickSearchIcon} aria-hidden="true">⌕</span>
              <input
                className={styles.pickSearchInput}
                type="search"
                value={pickerKeyword}
                onChange={handlePickerSearch}
                placeholder="제품명, HS Code, 대상국 키워드로 검색"
                aria-label="저장된 제품 검색"
              />
              {pickerKeyword && (
                <button className={styles.pickSearchClear} onClick={() => { setPickerKeyword(''); setPickerPage(1) }}>
                  초기화
                </button>
              )}
            </div>

            <div className={styles.pickMetaRow}>
              <span>총 <b>{filteredProducts.length}</b>개 제품 · 5개씩 보기</span>
              <span className={styles.pickExamples}>제품명 · HS Code · 대상국 · 설명 검색</span>
            </div>

            <div className={styles.pickList}>
              {pickerLoadError && (
                <div className={styles.pickError}>{pickerLoadError}</div>
              )}
              {savedProducts.length === 0 && (
                <div className={styles.pickEmpty}>저장된 제품이 없습니다. 마이페이지에서 먼저 제품을 등록하세요.</div>
              )}
              {savedProducts.length > 0 && filteredProducts.length === 0 && (
                <div className={styles.pickEmpty}>
                  검색 결과가 없습니다. 제품명, HS Code, 대상국 또는 설명 키워드를 다시 입력해 주세요.
                </div>
              )}
              {pagedProducts.map((p) => (
                <button key={p.id} className={styles.pickItem} onClick={() => pickProduct(p)}>
                  <div>
                    <div className={styles.pickName}>{p.name}</div>
                    <div className={styles.pickMeta}>HS {p.hs} · {p.country} 수출</div>
                    {p.description && <div className={styles.pickDescription}>{p.description}</div>}
                  </div>
                  <span className={styles.pickGo}>선택 →</span>
                </button>
              ))}
            </div>

            {shouldShowPagination && (
              <div className={styles.pickPaginationRow}>
                <span className={styles.pickPageHelp}>
                  {safePickerPage}/{pickerTotalPages}페이지 · 제품 {pickerStart + 1}-{Math.min(pickerStart + PICKER_PAGE_SIZE, filteredProducts.length)}번째 표시
                </span>
                <div className={styles.pickPager} aria-label="저장된 제품 페이지 이동">
                  <button
                    className={styles.pickPageButton}
                    disabled={safePickerPage === 1}
                    onClick={() => setPickerPage((page) => Math.max(1, page - 1))}
                  >
                    이전
                  </button>
                  {visiblePickerPages.map((page) => (
                    <button
                      key={page}
                      className={`${styles.pickPageButton} ${page === safePickerPage ? styles.activePage : ''}`}
                      onClick={() => setPickerPage(page)}
                      aria-current={page === safePickerPage ? 'page' : undefined}
                    >
                      {page}
                    </button>
                  ))}
                  <button
                    className={styles.pickPageButton}
                    disabled={safePickerPage === pickerTotalPages}
                    onClick={() => setPickerPage((page) => Math.min(pickerTotalPages, page + 1))}
                  >
                    다음
                  </button>
                </div>
              </div>
            )}

            <div className={styles.sheetFoot}>
              <button className={styles.sheetCancel} onClick={() => setPickerOpen(false)}>닫기</button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
