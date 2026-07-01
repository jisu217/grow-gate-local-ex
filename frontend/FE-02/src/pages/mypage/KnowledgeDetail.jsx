import { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import styles from './KnowledgeDetail.module.css'
import { usePermission } from '../../hooks/usePermission.js'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import {
  acceptKnowledgeLoopItem,
  dismissKnowledgeLoopItem,
  getKnowledgeLoopItems,
  getProductAuditLogs,
  getProductKnowledge,
} from '../../services/productService.js'
import { toGradeLabel, toGradeClass } from '../../utils/trustGrade.js'

// 인증 출처 유형 맵
const CERT_SOURCE = {
  official: { label: '공식 문서', cls: 'cond' },
  internal: { label: '내부 메모', cls: 'memo' },
}

const ALL_TABS = ['기본 정보', '인증서', 'FTO', '바이어 요구사항', 'AI 지식화', '변경 이력']
const KNOWLEDGE_IDEAS = [
  '반복 진단에서 2회 이상 등장한 리스크',
  '담당자가 보유한 내부 정책·라벨링 기준',
  '공공 API fallback이 발생한 영역의 대체 출처',
  '인증 갱신·FTO 검토·바이어 요구사항 체크리스트',
]

// 제품 지식 상세 페이지 컴포넌트
export default function KnowledgeDetail() {
  const { productId } = useParams()
  const navigate = useNavigate()
  const { can } = usePermission()

  const [product, setProduct] = useState(null)
  const [loopItems, setLoopItems] = useState([])
  const [loopState, setLoopState] = useState('idle')
  const [pageState, setPageState] = useState('loading')
  const [activeTab, setActiveTab] = useState(0)

  // audit:view 권한 없으면 변경 이력 탭 제외
  const canViewAudit = can('audit:view')
  const tabs = canViewAudit ? ALL_TABS : ALL_TABS.filter((t) => t !== '변경 이력')

  useEffect(() => {
    setPageState('loading')
    getProductKnowledge(productId)
      .then((data) => {
        setProduct(data)
        setPageState('ok')
        // 변경 이력은 별도 엔드포인트라 본문 로딩과 분리해 채움 (권한 없으면 호출 생략)
        if (canViewAudit) {
          getProductAuditLogs(productId).then((auditLog) => {
            setProduct((prev) => (prev ? { ...prev, auditLog } : prev))
          })
        }
        getKnowledgeLoopItems(productId)
          .then((items) => setLoopItems(items))
          .catch(() => setLoopItems([]))
      })
      .catch(() => setPageState('error'))
  }, [productId, canViewAudit])

  const refreshLoopItems = async () => {
    const items = await getKnowledgeLoopItems(productId)
    setLoopItems(items)
  }

  const handleLoopAction = async (itemId, action) => {
    if (!can('knowledge:edit')) return
    setLoopState(itemId)
    try {
      if (action === 'accept') await acceptKnowledgeLoopItem(productId, itemId)
      else await dismissKnowledgeLoopItem(productId, itemId)
      await refreshLoopItems()
    } finally {
      setLoopState('idle')
    }
  }

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
        <span className={styles.grade}>신뢰도 {toGradeLabel(product.trustGrade)}</span>
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
              <span className={`pill ${toGradeClass(product.trustGrade)}`}>
                신뢰도 {toGradeLabel(product.trustGrade)}
              </span>
              {product.knowledgeStatus === 'expiring' && <span className="pill warn">만료 임박</span>}
              {product.knowledgeStatus === 'valid'    && <span className="pill ok">유효</span>}
              {product.knowledgeStatus === 'expired'  && <span className="pill req">만료</span>}
              {product.knowledgeStatus === 'needs-check' && <span className="pill warn">검증 필요</span>}
              {CERT_SOURCE[product.sourceBasis] && (
                <span className={`pill ${CERT_SOURCE[product.sourceBasis].cls}`}>
                  {CERT_SOURCE[product.sourceBasis].label}
                </span>
              )}
            </div>

            {product.missingFields?.length > 0 && (
              <div className={styles.missingList}>
                <div className={styles.missingTitle}>누락 항목 — 진단 신뢰도에 영향</div>
                {product.missingFields.map((f) => (
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
                    {CERT_SOURCE[cert.sourceType] && (
                      <span
                        className={`pill ${CERT_SOURCE[cert.sourceType].cls}`}
                        style={{ marginLeft: 8, fontSize: 10 }}
                      >
                        {CERT_SOURCE[cert.sourceType].label}
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
                <li key={req.id} className={styles.reqItem}>
                  <strong>{req.buyerName}</strong>
                  {req.country ? ` · ${req.country}` : ''} — {req.requirements}
                </li>
              ))}
            </ul>
          </div>
        )}

        {activeTab === 4 && (
          <div className={styles.card}>
            <div className={styles.sectionTitle}>AI 지식화 루프</div>
            <div className={styles.loopIntro}>
              진단 agent가 판단한 근거 중 다음 진단에 재사용할 만한 항목입니다. 담당자가 승인한 항목만 제품 지식 출처로 반영됩니다.
            </div>
            {loopItems.length === 0 ? (
              <div className={styles.ideaPanel}>
                <div className={styles.missingTitle}>지식화하면 좋은 항목</div>
                {KNOWLEDGE_IDEAS.map((idea) => (
                  <div key={idea} className={styles.ideaItem}>{idea}</div>
                ))}
              </div>
            ) : (
              <div className={styles.loopList}>
                {loopItems.map((item) => (
                  <div key={item.id} className={styles.loopItem}>
                    <div className={styles.loopHead}>
                      <div>
                        <span className="pill memo">{item.type}</span>
                        <div className={styles.loopTitle}>{item.title}</div>
                      </div>
                      <span className={`pill ${item.status === 'accepted' ? 'ok' : item.status === 'dismissed' ? 'cond' : 'warn'}`}>
                        {item.status}
                      </span>
                    </div>
                    {item.evidence && <div className={styles.loopText}>근거: {item.evidence}</div>}
                    {item.rationale && <div className={styles.loopText}>판단: {item.rationale}</div>}
                    {item.impact && <div className={styles.loopText}>효과: {item.impact}</div>}
                    {item.status === 'pending' && can('knowledge:edit') && (
                      <div className={styles.loopActions}>
                        <button
                          className={styles.acceptBtn}
                          disabled={loopState === item.id}
                          onClick={() => handleLoopAction(item.id, 'accept')}
                        >
                          지식화
                        </button>
                        <button
                          className={styles.dismissBtn}
                          disabled={loopState === item.id}
                          onClick={() => handleLoopAction(item.id, 'dismiss')}
                        >
                          제외
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {activeTab === 5 && (
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
