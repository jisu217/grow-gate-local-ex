import { useParams, useNavigate } from 'react-router-dom'
import styles from './DiagnosisSources.module.css'

const SOURCES = [
  { category: '공공데이터 근거', items: [
    { title: '관세청 수출입무역통계 — HS 850760 대독일 수출 현황', date: '2025-05' },
    { title: 'KOTRA 독일 시장 보고서 — 배터리 규제 동향', date: '2025-04' },
    { title: 'SafetyKorea 리콜 데이터 — 리튬이온 배터리 사례 56건', date: '2025-05' },
  ]},
  { category: '제품 지식 출처', items: [
    { title: 'UN38.3 시험 계획서 초안 (내부 문서)', date: '2025-05' },
    { title: 'CE/EMC 기술문서 체크리스트', date: '2025-04' },
  ]},
]

// 진단 근거 출처 페이지 컴포넌트
export default function DiagnosisSources() {

  const { id } = useParams()
  const navigate = useNavigate()

  return (
    <div className={styles.wrap}>
      <button className={styles.back} onClick={() => navigate(`/diagnosis/${id}`)}>
        ← 진단 결과로 돌아가기
      </button>
      <h1 className={styles.title}>진단 근거 및 출처</h1>
      <p className={styles.sub}>진단 ID: {id}</p>

      {SOURCES.map((section) => (
        <div key={section.category} className={styles.section}>
          <div className={styles.sectionTitle}>{section.category}</div>
          {section.items.map((item) => (
            <div key={item.title} className={styles.item}>
              <span className={styles.itemTitle}>{item.title}</span>
              <span className={styles.itemDate}>{item.date}</span>
            </div>
          ))}
        </div>
      ))}
    </div>
  )
}
