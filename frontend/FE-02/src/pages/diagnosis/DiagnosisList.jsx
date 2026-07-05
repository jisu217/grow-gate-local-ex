import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './DiagnosisList.module.css'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import { deleteDiagnosis, getDiagnosisList } from '../../services/diagnosisService.js'

// Gate 판단 상태 → 레이블 맵
const STATUS_MAP = {
  done:    { label: '완료',    cls: styles.done    },
  pending: { label: '분석 중', cls: styles.pending },
  running: { label: '분석 중', cls: styles.pending },
  failed:  { label: '실패',    cls: styles.failed  },
}

// 한 페이지에 보여줄 진단 이력 수
const ITEMS_PER_PAGE = 5

// Gate 판단 이력 목록 페이지 컴포넌트
export default function DiagnosisList() {
  const navigate = useNavigate()
  const [list, setList] = useState([])
  const [pageState, setPageState] = useState('loading')
  const [page, setPage] = useState(0)
  const [reloadKey, setReloadKey] = useState(0)
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [deleteStatus, setDeleteStatus] = useState({ type: 'idle', message: '' })

  useEffect(() => {
    setPageState('loading')
    getDiagnosisList()
      .then((data) => { setList(data); setPageState('ok') })
      .catch(() => setPageState('error'))
  }, [reloadKey])

  const openDeleteConfirm = (diag, event) => {
    event.stopPropagation()
    setDeleteStatus({ type: 'idle', message: '' })
    setDeleteTarget(diag)
  }

  const closeDeleteConfirm = () => {
    if (deleteStatus.type === 'deleting') return
    setDeleteTarget(null)
  }

  const confirmDelete = async () => {
    if (!deleteTarget) return
    setDeleteStatus({ type: 'deleting', message: '진단 이력을 삭제하고 있습니다.' })
    try {
      await deleteDiagnosis(deleteTarget.id)
      setList((items) => items.filter((item) => item.id !== deleteTarget.id))
      setDeleteTarget(null)
      setDeleteStatus({ type: 'success', message: '진단 이력을 삭제했습니다.' })
      setPage((current) => {
        const nextLength = Math.max(0, list.length - 1)
        const nextPageCount = Math.max(1, Math.ceil(nextLength / ITEMS_PER_PAGE))
        return Math.min(current, nextPageCount - 1)
      })
    } catch (err) {
      setDeleteStatus({
        type: 'error',
        message: err?.message || '진단 이력을 삭제하지 못했습니다. 잠시 후 다시 시도해주세요.',
      })
    }
  }

  // 페이지네이션 (범위 자동 보정)
  const pageCount = Math.max(1, Math.ceil(list.length / ITEMS_PER_PAGE))
  const safePage = Math.min(page, pageCount - 1)
  const pagedList = list.slice(safePage * ITEMS_PER_PAGE, safePage * ITEMS_PER_PAGE + ITEMS_PER_PAGE)

  return (
    <>
      <div className={styles.wrap}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>수출 Gate 판단 이력</h1>
          <p className={styles.sub}>요청한 제품·바이어 문의별 Gate 판단과 근거 기반 점검 결과를 확인하세요.</p>
        </div>
        {/* 빈 화면에는 아래 전용 CTA가 있으므로 목록이 있을 때만 헤더 버튼 노출 */}
        {!(pageState === 'ok' && list.length === 0) && (
          <button className={styles.newBtn} onClick={() => navigate('/diagnosis/new')}>
            + 새 Gate 판단
          </button>
        )}
      </div>

      {pageState === 'loading' && (
        <StatusMessage type="loading" message="공공데이터와 제품 정보를 대조 중입니다." />
      )}
      {pageState === 'error' && (
        <StatusMessage
          type="error"
          message="현재 진단을 불러오지 못했습니다. 네트워크를 확인하고 다시 시도해주세요."
          onAction={() => setReloadKey((k) => k + 1)}
        />
      )}
      {deleteStatus.type === 'success' && (
        <div className={styles.noticeSuccess} role="status">{deleteStatus.message}</div>
      )}
      {deleteStatus.type === 'error' && (
        <div className={styles.noticeError} role="alert">{deleteStatus.message}</div>
      )}
      {pageState === 'ok' && list.length === 0 && (
        <div className={styles.empty}>
          <div className={styles.emptyIcon}>
            <svg fill="none" strokeWidth="2" viewBox="0 0 24 24" strokeLinecap="round" strokeLinejoin="round">
              <path d="M9 5H7a2 2 0 0 0-2 2v12a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2V7a2 2 0 0 0-2-2h-2M9 5a2 2 0 0 0 2 2h2a2 2 0 0 0 2-2M9 5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2m-6 9 2 2 4-4" />
            </svg>
          </div>
          <h3 className={styles.emptyTitle}>아직 Gate 판단한 제품이 없어요</h3>
          <p className={styles.emptyDesc}>
            제품과 바이어 문의를 등록하면 첫 Gate 판단이 여기서 시작됩니다. 아래 3단계면 충분해요.
          </p>
          <div className={styles.steps}>
            <span className={styles.step}><b>1</b> 제품 등록</span>
            <span className={styles.step}><b>2</b> 바이어 문의 입력</span>
            <span className={styles.step}><b>3</b> Gate 판단</span>
          </div>
          <button className={styles.emptyCta} onClick={() => navigate('/diagnosis/new')}>
            제품 등록하고 Gate 판단 →
          </button>
        </div>
      )}

      {pageState === 'ok' && list.length > 0 && (
        <div className={styles.list}>
          {pagedList.map((diag) => {
            const status = STATUS_MAP[diag.status] ?? { label: diag.status ?? '알 수 없음', cls: styles.pending }
            const overallScore = typeof diag.diagnosisScore === 'number'
              ? diag.diagnosisScore
              : diag.diagnosisScore?.overall
            return (
              <div
                key={diag.id}
                className={styles.card}
                onClick={() => navigate(`/diagnosis/${diag.id}`)}
              >
                <div className={styles.cardTop}>
                  <div>
                    <div className={styles.productName}>{diag.productName}</div>
                    <div className={styles.meta}>
                      HS {diag.hsCode} · {diag.targetMarket} · {diag.requestedAt}
                    </div>
                  </div>
                  <div className={styles.cardRight}>
                    {overallScore != null && (
                      <div className={styles.score}>{overallScore}<span>/100</span></div>
                    )}
                    <span className={`${styles.badge} ${status.cls}`}>{status.label}</span>
                    <button
                      type="button"
                      className={styles.deleteBtn}
                      aria-label={`${diag.productName || '진단'} 이력 삭제`}
                      title="진단 이력 삭제"
                      onClick={(event) => openDeleteConfirm(diag, event)}
                    >
                      삭제
                    </button>
                  </div>
                </div>

                {diag.summary ? (
                  <div className={styles.summaryRow}>
                    {diag.summary.map((item) => (
                      <div key={item.label} className={styles.sumItem}>
                        <div className={styles.sumLabel}>{item.label}</div>
                        <div className={styles.sumVal}>
                          {item.value}<span className={styles.sumUnit}>{item.unit}</span>
                        </div>
                        <div className={styles.sumDesc}>{item.desc}</div>
                      </div>
                    ))}
                  </div>
                ) : ['pending', 'running'].includes(diag.status) ? (
                  <div className={styles.summaryPending}>공공데이터와 제품 정보를 대조 중입니다…</div>
                ) : diag.status === 'failed' ? (
                  <div className={styles.summaryFailed}>Gate 판단에 실패했습니다. 다시 시도해 주세요.</div>
                ) : null}
              </div>
            )
          })}

          {/* 진단 이력이 한 페이지를 넘으면 페이지네이션 노출 */}
          {pageCount > 1 && (
            <div className={styles.pager}>
              <button
                className={styles.pagerBtn}
                onClick={() => setPage((n) => Math.max(0, n - 1))}
                disabled={safePage === 0}
              >
                ← 이전
              </button>
              <span className={styles.pagerInfo}>{safePage + 1} / {pageCount}</span>
              <button
                className={styles.pagerBtn}
                onClick={() => setPage((n) => Math.min(pageCount - 1, n + 1))}
                disabled={safePage >= pageCount - 1}
              >
                다음 →
              </button>
            </div>
          )}
        </div>
      )}
      </div>
      {deleteTarget && (
        <div className={styles.modalBackdrop} onClick={closeDeleteConfirm}>
          <div
            className={styles.modal}
            role="dialog"
            aria-modal="true"
            aria-labelledby="delete-diagnosis-title"
            onClick={(event) => event.stopPropagation()}
          >
            <h2 id="delete-diagnosis-title" className={styles.modalTitle}>진단 이력 삭제</h2>
            <p className={styles.modalDesc}>
              {deleteTarget.productName || '선택한 진단'} 이력을 목록에서 삭제합니다.
              삭제 후에는 해당 결과 화면과 근거 데이터를 다시 열 수 없습니다.
            </p>
            <div className={styles.modalMeta}>
              HS {deleteTarget.hsCode || '미등록'} · {deleteTarget.targetMarket || '대상국 미정'} · {deleteTarget.requestedAt || '일시 미등록'}
            </div>
            <div className={styles.modalActions}>
              <button
                type="button"
                className={styles.cancelBtn}
                onClick={closeDeleteConfirm}
                disabled={deleteStatus.type === 'deleting'}
              >
                취소
              </button>
              <button
                type="button"
                className={styles.confirmDeleteBtn}
                onClick={confirmDelete}
                disabled={deleteStatus.type === 'deleting'}
              >
                {deleteStatus.type === 'deleting' ? '삭제 중' : '삭제'}
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  )
}
