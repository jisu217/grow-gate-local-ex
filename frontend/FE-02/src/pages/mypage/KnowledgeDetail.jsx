import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import styles from './KnowledgeDetail.module.css'
import { usePermission } from '../../hooks/usePermission.js'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import { getProductKnowledge } from '../../services/productService.js'

// 인증 출처 유형 맵
const CERT_SOURCE = {
  official: { label: '공식 문서', cls: 'cond' },
  internal: { label: '내부 메모', cls: 'memo' },
}

const ALL_TABS = ['기본 정보', '인증서', 'FTO', '바이어 요구사항', '변경 이력']

export default function KnowledgeDetail() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const { can } = usePermission()

  const [product, setProduct] = useState(null)
  const [pageState, setPageState] = useState('loading')
  const [activeTab, setActiveTab] = useState(0)

  useEffect(() => {
    setPageState('loading')
    getProductKnowledge(productId)
      .then((data) => { setProduct(data); setPageState('ok') })
      .catch(() => setPageState('error'))
  }, [productId])

  // audit:view 권한 없으면 변경 이력 탭 제외
  const tabs = can('audit:view') ? ALL_TABS : ALL_TABS.filter((t) => t !== '변경 이력')

  if (pageState === 'loading') return (
    <div className={styles.wrap}>
      <StatusMessage type="loading" message="제품 지식을 불러오는 중입니다." />
    </div>
  )
  if (pageState === 'error') return (
    <div className={styles.wrap}>
      <button className={styles.back} onClick={() => navigate('/mypage/knowledge')}>← 제품 지식 관리</button>
      <StatusMessage type="error" message="제품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요." />
    </div>
  )

  return (
    <div className={styles.wrap}>

      <div className={styles.topRow}>
        <button className={styles.back} onClick={() => navigate('/mypage/knowledge')}>
          ← 제품 지식 관리
        </button>
        {can('knowledge:edit') && (
          <button
            className={styles.editBtn}
            onClick={() => navigate(`/mypage/knowledge/${productId}/edit`)}
          >
            수정
          </button>
        )}
      </div>

      <div className={styles.titleRow}>
        <h1 className={styles.title}>{product.name}</h1>
        <span className={styles.grade}>신뢰도 {product.grade}</span>
      </div>
      <div className={styles.meta}>{product.category} · 최근 수정 {product.updatedAt}</div>

      <div className={styles.tabs}>
        {tabs.map((tab, i) => (
          <button
            key={tab}
            className={`${styles.tab} ${activeTab === i ? styles.tabActive : ''}`}
            onClick={() => setActiveTab(i)}
          >
            {tab}
          </button>
        ))}
      </div>

      <div className={styles.tabContent}>

        {activeTab === 0 && (
          <div className={styles.card}>
            <div className={styles.sectionTitle}>제품 지식 품질</div>
            <div className={styles.qualityPanel}>
              <span className={`pill ${product.grade === 'A' ? 'ok' : product.grade === 'B+' ? 'cond' : 'warn'}`}>
                신뢰도 {product.grade}
              </span>
              {product.knowledge_status === 'expiring' && <span className="pill warn">만료 임박</span>}
              {product.knowledge_status === 'valid'    && <span className="pill ok">유효</span>}
              {product.knowledge_status === 'expired'  && <span className="pill req">만료</span>}
              {product.knowledge_status === 'needs-check' && <span className="pill warn">검증 필요</span>}
              {CERT_SOURCE[product.source_basis] && (
                <span className={`pill ${CERT_SOURCE[product.source_basis].cls}`}>
                  {CERT_SOURCE[product.source_basis].label}
                </span>
              )}
            </div>

            {product.missing_fields?.length > 0 && (
              <div className={styles.missingList}>
                <div className={styles.missingTitle}>누락 항목 — AI 진단 정확도에 영향</div>
                {product.missing_fields.map((f) => (
                  <div key={f} className={styles.missingItem}>{f}</div>
                ))}
              </div>
            )}

            <div className={styles.sectionTitle} style={{ marginTop: 20 }}>제품 설명</div>
            <p className={styles.desc}>{product.description}</p>
          </div>
        )}

        {activeTab === 1 && (
          <div className={styles.card}>
            <div className={styles.sectionTitle}>인증서 현황</div>
            {product.certs.map((cert) => (
              <div key={cert.name} className={styles.certRow}>
                <div>
                  <div className={styles.certName}>
                    {cert.name}
                    {CERT_SOURCE[cert.source_type] && (
                      <span
                        className={`pill ${CERT_SOURCE[cert.source_type].cls}`}
                        style={{ marginLeft: 8, fontSize: 10 }}
                      >
                        {CERT_SOURCE[cert.source_type].label}
                      </span>
                    )}
                  </div>
                  <div className={styles.certMeta}>갱신 주기: {cert.renewalCycle}</div>
                </div>
                <div className={styles.certRight}>
                  <div className={styles.certExpiry}>만료 {cert.expiryDate}</div>
                  <span className={`${styles.certStatus} ${styles[cert.status]}`}>
                    {cert.status}
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}

        {activeTab === 2 && (
          <div className={styles.card}>
            <div className={styles.sectionTitle}>FTO (Freedom to Operate)</div>
            {product.fto.map((item, i) => (
              <div key={i} className={styles.ftoRow}>
                <div className={styles.certName}>대상 시장: {item.target}</div>
                <div className={styles.certMeta}>{item.note}</div>
                <span className={`${styles.certStatus} ${styles['검토 중']}`}>{item.status}</span>
              </div>
            ))}
          </div>
        )}

        {activeTab === 3 && (
          <div className={styles.card}>
            <div className={styles.sectionTitle}>바이어 요구사항</div>
            <ul className={styles.reqList}>
              {product.buyerRequirements.map((req) => (
                <li key={req} className={styles.reqItem}>{req}</li>
              ))}
            </ul>
          </div>
        )}

        {activeTab === 4 && (
          <div className={styles.card}>
            <div className={styles.sectionTitle}>변경 이력 (Audit Log)</div>
            {product.auditLog.map((log, i) => (
              <div key={i} className={styles.logEntry}>

                <div className={styles.logHeader}>
                  <span className={styles.logField}>{log.field}</span>
                  <span className={styles.logMeta}>{log.by} · {log.date}</span>
                </div>

                <div className={styles.logDiff}>
                  {log.oldValue ? (
                    <>
                      <span className={styles.logOld}>{log.oldValue}</span>
                      <span className={styles.logArrow}>→</span>
                      <span className={styles.logNew}>{log.newValue}</span>
                    </>
                  ) : (
                    <span className={styles.logNew}>{log.newValue}</span>
                  )}
                </div>

                <div className={styles.logReason}>
                  <span className={styles.logReasonLabel}>변경 사유 </span>{log.reason}
                </div>

              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
