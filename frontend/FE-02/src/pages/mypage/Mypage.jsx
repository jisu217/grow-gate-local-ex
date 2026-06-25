import { useState, useEffect } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import styles from './Mypage.module.css'
import {
  getMypageOverview,
  getMypageProducts,
  createMypageProduct,
  updateMypageProduct,
  rerunDiagnosis,
} from '../../services/mypageService.js'
import { getAuthUser, logout } from '../../services/authService.js'

const COUNTRIES = ['독일', '미국', '일본', 'ASEAN', '중국', 'EU']

// 마이페이지 — 수출 준비 상태 대시보드
export default function Mypage() {
  const navigate = useNavigate()
  const user = getAuthUser() // 로그인 정보 (없으면 null)

  // 로그아웃 → 홈으로
  const handleLogout = async () => {
    await logout()
    navigate('/')
  }
  const [overview, setOverview] = useState({ company: null, members: [] })
  const [products, setProducts] = useState([])
  const [selectedId, setSelectedId] = useState('')

  // 모달 상태
  const [modalOpen, setModalOpen] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState({ name: '', hs: '', country: '독일' })

  // 진단 재실행 로딩
  const [rerunLoading, setRerunLoading] = useState(false)

  useEffect(() => {
    getMypageOverview().then(setOverview)
    getMypageProducts().then((data) => {
      setProducts(data)
      if (data.length) setSelectedId(data[0].id)
    })
  }, [])

  const current = products.find((p) => p.id === selectedId)

  // 제품 선택
  function selectProduct(id) {
    setSelectedId(id)
    document.querySelector(`.${styles.slabel}`)?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }

  // 모달 열기/닫기
  function openAdd() {
    setEditingId(null)
    setForm({ name: '', hs: '', country: '독일' })
    setModalOpen(true)
  }
  function openEdit(id) {
    const p = products.find((x) => x.id === id)
    setEditingId(id)
    setForm({ name: p.name, hs: p.hs, country: p.country })
    setModalOpen(true)
  }
  function closeModal() { setModalOpen(false) }

  // 제품 저장 (추가/수정)
  async function saveProduct() {
    const name = form.name.trim()
    const hs = form.hs.trim()
    if (!name || !hs) { alert('제품명과 HS Code를 입력해주세요.'); return }

    if (editingId) {
      await updateMypageProduct(editingId, { name, hs, country: form.country })
      setProducts((prev) => prev.map((p) =>
        p.id === editingId ? { ...p, name, hs, country: form.country } : p))
    } else {
      const created = await createMypageProduct({ name, hs, country: form.country })
      setProducts((prev) => [...prev, created])
      setSelectedId(created.id)
    }
    closeModal()
  }

  // AI 진단 다시 실행
  async function rerun() {
    if (!current) return
    setRerunLoading(true)
    await rerunDiagnosis(current.id, current.country)
    setProducts((prev) => prev.map((p) => {
      if (p.id !== current.id) return p
      if (p.score == null) {
        return { ...p, score: 60, chips: ['규제 위험 3건', '인증 필요 2건', 'FTO 위험 보통'] }
      }
      return { ...p, score: Math.min(100, p.score + 5) }
    }))
    setRerunLoading(false)
  }

  return (
    <div className={styles.root}>
      <div className={styles.bgfx}>
        <div className={`${styles.aurora} ${styles.a1}`} />
        <div className={`${styles.aurora} ${styles.a2}`} />
        <div className={styles.grid} />
      </div>

      <div className={styles.page}>
        {/* NAV */}
        <nav className={styles.nav}>
          <Link to="/" className={styles.brand}>GROW-GATE <span className={styles.ex}>Local EX</span></Link>
          <div className={styles.navLinks}>
            <Link to="/">홈</Link>
            <Link to="/diagnosis">AI 진단</Link>
            <Link to="/mypage" className={styles.active}>마이페이지</Link>
          </div>
          <div className={styles.navCta}>
            {user
              ? <button className={styles.login} onClick={handleLogout}>로그아웃</button>
              : <Link className={styles.login} to="/login">로그인</Link>}
            <Link className={styles.signup} to="/diagnosis/new">진단 시작</Link>
          </div>
        </nav>

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
                className={`${styles.card} ${styles.score} ${current.runId ? styles.clickable : ''}`}
                onClick={() => current.runId && navigate(`/diagnosis/${current.runId}`)}
                role={current.runId ? 'button' : undefined}
              >
                <div className={styles.ctitle}>최근 AI 진단 결과 {current.runId && <span className={styles.goHint}>· 클릭하면 진단 결과로 →</span>}</div>
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
                <span className={styles.ar}>↻</span>&nbsp;이 제품 AI 진단 다시 실행
              </button>
            </div>
          </>
        )}

        {/* COMPANY-LEVEL */}
        <div className={`${styles.slabel} ${styles.company}`}>
          <span className={styles.dot} />내 제품 · 회사 정보 · 제품을 바꿔도 회사 정보는 고정됩니다
        </div>

        <div className={`${styles.card} ${styles.pm}`}>
          <div className={styles.pmHead}>
            <div className={styles.ctitle}>내 제품 관리</div>
            <button className={styles.add} onClick={openAdd}>+ 제품 추가</button>
          </div>
          <div className={styles.plist}>
            {products.map((p) => (
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
                  <button className={`${styles.ab} ${styles.abPrimary}`} onClick={() => selectProduct(p.id)}>보기</button>
                </div>
              </div>
            ))}
          </div>
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
          <h3>{editingId ? '제품 정보 수정' : '제품 추가'}</h3>
          <div className={styles.desc}>
            {editingId ? '수정 후 진단을 다시 실행하면 새 정보로 분석됩니다.' : '제품명·HS Code·수출 대상국을 입력하면 목록에 추가됩니다.'}
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
              {COUNTRIES.map((c) => <option key={c}>{c}</option>)}
            </select>
          </div>
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
            <button className={styles.save} onClick={saveProduct}>{editingId ? '저장' : '추가'}</button>
          </div>
        </div>
      </div>
    </div>
  )
}
