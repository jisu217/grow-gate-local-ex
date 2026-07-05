import DonutChart from '../ui/DonutChart.jsx'
import Pill from '../ui/Pill.jsx'
import styles from './KpiRow.module.css'

// KPI 지표 행 컴포넌트
export default function KpiRow({ kpiData }) {
  return (

    <div className={styles.kpiRow} data-count={kpiData.length}>
      {kpiData.map((kpi) => (
        <div key={kpi.step} className={styles.kpi} aria-label={`${kpi.step} ${kpi.eyebrow ?? kpi.label ?? kpi.title ?? ''}`}>

          <div className={styles.step}>{kpi.step}</div>
          <div className="eyebrow">{kpi.eyebrow ?? kpi.label ?? kpi.title ?? '진단 지표'}</div>

          {kpi.score !== undefined ? (
            <>
              <div className={styles.scoreHead}>
                <div className={`big ${styles.scoreNum}`}>
                  {kpi.score}<span>/100</span>
                </div>

                <DonutChart score={kpi.score} />
              </div>
              {kpi.sub && <div className="sub">{kpi.sub}</div>}
            </>
          ) : (

            <>
              <div className={`big ${styles.bigValue}`} style={{ fontSize: kpi.valueFontSize }} title={String(kpi.value ?? '')}>
                {kpi.value}
                <span className={styles.unit}>{kpi.unit}</span>
              </div>
              {kpi.sub && <div className="sub">{kpi.sub}</div>}
            </>
          )}

          {kpi.sources && (
            <div className={styles.srcRow}>
              {kpi.sources.map((src) => (

                <span key={src} className={styles.src}>{src}</span>
              ))}
            </div>
          )}

          {kpi.pills && (
            <div className={styles.rulePills}>
              {kpi.pills.map((pill) => (
                <Pill key={pill.label} type={pill.type}>{pill.label}</Pill>
              ))}
            </div>
          )}

          {kpi.roadSegments && (
            <div className={styles.road}>
              {kpi.roadSegments.map((seg, i) => (
                <div key={seg.days} className={styles.seg}>
                  <div className={styles.d}>{seg.days}</div>
                  <div className={styles.l}>{seg.label}</div>

                  <div
                    className={styles.bar}
                    style={{
                      background: i === 0
                        ? 'var(--blue)'
                        : i === 1
                        ? '#7aa0ff'
                        : 'var(--blue-line)',
                    }}
                  />
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  )
}
