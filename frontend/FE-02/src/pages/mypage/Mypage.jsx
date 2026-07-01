import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './Mypage.module.css'
import {
  getMypageOverview,
  getMypageProducts,
  createMypageProduct,
  updateMypageProduct,
  deleteMypageProduct,
  rerunDiagnosis,
} from '../../services/mypageService.js'
import { COUNTRY_OPTIONS, normalizeTargetCountry } from '../../utils/country.js'

// 내 제품 관리 목록: 한 페이지에 보여줄 제품 수
const PRODUCTS_PER_PAGE = 5

// 마이페이지 — 수출 준비 상태 대시보드
export default function Mypage() {
  const navigate = useNavigate()
  const [overview, setOverview] = useState({ company: null, members: [] })
  const [products, setProducts] = useState([])
  const [selectedId, setSelectedId] = useState('')
  const [loadError, setLoadError] = useState('')
  const [saveError, setSaveError] = useState('')

  // 제품 추가 모달 상태
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({ name: '', hs: '', country: 'DE' })

  // 제품 삭제 확인 모달 대상 (null이면 닫힘)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleting, setDeleting] = useState(false)

  // 진단 재실행 로딩
  const [rerunLoading, setRerunLoading] = useState(false)

  // 내 제품 관리 목록 페이지 (0부터)
  const [productPage, setProductPage] = useState(0)

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
  }, [])

  const current = products.find((p) => p.id === selectedId)

  // 내 제품 관리 목록 페이지네이션 (제품 수가 바뀌어도 범위 벗어나지 않게 보정)
  const pageCount = Math.max(1, Math.ceil(products.length / PRODUCTS_PER_PAGE))
  const safePage = Math.min(productPage, pageCount - 1)
  const pagedProducts = products.slice(safePage * PRODUCTS_PER_PAGE, safePage * PRODUCTS_PER_PAGE + PRODUCTS_PER_PAGE)

  // 제품 추가 모달 열기/닫기
  function openAdd() {
    setEditingId(null)
    setForm({ name: '', hs: '', country: 'DE' })
    setSaveError('')
    setModalOpen(true)
  }
  function openEdit(id) {
    const p = products.find((x) => x.id === id)
    if (!p) return
    setEditingId(id)
    setForm({ name: p.name, hs: p.hs, country: normalizeTargetCountry(p.country) })
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

    try {
      if (editingId) {
        await updateMypageProduct(editingId, { name, hs, country })
        setProducts((prev) => prev.map((p) =>
          p.id === editingId ? { ...p, name, hs, country } : p))
      } else {
        const created = await createMypageProduct({ name, hs, country })
        setProducts((prev) => [...prev, created])
        setSelectedId(created.id)
      }
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

  // 수출 준비도 진단 다시 실행
  async function rerun() {
    if (!current) return
    setRerunLoading(true)
    try {
      const result = await rerunDiagnosis(current.id, current.country)
      navigate(`/diagnosis/${result.diagnosisId}`)
    } catch (err) {
      alert(err.message || '진단 실행에 실패했습니다.')
    } finally {
      setRerunLoading(false)
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
                <div className={styles.ctitle}>최근 AI 진단 결과 <span className={styles.goHint}>· 왜 이 점수인지 보기 →</span></div>
                <div className={styles.big}>
                  {current.score != null ? current.score : '—'}<span className={styles.u}>/100</span>
                </div>
                <div className={styles.chips}>
                  {current.chips.length
                    ? current.chips.map((c) => <span key={c} className={styles.chip}>{c}</span>)
                    : <span className={styles.chip}>아직 진단 전입니다</span>}
                </div>
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
                onClick={() => navigate('/mypage/knowledge')}
                role="button"
              >
                <div className={styles.ctitle}>제품 지식 등록 현황 <span className={styles.goHint}>· 지식 입력하러 가기 →</span></div>
                <div className={styles.pct}>{current.kn}%</div>
                <div className={styles.bar}>
                  <div className={styles.fill} style={{ width: `${current.kn}%` }} />
                </div>
                <div className={styles.meta}><span>입력 완료</span><span>{current.miss}</span></div>
              </div>

              <div className={`${styles.card} ${styles.rep}`}>
                <div className={styles.ctitle}>이 제품 리포트 보관함</div>
                <div className={styles.files}>
                  {current.reports.length
                    ? current.reports.map((r) => (
                      <div key={r.file} className={styles.f}>
                        <span className={styles.ic}>⬢</span>
                        <span>{r.file}</span>
                        <span className={styles.dt}>{r.date}</span>
                      </div>
                    ))
                    : <div className={styles.empty}>아직 생성된 리포트가 없습니다.</div>}
                </div>
              </div>
            </div>

            <div className={styles.rerunWrap}>
              <button
                className={`${styles.rerun} ${rerunLoading ? styles.loading : ''}`}
                onClick={rerun}
                disabled={rerunLoading}
              >
                <span className={styles.sp} />
                <span className={styles.ar}>↻</span>&nbsp;이 제품 준비도 진단 다시 실행
              </button>
            </div>
          </>
        )}

        {/* COMPANY-LEVEL */}
        <div className={`${styles.slabel} ${styles.company}`}>
          <span className={styles.dot} />내 제품 · 회사 정보 · 제품을 바꿔도 회사 정보는 고정됩니다
        </div>

        {loadError && <div className={styles.notice}>{loadError}</div>}

        <div className={`${styles.card} ${styles.pm}`}>
          <div className={styles.pmHead}>
            <div className={styles.ctitle}>내 제품 관리</div>
            <button className={styles.add} onClick={openAdd}>+ 제품 추가</button>
          </div>
          <div className={styles.plist}>
            {pagedProducts.map((p) => (
              <div key={p.id} className={`${styles.prow} ${p.id === selectedId ? styles.cur : ''}`}>
                <div>
                  <div className={styles.pn}>{p.name}</div>
                  <div className={styles.pmeta}>HS {p.hs} · {p.country} 수출</div>
                </div>
                <span className={`${styles.badge} ${p.id === selectedId ? styles.badgeCur : ''}`}>
                  {p.id === selectedId ? '지금 보는 제품' : '제품'}
                </span>
                {p.score != null
                  ? <span className={styles.sc}>{p.score}<span className={styles.scU}>점</span></span>
                  : <span className={`${styles.sc} ${styles.scNone}`}>미진단</span>}
                <div className={styles.acts}>
                  <button className={styles.ab} onClick={() => openEdit(p.id)}>수정</button>
                  <button className={`${styles.ab} ${styles.abDanger}`} onClick={() => askDelete(p.id)}>삭제</button>
                  <button className={`${styles.ab} ${styles.abPrimary}`} onClick={() => navigate(`/mypage/diagnosis/${p.id}`)}>보기</button>
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
            2026 산업통상부 공공데이터 활용 공모전 · 본 결과는 공공데이터 기반 AI 분석이며 최종 판단은 전문가와 함께 하십시오.
          </div>
        </footer>
      </div>

      {/* 제품 추가 / 수정 모달 */}
      <div
        className={`${styles.modal} ${modalOpen ? styles.open : ''}`}
        onClick={(e) => { if (e.target === e.currentTarget) closeModal() }}
      >
        <div className={styles.sheet}>
          <h3>{editingId ? '제품 수정' : '제품 추가'}</h3>
          <div className={styles.desc}>
            제품명·HS Code·수출 대상국을 입력하면 마이페이지와 진단 입력에 반영됩니다.
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
          {saveError && <div className={styles.modalError}>{saveError}</div>}
          {editingId && (
            <button
              className={styles.knLink}
              onClick={() => { closeModal(); navigate('/mypage/knowledge') }}
            >
              📦 제품 지식(인증서·FTO·바이어 요구사항)은 여기서 관리 →
            </button>
          )}
          <div className={styles.foot}>
            <button className={styles.cancel} onClick={closeModal}>취소</button>
            <button className={styles.save} onClick={saveProduct}>{editingId ? '수정' : '추가'}</button>
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
            지금까지의 <b>AI 진단 이력과 감사 로그는 그대로 보존</b>돼요.
          </div>
          <div className={styles.foot}>
            <button className={styles.cancel} onClick={() => setDeleteTarget(null)} disabled={deleting}>취소</button>
            <button className={styles.saveDanger} onClick={confirmDelete} disabled={deleting}>
              {deleting ? '삭제 중…' : '삭제'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
