import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import styles from './Knowledge.module.css'
import { usePermission } from '../../hooks/usePermission.js'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import { getProducts } from '../../services/productService.js'
import { toGradeLabel } from '../../utils/trustGrade.js'

// 지식 품질 상태 맵
const QUALITY_STATUS = {
  valid:         { label: '유효',      cls: 'ok'   },
  expiring:      { label: '만료 임박', cls: 'warn' },
  expired:       { label: '만료',      cls: 'req'  },
  'needs-check': { label: '검증 필요', cls: 'warn' },
  missing:       { label: '미입력',    cls: 'req'  },
}

// 출처 유형 맵
const SOURCE_BASIS = {
  official: { label: '공식 문서', cls: 'cond' },
  internal: { label: '내부 메모', cls: 'memo' },
}

// 제품 지식 목록 페이지 컴포넌트
export default function Knowledge() {
  const navigate = useNavigate()
  const { can } = usePermission()
  const [products, setProducts] = useState([])
  const [pageState, setPageState] = useState('loading')

  useEffect(() => {
    getProducts()
      .then((data) => { setProducts(data); setPageState('ok') })
      .catch(() => setPageState('error'))
  }, [])

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <h1 className={styles.title}>제품 지식 관리</h1>
        {can('knowledge:create') && (
          <button className={styles.newBtn} onClick={() => navigate('/mypage/knowledge/new')}>
            + 신규 제품 등록
          </button>
        )}
      </div>

      {pageState === 'loading' && (
        <StatusMessage type="loading" message="제품 지식을 불러오는 중입니다." />
      )}
      {pageState === 'error' && (
        <StatusMessage type="error" message="제품 목록을 불러오지 못했습니다. 잠시 후 다시 시도해주세요." />
      )}
      {pageState === 'ok' && products.length === 0 && (
        <StatusMessage type="empty" message="등록된 제품이 없습니다. 제품 지식을 먼저 등록해주세요." />
      )}

      {pageState === 'ok' && (
        <div className={styles.grid}>
          {products.map((p) => (
            <div key={p.id} className={styles.card} onClick={() => navigate(`/mypage/knowledge/${p.id}`)}>
              <div className={styles.productName}>{p.name}</div>
              <div className={styles.meta}>{p.category}</div>

              <div className={styles.infoRow}>
                <span className={styles.grade}>신뢰도 {toGradeLabel(p.trustGrade)}</span>
                {QUALITY_STATUS[p.knowledgeStatus] && (
                  <span className={`pill ${QUALITY_STATUS[p.knowledgeStatus].cls}`}>
                    {QUALITY_STATUS[p.knowledgeStatus].label}
                  </span>
                )}
                {SOURCE_BASIS[p.sourceBasis] && (
                  <span className={`pill ${SOURCE_BASIS[p.sourceBasis].cls}`}>
                    {SOURCE_BASIS[p.sourceBasis].label}
                  </span>
                )}
              </div>

              <div className={styles.registration}>
                <div className={styles.registrationHead}>
                  <span>등록률</span>
                  <strong>{p.registrationRate}%</strong>
                </div>
                <div className={styles.registrationBar}>
                  <div style={{ width: `${p.registrationRate}%` }} />
                </div>
                <div className={styles.registrationNote}>{p.registrationLabel}</div>
              </div>

              {p.missingFields?.length > 0 && (
                <div className={styles.missingHint}>
                  누락 {p.missingFields.length}건 · 진단 신뢰도에 영향
                </div>
              )}

              <div className={styles.updated}>최근 수정 {p.updatedAt}</div>
              <button
                className={styles.diagBtn}
                onClick={(e) => { e.stopPropagation(); navigate(`/diagnosis/new?productId=${p.id}`) }}
              >
                이 제품으로 판단하기 →
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}
