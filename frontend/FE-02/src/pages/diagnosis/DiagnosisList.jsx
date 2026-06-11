import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './DiagnosisList.module.css'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import { getDiagnosisList } from '../../services/diagnosisService.js'

// 진단 상태 → 레이블 맵
const STATUS_MAP = {
  done:    { label: '완료',    cls: styles.done    },
  pending: { label: '분석 중', cls: styles.pending },
  failed:  { label: '실패',    cls: styles.failed  },
}

export default function DiagnosisList() {
  const navigate = useNavigate()
  const [list, setList] = useState([])
  const [pageState, setPageState] = useState('loading')

  useEffect(() => {
    getDiagnosisList()
      .then((data) => { setList(data); setPageState('ok') })
      .catch(() => setPageState('error'))
  }, [])

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <div>
          <h1 className={styles.title}>AI 진단 이력</h1>
          <p className={styles.sub}>요청한 진단 목록과 분석 결과를 확인하세요.</p>
        </div>
        <button className={styles.newBtn} onClick={() => navigate('/diagnosis/diag-001')}>
          + 새 진단 시작
        </button>
      </div>

      {pageState === 'loading' && (
        <StatusMessage type="loading" message="공공데이터와 제품 지식을 분석 중입니다." />
      )}
      {pageState === 'error' && (
        <StatusMessage type="error" message="현재 진단을 불러오지 못했습니다. 잠시 후 다시 시도해주세요." />
      )}
      {pageState === 'ok' && list.length === 0 && (
        <StatusMessage type="empty" message="아직 진단 이력이 없습니다. 새 진단을 시작해보세요." />
      )}

      {pageState === 'ok' && (
        <div className={styles.list}>
          {list.map((diag) => {
            const status = STATUS_MAP[diag.status]
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
                    {diag.diagnosis_score !== null && (
                      <div className={styles.score}>{diag.diagnosis_score}<span>/100</span></div>
                    )}
                    <span className={`${styles.badge} ${status.cls}`}>{status.label}</span>
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
                ) : diag.status === 'pending' ? (
                  <div className={styles.summaryPending}>AI 분석이 진행 중입니다…</div>
                ) : diag.status === 'failed' ? (
                  <div className={styles.summaryFailed}>분석에 실패했습니다. 다시 시도해 주세요.</div>
                ) : null}
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}
