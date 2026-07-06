import { useState, useRef, useEffect, useCallback } from 'react'
import { useParams, useSearchParams, useNavigate } from 'react-router-dom'

import Sidebar from '../../components/layout/Sidebar.jsx'
import Topbar  from '../../components/layout/Topbar.jsx'
import KpiRow  from '../../components/layout/KpiRow.jsx'
import Pager   from '../../components/ui/Pager.jsx'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import GateDecisionHero from '../../components/diagnosis/GateDecisionHero.jsx'
import DecisionBrief from '../../components/diagnosis/DecisionBrief.jsx'
import SourceTrustBar from '../../components/diagnosis/SourceTrustBar.jsx'
import AgentExplanationPanel from '../../components/diagnosis/AgentExplanationPanel.jsx'

import S00Overview   from '../../components/diagnosis/S00Overview.jsx'
import S01Input      from '../../components/diagnosis/S01Input.jsx'
import S02Connectors from '../../components/diagnosis/S02Connectors.jsx'
import S03PredLab    from '../../components/diagnosis/S03PredLab.jsx'
import S04Market     from '../../components/diagnosis/S04Market.jsx'
import S05RegMatrix  from '../../components/diagnosis/S05RegMatrix.jsx'
import S06Recall     from '../../components/diagnosis/S06Recall.jsx'
import S07FtoSupport from '../../components/diagnosis/S07FtoSupport.jsx'
import S08ExecBoard  from '../../components/diagnosis/S08ExecBoard.jsx'
import S09Report     from '../../components/diagnosis/S09Report.jsx'
import S10Sources    from '../../components/diagnosis/S10Sources.jsx'

import { DIAGNOSIS_STEPS } from '../../constants/diagnosisSteps.js'
import { getDiagnosisReport, getDiagnosisResult } from '../../services/diagnosisService.js'
import { collectPublicData } from '../../services/publicDataService.js'

// 진단 결과 뷰어 페이지 컴포넌트
export default function DiagnosisResult() {
  const { id } = useParams()
  const navigate = useNavigate()
  const [searchParams] = useSearchParams()

  // ?step=N 으로 진입하면 해당 단계부터 시작 (예: 마이페이지 "오늘 할 일" → 08 실행 보드)
  const stepParam = parseInt(searchParams.get('step'), 10)
  const initialStep = (stepParam >= 0 && stepParam < DIAGNOSIS_STEPS.length) ? stepParam : 0

  const [result, setResult]       = useState(null)
  const [pageState, setPageState] = useState('loading')
  const [currentStep, setCurrentStep] = useState(initialStep)
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [reportError, setReportError] = useState('')
  const [retryStatus, setRetryStatus] = useState('')
  const [reloadKey, setReloadKey] = useState(0)

  const scrollRef = useRef(null)

  const stepIndexFromHash = useCallback(() => {
    const sectionId = window.location.hash.replace('#', '')
    if (!sectionId) return -1
    return DIAGNOSIS_STEPS.findIndex((step) => step.sectionId === sectionId)
  }, [])

  const scrollToStep = useCallback((i, options = {}) => {
    const { updateHash = true, behavior = 'smooth' } = options
    if (i < 0 || i >= DIAGNOSIS_STEPS.length) return

    setCurrentStep(i)

    const sectionId = DIAGNOSIS_STEPS[i].sectionId
    const target = sectionId ? document.getElementById(sectionId) : null

    if (target) {
      target.scrollIntoView({ behavior, block: 'start' })
      if (updateHash) {
        const nextUrl = `${window.location.pathname}${window.location.search}#${sectionId}`
        window.history.replaceState(null, '', nextUrl)
      }
      return
    }

    scrollRef.current?.scrollTo({ top: 0, behavior })
  }, [])

  useEffect(() => {
    let timeoutId = null
    let cancelled = false

    const load = async () => {
      try {
        const data = await getDiagnosisResult(id)
        if (cancelled) return
        setResult(data)
        setPageState(data.session?.status === 'failed' ? 'failed' : 'ok')
        if (['pending', 'running'].includes(String(data.session?.status ?? '').toLowerCase())) {
          timeoutId = window.setTimeout(load, 2500)
        }
      } catch {
        if (!cancelled) setPageState('error')
      }
    }

    setPageState('loading')
    load()
    return () => {
      cancelled = true
      if (timeoutId) window.clearTimeout(timeoutId)
    }
  }, [id, reloadKey])

  // 단계 이동
  const go = (i) => {
    scrollToStep(i)
  }

  // 키보드 방향키 내비게이션
  useEffect(() => {
    const handleKey = (e) => {
      if (e.key === 'ArrowLeft')  go(currentStep - 1)
      if (e.key === 'ArrowRight') go(currentStep + 1)
    }
    window.addEventListener('keydown', handleKey)
    return () => window.removeEventListener('keydown', handleKey)
  }, [currentStep])

  useEffect(() => {
    if (pageState !== 'ok') return
    const hashStep = stepIndexFromHash()
    const targetStep = hashStep >= 0 ? hashStep : initialStep
    window.requestAnimationFrame(() => {
      scrollToStep(targetStep, {
        updateHash: hashStep >= 0,
        behavior: 'auto',
      })
    })
  }, [id, pageState, reloadKey, initialStep, scrollToStep, stepIndexFromHash])

  useEffect(() => {
    if (pageState !== 'ok') return undefined

    const sectionNodes = DIAGNOSIS_STEPS
      .map((step, index) => {
        const node = document.getElementById(step.sectionId)
        return node ? { node, index } : null
      })
      .filter(Boolean)

    if (sectionNodes.length === 0) return undefined

    const observer = new IntersectionObserver((entries) => {
      const visible = entries
        .filter((entry) => entry.isIntersecting)
        .sort((a, b) => a.boundingClientRect.top - b.boundingClientRect.top)

      const firstVisible = visible[0]
      if (!firstVisible) return

      const index = Number(firstVisible.target.dataset.stepIndex)
      if (Number.isInteger(index)) setCurrentStep(index)
    }, {
      root: null,
      rootMargin: '-96px 0px -58% 0px',
      threshold: [0.12, 0.35, 0.6],
    })

    sectionNodes.forEach(({ node }) => observer.observe(node))
    return () => observer.disconnect()
  }, [pageState, result])

  // 스텝 → 컴포넌트 매핑
  const buildStages = (stages) => {
    const s03 = stages.s03 ?? {}
    const s07 = stages.s07 ?? {}
    const s09 = stages.s09 ?? {}
    return [
      <S00Overview data={stages.s00 ?? {}} />,
      <S01Input data={stages.s01 ?? {}} />,
      <S02Connectors data={stages.s02 ?? []} />,
      <S03PredLab predictions={s03.predictions ?? []} features={s03.features ?? []} />,
      <S04Market markets={stages.s04 ?? []} />,
      <S05RegMatrix regulations={stages.s05 ?? []} />,
      <S06Recall recalls={stages.s06 ?? []} />,
      <S07FtoSupport patents={s07.patents ?? []} supports={s07.supports ?? []} />,
      <S08ExecBoard execData={stages.s08 ?? []} />,
      <S09Report scoreBars={s09.scoreBars ?? []} summary={s09.summary} onDownload={downloadReport} fastApiUsed={result.session.reportFastApiUsed} />,
      <S10Sources data={stages.s10 ?? {}} sourceTrustSummary={result.session.sourceTrustSummary} onRetry={retryPublicData} retryStatus={retryStatus} />,
    ]
  }

  const renderStages = (stages) => {
    const stageComponents = buildStages(stages)
    return stageComponents.map((stage, index) => {
      const step = DIAGNOSIS_STEPS[index]
      return (
        <section
          key={step.sectionId}
          id={step.sectionId}
          className="diagnosis-section"
          data-step-index={index}
          aria-label={`${step.no} ${step.nm}`}
        >
          {stage}
        </section>
      )
    })
  }

  const downloadReport = async () => {
    setReportError('')
    try {
      const report = await getDiagnosisReport(id)
      const blob = new Blob([report.markdown ?? ''], { type: report.contentType ?? 'text/markdown;charset=utf-8' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = report.fileName ?? `grow-gate-diagnosis-${id}.md`
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      setReportError(err.message || '리포트 다운로드에 실패했습니다. 잠시 후 다시 시도해주세요.')
    }
  }

  const retryPublicData = async () => {
    const payload = result?.session?.sourceTrustSummary?.retryPayload ?? {}
    if (!payload.hsCode || !payload.targetCountry || !payload.productName) {
      setRetryStatus('재수집 입력값 부족')
      return
    }
    setRetryStatus('running')
    try {
      const response = await collectPublicData(payload)
      const runId = response?.run?.id || response?.runId || 'created'
      setRetryStatus(`재수집 run 생성: ${runId}`)
    } catch (err) {
      setRetryStatus(err.message || '재수집 실패')
    }
  }

  if (pageState === 'loading') return (
    <div className="app">
      <div className="content">
        <div className="scroll">
          <StatusMessage type="loading" message="공공데이터와 제품 정보를 대조 중입니다." />
        </div>
      </div>
    </div>
  )

  if (pageState === 'error') return (
    <div className="app">
      <div className="content">
        <div className="scroll">
          <StatusMessage
            type="error"
            message="진단 결과를 불러오지 못했습니다. 네트워크를 확인하고 다시 시도해주세요."
            onAction={() => setReloadKey((k) => k + 1)}
            onSecondary={() => navigate('/diagnosis')}
            secondaryLabel="판단 이력으로"
          />
        </div>
      </div>
    </div>
  )

  if (pageState === 'failed') return (
    <div className="app">
      <div className="content">
        <div className="scroll">
          <StatusMessage
            type="error"
            message="진단 실행이 실패했습니다. 입력값을 확인하고 새 판단을 실행해주세요."
            actionLabel="새 판단 실행"
            onAction={() => navigate('/diagnosis/new')}
            onSecondary={() => navigate('/diagnosis')}
            secondaryLabel="판단 이력으로"
          />
        </div>
      </div>
    </div>
  )

  return (
    <div className={`app ${isCollapsed ? 'collapsed' : ''}`}>

      <Sidebar
        steps={DIAGNOSIS_STEPS}
        currentStep={currentStep}
        session={result.session}
        isCollapsed={isCollapsed}
        onToggle={() => setIsCollapsed((prev) => !prev)}
        onStepClick={go}
        onBoardClick={() => go(8)}
      />

      <div className="content">
        <Topbar currentStepName={DIAGNOSIS_STEPS[currentStep].nm} />

        <div className="scroll" ref={scrollRef}>
          <KpiRow kpiData={result.kpiData} />
          <GateDecisionHero
            gateDecision={result.session.gateDecision}
            impactEstimate={result.session.impactEstimate}
            confidencePanel={result.session.confidencePanel}
            sourceTrustSummary={result.session.sourceTrustSummary}
          />
          <DecisionBrief session={result.session} stages={result.stages} />
          <SourceTrustBar summary={result.session.sourceTrustSummary} onRetry={retryPublicData} retryStatus={retryStatus} />
          <AgentExplanationPanel
            session={result.session}
            onSourcesClick={() => go(10)}
            onBoardClick={() => go(8)}
          />

          {['pending', 'running'].includes(String(result.session?.status ?? '').toLowerCase()) && (
            <StatusMessage type="loading" message="공공데이터와 제품 정보를 대조 중입니다. 완료되면 이 화면이 자동으로 갱신됩니다." />
          )}
          {reportError && <StatusMessage type="error" message={reportError} />}

          {renderStages(result.stages)}

          <Pager
            currentStep={currentStep}
            totalSteps={DIAGNOSIS_STEPS.length}
            steps={DIAGNOSIS_STEPS}
            onPrev={() => go(currentStep - 1)}
            onNext={() => go(currentStep + 1)}
          />
        </div>
      </div>
    </div>
  )
}
