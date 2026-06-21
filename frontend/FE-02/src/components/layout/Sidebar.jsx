import { useEffect, useRef } from 'react'
import styles from './Sidebar.module.css'

// 진단 단계 사이드바 컴포넌트
export default function Sidebar({
  steps,
  currentStep,
  session,
  isCollapsed,
  onToggle,
  onStepClick,
  onBoardClick,
}) {
  const activeRef = useRef(null)

  useEffect(() => {
    activeRef.current?.scrollIntoView({ block: 'nearest', behavior: 'smooth' })
  }, [currentStep])

  return (

    <aside className={`${styles.sidebar} ${isCollapsed ? styles.isCollapsed : ''}`}>

      <div className={styles.sbTop}>
        <div className={styles.sbBrand}>
          <span className={styles.sbBrandFull}>
            GROW-GATE <span className={styles.ex}>Local EX</span>
          </span>
          <span className={styles.sbBrandMini}>G</span>
        </div>

        <button
          className={styles.sbToggle}
          onClick={onToggle}
          title="사이드바 접기/펼치기"
        >
          {isCollapsed ? '›' : '‹'}
        </button>
      </div>

      <div className={`${styles.sbCur} ${isCollapsed ? styles.collapsed : ''}`}>
        {!isCollapsed && (
          <>
            <div className={styles.k}>현재 진단</div>
            <div className={styles.co}>{session.companyName}</div>
            <div className={styles.pr}>{session.productName} · {session.targetMarket}</div>
          </>
        )}

        {isCollapsed && <span style={{ color: 'var(--blue)', fontSize: 12 }}>●</span>}
      </div>

      {!isCollapsed && (
        <div className={styles.sbSection}>진단 단계</div>
      )}

      <nav>
        {steps.map((step, i) => {
          const isActive = i === currentStep
          const isDone   = i < currentStep

          return (

            <div
              key={step.no}
              ref={isActive ? activeRef : null}
              className={[
                styles.navItem,
                isActive ? styles.active : '',
                isDone   ? styles.done   : '',
              ].join(' ')}
              data-nm={step.nm}
              onClick={() => onStepClick(i)}
              role="button"
              tabIndex={0}
              aria-current={isActive ? 'step' : undefined}
              onKeyDown={(e) => e.key === 'Enter' && onStepClick(i)}
            >

              <span className={`${styles.no} ${isActive ? styles.noActive : ''} ${isDone ? styles.noDone : ''}`}>
                {step.no}
              </span>

              {!isCollapsed && (
                <span className={`${styles.nm} ${isActive ? styles.nmActive : ''}`}>
                  {step.nm}
                </span>
              )}

              {isDone && !isCollapsed && (
                <span className={styles.ck}>✓</span>
              )}
            </div>
          )
        })}
      </nav>

      <div style={{ flex: 1 }} />

      <button
        className={styles.sbBoard}
        onClick={onBoardClick}
        title="업무 보드로 이동"
      >

        {isCollapsed ? '▶' : '업무 보드로 이동'}
      </button>
    </aside>
  )
}
