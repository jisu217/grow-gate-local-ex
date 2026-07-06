import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './Mypage.module.css'
import {
  getMypageOverview,
  getMypageProducts,
  createMypageProduct,
  deleteMypageProduct,
  getMypageReport,
  buildRerunDiagnosisPath,
} from '../../services/mypageService.js'
import { getDiagnosisResult, buildDecisionBrief } from '../../services/diagnosisService.js'
import { COUNTRY_OPTIONS, normalizeTargetCountry } from '../../utils/country.js'
import MarkdownReportPreview, { reportPreviewDocument } from '../../components/report/MarkdownReportPreview.jsx'
import ReportDownloadButtons from '../../components/report/ReportDownloadButtons.jsx'
import StatusMessage from '../../components/ui/StatusMessage.jsx'

// 내 제품 관리 목록: 한 페이지에 보여줄 제품 수
const PRODUCTS_PER_PAGE = 5

// 제품 카테고리 (제품 수정 페이지와 동일 목록)
const CATEGORIES = ['전기전자', '전자부품', '기계·장비', '화학·소재', '식품·의료', '기타']

// 판정 상태 → 색상 클래스 (DiagnosisDetail과 동일 규칙)
function gateClassOf(status) {
  const s = String(status ?? '').toUpperCase()
  if (s.includes('NO')) return 'gateNo'
  if (s.includes('GO') && !s.includes('CONDITION')) return 'gateGo'
  return 'gateCond'
}

// 마이페이지 — 수출 준비 상태 대시보드
export default function Mypage() {
  const navigate = useNavigate()
  const [overview, setOverview] = useState({ company: null, members: [] })
  const [products, setProducts] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [loadError, setLoadError] = useState('')
  const [saveError, setSaveError] = useState('')
  const [reportError, setReportError] = useState('')
  const [reportBusyId, setReportBusyId] = useState('')
  const [reportPreview, setReportPreview] = useState(null)

  // 제품 추가 모달 상태
  const [modalOpen, setModalOpen] = useState(false)
  const [form, setForm] = useState({ name: '', hs: '', country: 'DE', category: CATEGORIES[0], description: '' })

  // 제품 삭제 확인 모달 대상 (null이면 닫힘)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  // 내 제품 관리 목록 페이지 (0부터)
  const [productPage, setProductPage] = useState(0)

  // 선택 제품의 최신 판정(GO/조건부/NO-GO). 요약엔 판정값이 없어 진단 상세에서 가져온다.
  const [verdict, setVerdict] = useState(null)
  const [verdictState, setVerdictState] = useState('idle') // idle | loading | ok | none | error

  // 대시보드 로드 재시도 트리거
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let cancelled = false
    async function load() {
      const [overviewResult, productsResult] = await Promise.allSettled([
        getMypageOverview(),
        getMypageProducts(),
      ])
      if (cancelled) return
      const messages = []
      if (overviewResult.status === 'fulfilled') {
        setOverview(overviewResult.value)
      } else {
        messages.push(overviewResult.reason?.message || '회사/계정 정보를 불러오지 못했습니다.')
      }
      if (productsResult.status === 'fulfilled') {
        setProducts(productsResult.value)
        if (productsResult.value.length) setSelectedId(productsResult.value[0].id)
      } else {
        messages.push(productsResult.reason?.message || '제품 목록을 불러오지 못했습니다.')
      }
      setLoadError(messages.join(' '))
    }
    load()
    return () => { cancelled = true }
  }, [reloadKey])

  const current = products.find((p) => p.id === selectedId)

  // 선택 제품의 최신 진단 상세에서 실제 판정값을 가져온다 (점수로 지어내지 않음).
  useEffect(() => {
    let alive = true
    if (!current) { setVerdict(null); setVerdictState('idle'); return }
    if (!current.runId) { setVerdict(null); setVerdictState('none'); return }
    setVerdictState('loading')
    getDiagnosisResult(current.runId)
      .then((result) => {
        if (!alive) return
        const session = result.session ?? {}
        const stages = result.stages ?? {}
        const brief = buildDecisionBrief(session, stages)
        const mustFix = Array.isArray(session.gateDecision?.mustFixBeforeQuote)
          ? session.gateDecision.mustFixBeforeQuote.filter(Boolean)
          : []
        const risks = mustFix.length ? mustFix : brief.topRisks.map((r) => r.label)
        setVerdict({
          gateLabel: brief.gateLabel,
          gateReason: brief.gateReason,
          gateCls: gateClassOf(session.gateDecision?.status),
          risks,
        })
        setVerdictState('ok')
      })
      .catch(() => { if (alive) { setVerdict(null); setVerdictState('error') } })
    return () => { alive = false }
  }, [current?.id, current?.runId])

  // 내 제품 관리 목록 페이지네이션 (제품 수가 바뀌어도 범위 벗어나지 않게 보정)
  const pageCount = Math.max(1, Math.ceil(products.length / PRODUCTS_PER_PAGE))
  const safePage = Math.min(productPage, pageCount - 1)
  const pagedProducts = products.slice(safePage * PRODUCTS_PER_PAGE, safePage * PRODUCTS_PER_PAGE + PRODUCTS_PER_PAGE)

  // 제품 선택 → 위 대시보드를 그 제품으로 전환 (상단 드롭다운과 동기화)
  function selectProduct(id) {
    setSelectedId(id)
    document.querySelector(`.${styles.slabel}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  // 제품 추가 모달 열기/닫기 (기본 정보 수정은 '제품 정보' 상세 페이지에서)
  function openAdd() {
    setForm({ name: '', hs: '', country: 'DE', category: CATEGORIES[0], description: '' })
    setSaveError('')
    setModalOpen(true)
  }
  function closeModal() { setModalOpen(false) }

  // 제품 추가
  async function saveProduct() {
    const name = form.name.trim()
    const hs = form.hs.trim()
    const country = normalizeTargetCountry(form.country)
    if (!name || !hs) { alert('제품명과 HS Code를 입력해주세요.'); return }

    // 중복 등록 방지 (제품명 + HS Code + 대상국이 같으면 막음)
    const dup = products.find((p) =>
      p.name.trim().toLowerCase() === name.toLowerCase() &&
      String(p.hs).replace(/\D/g, '') === hs.replace(/\D/g, '') &&
      normalizeTargetCountry(p.country) === country
    )
    if (dup) {
      setSaveError(`이미 같은 제품이 등록돼 있어요 (${dup.name} · HS ${dup.hs} · ${dup.country}). 중복 등록은 막습니다.`)
      return
    }

    try {
      const created = await createMypageProduct({
        name, hs, country,
        category: form.category,
        description: form.description.trim(),
      })
      setProducts((prev) => [...prev, created])
      setSelectedId(created.id)
      closeModal()
    } catch (err) {
      setSaveError(err.message || '제품 저장에 실패했습니다. 입력값과 서버 상태를 확인해주세요.')
    }
  }

  // 삭제 버튼 → 확인 모달 열기
  function askDelete(id) {
    const p = products.find((x) => x.id === id)
    if (p) setDeleteTarget(p)
  }

  // 확인 모달에서 '삭제' → 실제 소프트 삭제 API 호출 (감사 로그·진단 이력은 보존됨)
  async function confirmDelete() {
    const id = deleteTarget.id
    setDeleting(true)
    try {
      await deleteMypageProduct(id)
      setProducts((prev) => {
        const next = prev.filter((x) => x.id !== id)
        // 삭제한 게 지금 보던 제품이면 첫 제품으로 전환
        if (id === selectedId) setSelectedId(next[0]?.id ?? '')
        return next
      })
      setDeleteTarget(null)
    } catch (err) {
      alert(err.message || '제품 삭제에 실패했습니다. 권한이 없거나 서버 오류일 수 있어요.')
    } finally {
      setDeleting(false)
    }
  }

  // 수출 Gate 판단 다시 실행 — 이번 건 바이어 문의/마감/요구 인증을 다시 입력받는다.
  function rerun() {
    if (!current) return
    navigate(buildRerunDiagnosisPath(current))
  }

  async function fetchReport(report) {
    setReportError('')
    setReportBusyId(report.diagnosisId)
    try {
      const payload = await getMypageReport(report.diagnosisId)
      return {
        ...report,
        ...payload,
        markdown: payload.markdown ?? '',
        fileName: payload.fileName ?? report.file,
        contentType: payload.contentType ?? 'text/markdown;charset=utf-8',
      }
    } finally {
      setReportBusyId('')
    }
  }

  async function downloadReport(report) {
    try {
      const payload = await fetchReport(report)
      const blob = new Blob([payload.markdown], { type: payload.contentType })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = payload.fileName
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      setReportError(err.message || 'Markdown 리포트 다운로드에 실패했습니다.')
    }
  }

  async function previewReport(report) {
    try {
      const payload = await fetchReport(report)
      setReportPreview(payload)
    } catch (err) {
      setReportError(err.message || 'HTML 미리보기를 불러오지 못했습니다.')
    }
  }

  async function printReport(report) {
    try {
      const payload = await fetchReport(report)
      const printWindow = window.open('', '_blank')
      if (!printWindow) {
        setReportPreview(payload)
        setReportError('팝업이 차단되어 HTML 미리보기를 열었습니다. 미리보기에서 브라우저 인쇄를 사용하세요.')
        return
      }
      printWindow.document.open()
      printWindow.document.write(reportPreviewDocument(payload.markdown))
      printWindow.document.close()
      printWindow.focus()
      window.setTimeout(() => printWindow.print(), 200)
    } catch (err) {
      setReportError(err.message || '브라우저 인쇄 화면을 열지 못했습니다.')
    }
  }

  return (
    <div className={styles.root}>
      <div className={styles.bgfx}>
        <div className={`${styles.aurora} ${styles.a1}`} />
        <div className={`${styles.aurora} ${styles.a2}`} />
        <div className={styles.grid} />
      </div>

      <div className={styles.page}>
        {/* HEAD + switcher */}
        <div className={styles.phead}>
          <div>
            <h1>마이페이지</h1>
            <div className={styles.sub}>수출 준비 상황을 한눈에 확인하고, 다음 할 일로 넘어가세요.</div>
          </div>
          <div className={styles.switcher}>
            <span className={styles.lab}>지금 보는 제품</span>
            <select value={selectedId} onChange={(e) => setSelectedId(e.target.value)}>
              {products.map((p) => (
                <option key={p.id} value={p.id}>{p.name} · {p.country}</option>
              ))}
            </select>
            <span className={styles.count}>(총 {products.length}개 제품 중)</span>
          </div>
        </div>

        {/* PRODUCT-LEVEL */}
        <div className={styles.slabel}>
          <span className={styles.dot} />제품별 진단 현황 · 제품을 바꾸면 아래 카드가 갱신됩니다
        </div>

        {current && (
          <>
            <div className={`${styles.row} ${styles.hero}`}>
              <div
                className={`${styles.card} ${styles.score} ${styles.clickable}`}
                onClick={() => navigate(`/mypage/diagnosis/${current.id}`)}
                role="button"
              >
                {verdictState === 'ok' && verdict ? (
                  <>
                    <div className={styles.gateRow}>
                      <span className={`${styles.gateBadge} ${styles[verdict.gateCls]}`}>
                        <span className={styles.gateDot} />{verdict.gateLabel}
                      </span>
                      {current.score != null && (
                        <span className={styles.refScore}>참고 점수 <b>{current.score}</b>/100</span>
                      )}
                    </div>
                    {verdict.gateReason && <p className={styles.gateReason}>{verdict.gateReason}</p>}
                    {verdict.risks.length > 0 && (
                      <div className={styles.riskList}>
                        <div className={styles.riskHead}>핵심 리스크 {verdict.risks.length}건</div>
                        {verdict.risks.slice(0, 3).map((r, i) => (
                          <div key={i} className={styles.riskItem}>
                            <span className={styles.riskNum}>{i + 1}</span><span>{r}</span>
                          </div>
                        ))}
                      </div>
                    )}
                    {current.runId && (
                      <button
                        className={styles.fullResultBtn}
                        onClick={(e) => { e.stopPropagation(); navigate(`/diagnosis/${current.runId}`) }}
                      >
                        전체 결과 보기 →
                      </button>
                    )}
                  </>
                ) : verdictState === 'loading' ? (
                  <div className={styles.big}>…</div>
                ) : (
                  <>
                    <div className={styles.big}>
                      {current.score != null ? current.score : '—'}<span className={styles.u}>/100</span>
                    </div>
                    <div className={styles.chips}>
                      {verdictState === 'error'
                        ? <span className={styles.chip}>판정 정보를 불러오지 못했습니다</span>
                        : current.chips.length
                        ? current.chips.map((c) => <span key={c} className={styles.chip}>{c}</span>)
                        : <span className={styles.chip}>아직 진단 전입니다</span>}
                    </div>
                  </>
                )}
              </div>

              <div className={`${styles.card} ${styles.todo}`}>
                <div className={styles.ctitle}>오늘 할 일 {current.runId && <span className={styles.goHint}>· 30·60·90일 보드로 →</span>}</div>
                <div className={styles.list}>
                  {current.todo.map((t, i) => (
                    <div
                      key={i}
                      className={`${styles.item} ${current.runId ? styles.clickableItem : ''}`}
                      onClick={() => current.runId && navigate(`/diagnosis/${current.runId}?step=8`)}
                    >
                      <span className={styles.box} />
                      <span>{t.task}</span>
                      <span className={`${styles.tag} ${styles[`t${t.due}`]}`}>{t.due}일</span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            <div className={`${styles.row} ${styles.two}`}>
              <div
                className={`${styles.card} ${styles.kn} ${styles.clickable}`}
                onClick={() => document.getElementById('my-products')?.scrollIntoView({ behavior: 'smooth', block: 'start' })}
                role="button"
              >
                <div className={styles.ctitle}>제품 정보 등록 현황 <span className={styles.goHint}>· 내 제품 보러 가기 →</span></div>
                <div className={styles.pct}>{current.kn}%</div>
                <div className={styles.bar}>
                  <div className={styles.fill} style={{ width: `${current.kn}%` }} />
                </div>
                <div className={styles.meta}><span>데이터 기준</span><span>{current.miss}</span></div>
              </div>

              <div className={`${styles.card} ${styles.rep}`}>
                <div className={styles.ctitle}>이 제품 리포트 보관함</div>
                <div className={styles.files}>
                  {current.reports.length
                    ? current.reports.map((r) => (
                      <div key={r.diagnosisId} className={styles.f}>
                        <span className={styles.ic}>MD</span>
                        <span className={styles.reportName}>{r.file}</span>
                        <span className={styles.dt}>{r.date}</span>
                        <ReportDownloadButtons
                          report={r}
                          onDownload={downloadReport}
                          onPreview={previewReport}
                          onPrint={printReport}
                          disabled={reportBusyId === r.diagnosisId}
                        />
                      </div>
                    ))
                    : <div className={styles.empty}>아직 생성된 리포트가 없습니다.</div>}
                </div>
                {reportError && <div className={styles.reportError}>{reportError}</div>}
              </div>
            </div>

            <div className={styles.rerunWrap}>
              <button
                className={styles.rerun}
                onClick={rerun}
              >
                <span className={styles.sp} />
                <span className={styles.ar}>↻</span>&nbsp;이번 문의 정보 입력 후 재진단
              </button>
            </div>
          </>
        )}

        {/* COMPANY-LEVEL */}
        <div className={`${styles.slabel} ${styles.company}`}>
          <span className={styles.dot} />내 제품 · 회사 정보 · 제품을 바꿔도 회사 정보는 고정됩니다
        </div>

        {loadError && (
          <StatusMessage
            type="error"
            message={`${loadError} 네트워크를 확인하고 다시 시도해주세요.`}
            onAction={() => setReloadKey((k) => k + 1)}
          />
        )}

        <div id="my-products" className={`${styles.card} ${styles.pm}`}>
          <div className={styles.pmHead}>
            <div className={styles.ctitle}>내 제품 관리</div>
            <button className={styles.add} onClick={openAdd}>+ 제품 추가</button>
          </div>
          <div className={styles.plist}>
            {pagedProducts.map((p) => (
              <div
                key={p.id}
                className={`${styles.prow} ${p.id === selectedId ? styles.cur : ''}`}
                onClick={() => selectProduct(p.id)}
                role="button"
                title="클릭하면 이 제품으로 전환됩니다"
              >
                <div>
                  <div className={styles.pn}>{p.name}</div>
                  <div className={styles.pmeta}>HS {p.hs} · {p.country} 수출 · 등록률 {p.kn}%</div>
                  {p.chips.some((c) => c.includes('누락') || c.includes('만료')) && (
                    <span
                      className={`pill ${p.chips.some((c) => c.includes('누락')) ? 'req' : 'warn'}`}
                      style={{ marginTop: 4, display: 'inline-block' }}
                    >
                      {p.chips.find((c) => c.includes('누락') || c.includes('만료'))}
                    </span>
                  )}
                </div>
                <span className={`${styles.badge} ${p.id === selectedId ? styles.badgeCur : ''}`}>
                  {p.id === selectedId ? '지금 보는 제품' : '클릭해 전환'}
                </span>
                {p.score != null
                  ? <span className={styles.sc}>{p.score}<span className={styles.scU}>점</span></span>
                  : <span className={`${styles.sc} ${styles.scNone}`}>미진단</span>}
                <div className={styles.acts}>
                  <button className={`${styles.ab} ${styles.abPrimary}`} onClick={(e) => { e.stopPropagation(); navigate(`/mypage/diagnosis/${p.id}`) }}>판단 근거</button>
                  <button className={styles.ab} onClick={(e) => { e.stopPropagation(); navigate(`/mypage/knowledge/${p.id}`) }}>제품 정보</button>
                  <button className={`${styles.ab} ${styles.abDanger}`} onClick={(e) => { e.stopPropagation(); askDelete(p.id) }}>삭제</button>
                </div>
              </div>
            ))}
          </div>

          {/* 제품이 한 페이지를 넘으면 페이지네이션 노출 */}
          {pageCount > 1 && (
            <div className={styles.ppager}>
              <button
                className={styles.ppBtn}
                onClick={() => setProductPage((n) => Math.max(0, n - 1))}
                disabled={safePage === 0}
              >
                ← 이전
              </button>
              <span className={styles.ppInfo}>{safePage + 1} / {pageCount}</span>
              <button
                className={styles.ppBtn}
                onClick={() => setProductPage((n) => Math.min(pageCount - 1, n + 1))}
                disabled={safePage >= pageCount - 1}
              >
                다음 →
              </button>
            </div>
          )}
        </div>

        <div className={`${styles.row} ${styles.companyRow}`}>
          <div className={styles.info}>
            <div className={styles.ctitle}>회사 정보</div>
            <div className={styles.kv}>
              <div className={styles.r}><span className={styles.k}>기업명</span><span className={styles.v}>{overview.company?.name}</span></div>
              <div className={styles.r}><span className={styles.k}>업종</span><span className={styles.v}>{overview.company?.industry}</span></div>
              <div className={styles.r}><span className={styles.k}>담당자</span><span className={styles.v}>{overview.company?.manager}</span></div>
              <div className={styles.r}><span className={styles.k}>이메일</span><span className={styles.v}>{overview.company?.email}</span></div>
            </div>
          </div>
          <div className={styles.info}>
            <div className={styles.ctitle}>팀원 권한 / 역할</div>
            <div className={styles.members}>
              {overview.members.map((m) => (
                <div key={m.name} className={styles.m}>
                  <span className={styles.av}>{m.initial}</span>
                  <span className={styles.nm}>{m.name}</span>
                  <span className={`${styles.mrole} ${styles[m.role]}`}>{m.roleLabel}</span>
                </div>
              ))}
            </div>
          </div>
        </div>

        <footer className={styles.footer}>
          <div className={styles.fBrand}>GROW-GATE <span className={styles.ex}>Local EX</span></div>
          <div className={styles.fNote}>
            2026 산업통상부 공공데이터 활용 공모전 · 본 결과는 공공데이터 수집 신호와 규칙 기반 Gate 판단이며 담당자 의사결정은 전문가 검토와 함께 하십시오.
          </div>
        </footer>
      </div>

      {/* 제품 추가 / 수정 모달 */}
      <div
        className={`${styles.modal} ${modalOpen ? styles.open : ''}`}
        onClick={(e) => { if (e.target === e.currentTarget) closeModal() }}
      >
        <div className={styles.sheet}>
          <h3>제품 추가</h3>
          <div className={styles.desc}>
            제품 정보를 등록해두면 다음 Gate 판단부터 제품명·HS Code·대상국을 자동으로 불러와 재사용합니다.
          </div>
          <div className={styles.field}>
            <label>제품명</label>
            <input
              type="text" placeholder="예: 리튬배터리 팩"
              value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })}
            />
          </div>
          <div className={styles.field}>
            <label>HS Code</label>
            <input
              type="text" placeholder="예: 850760"
              value={form.hs} onChange={(e) => setForm({ ...form, hs: e.target.value })}
            />
          </div>
          <div className={styles.field}>
            <label>수출 대상국</label>
            <select value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })}>
              {COUNTRY_OPTIONS.map((c) => <option key={c}>{c}</option>)}
            </select>
          </div>
          <div className={styles.field}>
            <label>카테고리</label>
            <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })}>
              {CATEGORIES.map((c) => <option key={c} value={c}>{c}</option>)}
            </select>
          </div>
          <div className={styles.field}>
            <label>제품 설명</label>
            <textarea
              rows={3}
              placeholder="제품의 주요 특성·용도·인증 현황 등"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          {saveError && <div className={styles.modalError}>{saveError}</div>}
          <div className={styles.foot}>
            <button className={styles.cancel} onClick={closeModal}>취소</button>
            <button className={styles.save} onClick={saveProduct}>추가</button>
          </div>
        </div>
      </div>

      {/* 제품 삭제 확인 모달 */}
      <div
        className={`${styles.modal} ${deleteTarget ? styles.open : ''}`}
        onClick={(e) => { if (e.target === e.currentTarget && !deleting) setDeleteTarget(null) }}
      >
        <div className={styles.sheet}>
          <h3>제품을 삭제할까요?</h3>
          <div className={styles.desc}>
            <b>{deleteTarget?.name}</b> 제품을 목록에서 삭제합니다.<br />
            지금까지의 <b>Gate 판단 이력과 감사 로그는 그대로 보존</b>돼요.
          </div>
          <div className={styles.foot}>
            <button className={styles.cancel} onClick={() => setDeleteTarget(null)} disabled={deleting}>취소</button>
            <button className={styles.saveDanger} onClick={confirmDelete} disabled={deleting}>
              {deleting ? '삭제 중…' : '삭제'}
            </button>
          </div>
        </div>
      </div>

      {/* Markdown 리포트 HTML 미리보기 */}
      <div
        className={`${styles.modal} ${reportPreview ? styles.open : ''}`}
        onClick={(e) => { if (e.target === e.currentTarget) setReportPreview(null) }}
      >
        <div className={`${styles.sheet} ${styles.previewSheet}`}>
          <div className={styles.previewHead}>
            <div>
              <h3>HTML 미리보기</h3>
              <div className={styles.desc}>공식 원본은 Markdown입니다. PDF 보관은 브라우저 인쇄를 사용하세요.</div>
            </div>
            <button className={styles.cancel} onClick={() => setReportPreview(null)}>닫기</button>
          </div>
          <div className={styles.previewBody}>
            <MarkdownReportPreview markdown={reportPreview?.markdown ?? ''} />
          </div>
          <div className={styles.foot}>
            <button className={styles.cancel} onClick={() => setReportPreview(null)}>닫기</button>
            <button className={styles.save} onClick={() => printReport(reportPreview)}>
              브라우저에서 PDF로 인쇄
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
