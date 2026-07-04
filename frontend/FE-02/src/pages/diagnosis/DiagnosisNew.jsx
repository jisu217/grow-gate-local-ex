import { useState, useEffect, useMemo } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import styles from './DiagnosisNew.module.css'
import { getMypageProducts, createMypageProduct } from '../../services/mypageService.js'
import { runDiagnosis } from '../../services/diagnosisService.js'
import { COUNTRY_OPTIONS, normalizeTargetCountry } from '../../utils/country.js'

const PICKER_PAGE_SIZE = 5
const PICKER_PAGE_WINDOW = 5
const CATEGORIES = ['전기전자', '전자부품', '기계·장비', '화학·소재', '식품·의료', '기타']
const DEMO_SCENARIOS = [
  {
    key: 'battery',
    title: '창원 배터리팩',
    gate: 'HOLD',
    account: 'owner+battery@growgate.local',
    productId: 'a2e2360b-c6ac-57c3-8e3b-61b303faf36c',
    diagnosisId: '2b52340c-d51d-50dc-b6cb-592627abb62d',
    region: '경남 창원',
    industry: '전기전자·이차전지 부품 제조',
    buyerRequest: 'CE 적합성 선언, UN38.3, RoHS/REACH 선언, 영문 라벨링, 배터리 회수/리콜 대응 프로세스, 독일어 안전주의 문구, 6주 이내 샘플 20대 제출 요청.',
    buyerDeadline: '2026-07-18',
    expectedShipmentDate: '2026-09-05',
    requestedCertifications: 'CE Technical File, RoHS/REACH Declaration, EU Battery Regulation labeling memo',
    currentDocuments: 'BOM v3.2, UN38.3 Test Summary, KC Safety Certificate, Battery Safety Manual EN draft, EU CE Gap Memo',
    exportExperience: 'EU 샘플 수출 1회, 양산 납품 경험 없음',
    budgetLimitKrw: '18000000',
  },
  {
    key: 'marine',
    title: '부산 마린 액추에이터',
    gate: 'CONDITIONAL_GO',
    account: 'owner+marine@growgate.local',
    productId: '9f7f291b-210a-5575-891a-bf6115bc7cce',
    diagnosisId: '6bc9800e-755c-55f9-b9ce-87a69dc2c450',
    region: '부산 강서',
    industry: '조선기자재·스마트 밸브 액추에이터',
    buyerRequest: 'UL/NRTL 적용 가능성 검토, FCC Part 15 해당 여부, IP67 시험성적서, 염수분무 720h 결과, ABS/DNV 선급 적용 가능성 메모, 납기 10주 견적 요청.',
    buyerDeadline: '2026-07-24',
    expectedShipmentDate: '2026-09-25',
    requestedCertifications: 'UL/NRTL applicability memo, FCC applicability memo, ABS/DNV pre-check memo',
    currentDocuments: 'IP67 Test Report, Salt Spray 720h Report, Firmware OTA Release Note, Assembly Drawing Rev.C, Buyer RFQ US-2026-07',
    exportExperience: '동남아/중동 선박 부품 납품 4회, 미국 신규 바이어 첫 거래',
    budgetLimitKrw: '26000000',
  },
  {
    key: 'medical',
    title: '대구 메디센서',
    gate: 'STOP',
    account: 'owner+medical@growgate.local',
    productId: 'db88726d-1c62-56fa-94d5-4d147710c7ce',
    diagnosisId: 'cf7d23b4-51d8-53f7-be26-a92bf6682a6d',
    region: '대구 수성',
    industry: '디지털헬스·웨어러블 센서 제조',
    buyerRequest: '일본 PMDA 등급 분류, QMS 적합성, 일본어 IFU, 개인정보/클라우드 전송 설명, BLE 무선 인증, 임상적 성능 근거, 파일럿 100세트 견적 요청.',
    buyerDeadline: '2026-07-15',
    expectedShipmentDate: '2026-08-30',
    requestedCertifications: 'PMDA classification memo, Japanese IFU, FTO counsel memo, Wireless approval Japan pre-check, Privacy impact memo',
    currentDocuments: 'ISO13485 Certificate, BLE Module KC Registration, Clinical Evaluation Draft, IFU Korean Draft, Cloud Data Flow Diagram',
    exportExperience: '일본 수출 경험 없음, 국내 병원 파일럿 2건',
    budgetLimitKrw: '12000000',
  },
]

function normalizeSearchValue(value) {
  return String(value ?? '').toLowerCase().replace(/\s+/g, '')
}

function normalizeHsCode(value) {
  return String(value ?? '').replace(/[^\d]/g, '')
}

function splitList(value) {
  return String(value ?? '')
    .split(',')
    .map((item) => item.trim())
    .filter(Boolean)
}

function buildRerunFormDefaults(product) {
  return {
    region: '',
    industry: product?.category ?? '',
    buyerRequest: product?.buyerRequest ?? '',
    buyerDeadline: '',
    expectedShipmentDate: '',
    requestedCertifications: '',
    currentDocuments: '',
    exportExperience: '',
    budgetLimitKrw: '',
  }
}

// 수출 문의 판단 입력 페이지
// 제품 정보는 마이페이지에서만 입력·관리하고, 여기서는 "등록된 제품 선택 + 이번 건 판단 정보"만 받는다.
export default function DiagnosisNew() {
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()
  const preselectId = searchParams.get('productId') // 제품 맥락에서 진입 시 자동 선택할 제품
  const isRerunMode = searchParams.get('mode') === 'rerun'

  // 이번 수출 건 판단 정보 (제품 정보는 선택한 제품에서 가져옴)
  const [formData, setFormData] = useState({
    region: '',
    industry: '',
    buyerRequest: '',
    buyerDeadline: '',
    expectedShipmentDate: '',
    requestedCertifications: '',
    currentDocuments: '',
    exportExperience: '',
    budgetLimitKrw: '',
  })

  const [submitError, setSubmitError] = useState('')
  const [submitting, setSubmitting] = useState(false)
  const [pickerLoadError, setPickerLoadError] = useState('')

  const [savedProducts, setSavedProducts] = useState([])
  const [selectedId, setSelectedId] = useState(null)
  const selectedProduct = savedProducts.find((p) => p.id === selectedId) ?? null

  // 제품 선택 피커
  const [pickerOpen, setPickerOpen] = useState(false)
  const [pickerKeyword, setPickerKeyword] = useState('')
  const [pickerPage, setPickerPage] = useState(1)

  // 새 제품 등록 모달 (마이페이지 등록 흐름을 그대로 재사용 → 중복 경로 없음)
  const [addOpen, setAddOpen] = useState(false)
  const [addForm, setAddForm] = useState({ name: '', hs: '', country: 'DE', category: CATEGORIES[0], description: '' })
  const [addError, setAddError] = useState('')
  const [adding, setAdding] = useState(false)

  // 마이페이지에 저장한 제품 목록 로드 (+ 제품 맥락 진입 시 자동 선택)
  useEffect(() => {
    getMypageProducts()
      .then((products) => {
        setSavedProducts(products)
        setPickerLoadError('')
        const preselected = products.find((p) => p.id === preselectId)
        if (preselected) {
          setSelectedId(preselectId)
          if (isRerunMode) setFormData(buildRerunFormDefaults(preselected))
        }
      })
      .catch((err) => {
        setSavedProducts([])
        setPickerLoadError(err.message || '저장된 제품을 불러오지 못했습니다. 로그인 상태와 서버 연결을 확인해주세요.')
      })
  }, [preselectId, isRerunMode])

  const filteredProducts = useMemo(() => {
    const keyword = normalizeSearchValue(pickerKeyword)
    if (!keyword) return savedProducts
    return savedProducts.filter((product) => {
      const haystack = normalizeSearchValue([product.name, product.hs, product.country, product.description].join(' '))
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

  // 이번 건 판단 정보 입력값 변경
  const handleChange = (e) => {
    setFormData((prev) => ({ ...prev, [e.target.name]: e.target.value }))
    if (submitError) setSubmitError('')
  }

  const openPicker = () => { setPickerKeyword(''); setPickerPage(1); setPickerOpen(true) }
  const pickProduct = (p) => {
    setSelectedId(p.id)
    if (isRerunMode) setFormData(buildRerunFormDefaults(p))
    setPickerOpen(false)
    setSubmitError('')
  }
  const handlePickerSearch = (e) => { setPickerKeyword(e.target.value); setPickerPage(1) }
  const loadDemoScenario = (scenario) => {
    const product = savedProducts.find((p) => p.id === scenario.productId)
    if (!product) {
      setSubmitError(`${scenario.title} seed 제품이 현재 계정 제품 목록에 없습니다. ${scenario.account} / growgateadmin 계정으로 로그인해 주세요.`)
      return
    }
    setSelectedId(product.id)
    setFormData({
      region: scenario.region,
      industry: scenario.industry,
      buyerRequest: scenario.buyerRequest,
      buyerDeadline: scenario.buyerDeadline,
      expectedShipmentDate: scenario.expectedShipmentDate,
      requestedCertifications: scenario.requestedCertifications,
      currentDocuments: scenario.currentDocuments,
      exportExperience: scenario.exportExperience,
      budgetLimitKrw: scenario.budgetLimitKrw,
    })
    setSubmitError('synthetic_demo_not_real_customer 시나리오가 로드되었습니다. Seed는 live 공공 API proof가 아닙니다.')
  }

  // 새 제품 등록 (createMypageProduct = 마이페이지와 동일한 유일 생성 경로) → 등록 후 자동 선택
  const openAdd = () => {
    setAddForm({ name: '', hs: '', country: 'DE', category: CATEGORIES[0], description: '' })
    setAddError('')
    setAddOpen(true)
  }
  const submitAddProduct = async () => {
    const name = addForm.name.trim()
    const hs = normalizeHsCode(addForm.hs)
    const country = normalizeTargetCountry(addForm.country)
    if (!name) { setAddError('제품명을 입력해주세요.'); return }
    if (!/^\d{6,10}$/.test(hs)) { setAddError('HS Code는 숫자 6~10자리로 입력해주세요.'); return }

    // 중복 등록 방지 — 같은 제품이 이미 있으면 만들지 않고 그 제품을 선택
    const dup = savedProducts.find((p) =>
      p.name.trim().toLowerCase() === name.toLowerCase() &&
      normalizeHsCode(p.hs) === hs &&
      normalizeTargetCountry(p.country) === country
    )
    if (dup) {
      setAddError(`이미 등록된 제품이에요 (${dup.name} · HS ${dup.hs} · ${dup.country}). "취소" 후 저장된 제품에서 선택하세요.`)
      return
    }

    setAdding(true)
    setAddError('')
    try {
      const created = await createMypageProduct({
        name, hs,
        country: normalizeTargetCountry(addForm.country),
        category: addForm.category,
        description: addForm.description.trim(),
      })
      setSavedProducts((prev) => [...prev, created])
      setSelectedId(created.id)
      setAddOpen(false)
    } catch (err) {
      setAddError(err.message || '제품 등록에 실패했습니다. 잠시 후 다시 시도해주세요.')
    } finally {
      setAdding(false)
    }
  }

  // Gate 판단 시작 — 선택된 제품 + 이번 건 판단 정보로 진단 실행
  const handleSubmit = async () => {
    if (!selectedProduct) { setSubmitError('판단할 제품을 선택하거나 새로 등록해주세요.'); return }

    setSubmitting(true)
    setSubmitError('')
    try {
      const result = await runDiagnosis({
        productId: selectedProduct.id,
        productName: selectedProduct.name,
        hsCode: normalizeHsCode(selectedProduct.hs),
        targetCountry: normalizeTargetCountry(selectedProduct.country),
        description: selectedProduct.description ?? '',
        region: formData.region.trim(),
        industry: formData.industry.trim(),
        buyerRequest: formData.buyerRequest.trim(),
        buyerDeadline: formData.buyerDeadline || null,
        expectedShipmentDate: formData.expectedShipmentDate || null,
        requestedCertifications: splitList(formData.requestedCertifications),
        currentDocuments: splitList(formData.currentDocuments),
        exportExperience: formData.exportExperience.trim(),
        budgetLimitKrw: formData.budgetLimitKrw ? Number(formData.budgetLimitKrw) : null,
      })
      navigate(`/diagnosis/${result.id}`)
    } catch (err) {
      setSubmitError(err.message || '진단 실행에 실패했습니다. 입력값과 서버 상태를 확인해주세요.')
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
            <h2 className={styles.formTitle}>{isRerunMode ? '재진단 입력' : '수출 문의 판단'}</h2>
            <p className={styles.formDesc}>
              {isRerunMode
                ? '제품 정보는 유지하고, 이번 바이어 문의·회신 마감일·요구 인증·보유 문서를 다시 입력해 새 Gate 판단을 실행합니다.'
                : '등록된 제품을 선택하고, 이번 수출 문의(바이어 요구사항)를 입력하면 견적 전 Gate 판단으로 정리합니다.'}
              {' '}제품 정보는 마이페이지에서 관리됩니다.
            </p>

            <div className={styles.demoPanel}>
              <div>
                <div className={styles.demoTitle}>합성 시연 시나리오</div>
                <p className={styles.demoDesc}>synthetic_demo_not_real_customer 데이터입니다. live 공공 API proof로 계산하지 않습니다.</p>
              </div>
              <div className={styles.demoGrid}>
                {DEMO_SCENARIOS.map((scenario) => (
                  <div key={scenario.key} className={styles.demoCard}>
                    <strong>{scenario.title}</strong>
                    <span>Gate {scenario.gate}</span>
                    <span className={styles.demoAccount}>{scenario.account}</span>
                    <div className={styles.demoActions}>
                      <button type="button" onClick={() => loadDemoScenario(scenario)}>입력 로드</button>
                      <button type="button" onClick={() => navigate(`/diagnosis/${scenario.diagnosisId}`)}>완료 결과 보기</button>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* ① 판단할 제품 */}
            <div className={styles.stepHead}>
              <span className={styles.stepNum}>1</span>
              <div>
                <div className={styles.stepTitle}>판단할 제품</div>
                <div className={styles.stepSub}>이번 수출 건에 쓸 제품을 선택하세요</div>
              </div>
            </div>
            {selectedProduct ? (
              <div className={styles.productSummary}>
                <div>
                  <div className={styles.psName}>{selectedProduct.name}</div>
                  <div className={styles.psMeta}>
                    HS {selectedProduct.hs} · {selectedProduct.country} 수출{selectedProduct.category ? ` · ${selectedProduct.category}` : ''}
                  </div>
                  {selectedProduct.description && <div className={styles.psDesc}>{selectedProduct.description}</div>}
                </div>
                <div className={styles.psActions}>
                  <button className={styles.outlineBtn} onClick={() => setSelectedId(null)}>다른 제품 선택</button>
                  <button className={styles.linkBtn} onClick={() => navigate(`/mypage/knowledge/${selectedProduct.id}/edit`)}>
                    제품 정보 수정 →
                  </button>
                </div>
              </div>
            ) : savedProducts.length === 0 ? (
              <div className={styles.productPick}>
                {pickerLoadError && <div className={styles.fieldError}>{pickerLoadError}</div>}
                <div className={styles.pickEmptyState}>
                  <p>아직 등록된 제품이 없어요.<br />제품을 등록하면 판단을 시작할 수 있어요.</p>
                  <button className={styles.primaryOutlineBtn} onClick={openAdd}>+ 새 제품 등록</button>
                </div>
              </div>
            ) : (
              <>
                {pickerLoadError && <div className={styles.fieldError}>{pickerLoadError}</div>}
                <div className={styles.productSelectRow}>
                  <button className={styles.selectField} onClick={openPicker}>
                    <span>저장된 제품 ({savedProducts.length}개)</span>
                    <span className={styles.selectChevron}>▾</span>
                  </button>
                  <button className={styles.outlineBtn} onClick={openAdd}>+ 새 제품 등록</button>
                </div>
              </>
            )}

            {/* ② 미리보기 (제품 선택 전 흐름 안내) */}
            {!selectedProduct && (
              <>
                <div className={styles.stepHead}>
                  <span className={`${styles.stepNum} ${styles.stepNumMuted}`}>2</span>
                  <div>
                    <div className={styles.stepTitle}>이번 수출 문의 정보</div>
                    <div className={styles.stepSub}>제품을 선택하면 입력할 수 있어요</div>
                  </div>
                </div>
                <div className={styles.stepPreview}>
                  제품을 선택하면 바이어 문의 원문·회신 마감일·요구 인증·검토 예산 등 이번 건 정보를 입력하는 칸이 나타납니다.
                </div>
              </>
            )}

            {/* ② 이번 수출 문의 정보 (제품 선택 시에만) */}
            {selectedProduct && (
              <>
                <div className={styles.stepHead}>
                  <span className={styles.stepNum}>2</span>
                  <div>
                    <div className={styles.stepTitle}>이번 수출 문의 정보</div>
                    <div className={styles.stepSub}>
                      {isRerunMode
                        ? '이전 진단값을 재사용하지 않고 이번 문의 조건을 새로 입력합니다'
                        : '바이어 요구사항을 입력하면 견적 전 Gate 판단으로 정리합니다'}
                    </div>
                  </div>
                </div>
                <div className={styles.fields}>
                  <div className={styles.inlineGrid}>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="region">기업 지역</label>
                      <input id="region" className={styles.input} name="region"
                        value={formData.region} onChange={handleChange} placeholder="예: 경남 / 창원 / 부산" />
                    </div>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="industry">지역 업종</label>
                      <input id="industry" className={styles.input} name="industry"
                        value={formData.industry} onChange={handleChange} placeholder="예: 전기전자 · 배터리" />
                    </div>
                  </div>
                  <div className={styles.field}>
                    <label className={styles.label} htmlFor="buyerRequest">바이어 문의 원문</label>
                    <textarea
                      id="buyerRequest" className={styles.textarea} name="buyerRequest"
                      value={formData.buyerRequest} onChange={handleChange}
                      placeholder="예: 독일 바이어가 CE, UN38.3 보유 여부와 4주 내 샘플 출하 가능 여부를 문의"
                      rows={4}
                    />
                  </div>
                  <div className={styles.inlineGrid}>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="buyerDeadline">회신 마감일</label>
                      <input id="buyerDeadline" className={styles.input} name="buyerDeadline" type="date"
                        value={formData.buyerDeadline} onChange={handleChange} />
                    </div>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="expectedShipmentDate">예상 출하일</label>
                      <input id="expectedShipmentDate" className={styles.input} name="expectedShipmentDate" type="date"
                        value={formData.expectedShipmentDate} onChange={handleChange} />
                    </div>
                  </div>
                  <div className={styles.inlineGrid}>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="requestedCertifications">요구 인증</label>
                      <input id="requestedCertifications" className={styles.input} name="requestedCertifications"
                        value={formData.requestedCertifications} onChange={handleChange} placeholder="예: CE, UN38.3, IEC62133" />
                    </div>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="currentDocuments">현재 보유 문서</label>
                      <input id="currentDocuments" className={styles.input} name="currentDocuments"
                        value={formData.currentDocuments} onChange={handleChange} placeholder="예: KC 인증서, 시험성적서" />
                    </div>
                  </div>
                  <div className={styles.inlineGrid}>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="exportExperience">수출 경험</label>
                      <input id="exportExperience" className={styles.input} name="exportExperience"
                        value={formData.exportExperience} onChange={handleChange} placeholder="예: 첫 수출 / EU 납품 경험 있음" />
                    </div>
                    <div className={styles.field}>
                      <label className={styles.label} htmlFor="budgetLimitKrw">검토 가능 예산</label>
                      <input id="budgetLimitKrw" className={styles.input} name="budgetLimitKrw" type="number" min="0"
                        value={formData.budgetLimitKrw} onChange={handleChange} placeholder="예: 5000000" />
                    </div>
                  </div>
                </div>

                {submitError && <div className={styles.fieldError}>{submitError}</div>}

                <button className={styles.submitBtn} onClick={handleSubmit} disabled={submitting}>
                  {submitting ? 'Gate 판단 요청 중…' : '수출 Gate 판단 시작 →'}
                </button>
              </>
            )}

            {!selectedProduct && submitError && <div className={styles.fieldError}>{submitError}</div>}
          </div>
        </div>
      </div>

      {/* 제품 선택 피커 모달 */}
      {pickerOpen && (
        <div className={styles.modal} onClick={(e) => { if (e.target === e.currentTarget) setPickerOpen(false) }}>
          <div className={styles.sheet}>
            <h3 className={styles.sheetTitle}>제품 선택</h3>
            <p className={styles.sheetDesc}>마이페이지에 등록한 제품 중에서 판단할 제품을 선택하세요.</p>

            <div className={styles.pickSearch}>
              <span className={styles.pickSearchIcon} aria-hidden="true">⌕</span>
              <input
                className={styles.pickSearchInput} type="search"
                value={pickerKeyword} onChange={handlePickerSearch}
                placeholder="제품명, HS Code, 대상국 키워드로 검색" aria-label="저장된 제품 검색"
              />
              {pickerKeyword && (
                <button className={styles.pickSearchClear} onClick={() => { setPickerKeyword(''); setPickerPage(1) }}>초기화</button>
              )}
            </div>

            <div className={styles.pickMetaRow}>
              <span>총 <b>{filteredProducts.length}</b>개 제품 · 5개씩 보기</span>
              <span className={styles.pickExamples}>제품명 · HS Code · 대상국 · 설명 검색</span>
            </div>

            <div className={styles.pickList}>
              {savedProducts.length === 0 && (
                <div className={styles.pickEmpty}>저장된 제품이 없습니다. 마이페이지에서 먼저 제품을 등록하세요.</div>
              )}
              {savedProducts.length > 0 && filteredProducts.length === 0 && (
                <div className={styles.pickEmpty}>검색 결과가 없습니다. 제품명, HS Code, 대상국 또는 설명 키워드를 다시 입력해 주세요.</div>
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
                  <button className={styles.pickPageButton} disabled={safePickerPage === 1}
                    onClick={() => setPickerPage((page) => Math.max(1, page - 1))}>이전</button>
                  {visiblePickerPages.map((page) => (
                    <button key={page}
                      className={`${styles.pickPageButton} ${page === safePickerPage ? styles.activePage : ''}`}
                      onClick={() => setPickerPage(page)} aria-current={page === safePickerPage ? 'page' : undefined}>
                      {page}
                    </button>
                  ))}
                  <button className={styles.pickPageButton} disabled={safePickerPage === pickerTotalPages}
                    onClick={() => setPickerPage((page) => Math.min(pickerTotalPages, page + 1))}>다음</button>
                </div>
              </div>
            )}

            <div className={styles.sheetFoot}>
              <button className={styles.outlineBtn} onClick={() => { setPickerOpen(false); openAdd() }}>+ 새 제품 등록</button>
              <button className={styles.sheetCancel} onClick={() => setPickerOpen(false)}>닫기</button>
            </div>
          </div>
        </div>
      )}

      {/* 새 제품 등록 모달 (마이페이지 등록과 동일 경로) */}
      {addOpen && (
        <div className={styles.modal} onClick={(e) => { if (e.target === e.currentTarget && !adding) setAddOpen(false) }}>
          <div className={styles.sheet}>
            <h3 className={styles.sheetTitle}>새 제품 등록</h3>
            <p className={styles.sheetDesc}>제품 정보는 마이페이지에 저장되고, 등록 후 이번 판단에 바로 선택됩니다.</p>

            <div className={styles.field}>
              <label className={styles.label}>제품명 *</label>
              <input className={styles.input} value={addForm.name} placeholder="예: 리튬이온 배터리팩"
                onChange={(e) => setAddForm({ ...addForm, name: e.target.value })} />
            </div>
            <div className={styles.field}>
              <label className={styles.label}>HS Code *</label>
              <input className={styles.input} value={addForm.hs} placeholder="예: 850760" inputMode="numeric"
                onChange={(e) => setAddForm({ ...addForm, hs: e.target.value })} />
            </div>
            <div className={styles.inlineGrid}>
              <div className={styles.field}>
                <label className={styles.label}>수출 대상국</label>
                <select className={styles.input} value={addForm.country}
                  onChange={(e) => setAddForm({ ...addForm, country: e.target.value })}>
                  {COUNTRY_OPTIONS.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
              <div className={styles.field}>
                <label className={styles.label}>카테고리</label>
                <select className={styles.input} value={addForm.category}
                  onChange={(e) => setAddForm({ ...addForm, category: e.target.value })}>
                  {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
                </select>
              </div>
            </div>
            <div className={styles.field}>
              <label className={styles.label}>제품 설명</label>
              <textarea className={styles.textarea} rows={3} value={addForm.description}
                placeholder="제품의 주요 특성·용도·인증 현황 등"
                onChange={(e) => setAddForm({ ...addForm, description: e.target.value })} />
            </div>

            {addError && <div className={styles.fieldError}>{addError}</div>}

            <div className={styles.sheetFoot}>
              <button className={styles.sheetCancel} onClick={() => setAddOpen(false)} disabled={adding}>취소</button>
              <button className={styles.sheetSave} onClick={submitAddProduct} disabled={adding}>
                {adding ? '등록 중…' : '등록하고 선택'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
