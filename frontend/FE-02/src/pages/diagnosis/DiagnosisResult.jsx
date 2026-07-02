import { useState, useRef, useEffect } from 'react'
import { useParams, useSearchParams } from 'react-router-dom'

import Sidebar from '../../components/layout/Sidebar.jsx'
import Topbar  from '../../components/layout/Topbar.jsx'
import KpiRow  from '../../components/layout/KpiRow.jsx'
import Pager   from '../../components/ui/Pager.jsx'
import StatusMessage from '../../components/ui/StatusMessage.jsx'
import GateDecisionHero from '../../components/diagnosis/GateDecisionHero.jsx'
import SourceTrustBar from '../../components/diagnosis/SourceTrustBar.jsx'

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

import { DIAGNOSIS_STEPS } from '../../mock/diagnosisSteps.js'
import { getDiagnosisReport, getDiagnosisResult } from '../../services/diagnosisService.js'

// 진단 결과 뷰어 페이지 컴포넌트
export default function DiagnosisResult() {
  const { id } = useParams()
  const [searchParams] = useSearchParams()

  // ?step=N 으로 진입하면 해당 단계부터 시작 (예: 마이페이지 "오늘 할 일" → 08 실행 보드)
  const stepParam = parseInt(searchParams.get('step'), 10)
  const initialStep = (stepParam >= 0 && stepParam < DIAGNOSIS_STEPS.length) ? stepParam : 0

  const [result, setResult]       = useState(null)
  const [pageState, setPageState] = useState('loading')
  const [currentStep, setCurrentStep] = useState(initialStep)
  const [isCollapsed, setIsCollapsed] = useState(false)
  const [reportError, setReportError] = useState('')

  const scrollRef = useRef(null)

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
  }, [id])

  // 단계 이동
  const go = (i) => {
    if (i < 0 || i >= DIAGNOSIS_STEPS.length) return
    setCurrentStep(i)
    scrollRef.current?.scrollTo({ top: 0, behavior: 'smooth' })
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

  // 스텝 → 컴포넌트 매핑
  const renderStage = (stages) => {
    const key = currentStep
    switch (currentStep) {
      case 0:  return <S00Overview   key={key} data={stages.s00} />
      case 1:  return <S01Input      key={key} data={stages.s01} />
      case 2:  return <S02Connectors key={key} data={stages.s02} />
      case 3:  return <S03PredLab    key={key} predictions={stages.s03.predictions} features={stages.s03.features} />
      case 4:  return <S04Market     key={key} markets={stages.s04} />
      case 5:  return <S05RegMatrix  key={key} regulations={stages.s05} />
      case 6:  return <S06Recall     key={key} recalls={stages.s06} />
      case 7:  return <S07FtoSupport key={key} patents={stages.s07.patents} supports={stages.s07.supports} />
      case 8:  return <S08ExecBoard  key={key} execData={stages.s08} />
      case 9:  return <S09Report     key={key} scoreBars={stages.s09.scoreBars} summary={stages.s09.summary} onDownload={downloadReport} fastApiUsed={result.session.reportFastApiUsed} />
      case 10: return <S10Sources    key={key} data={stages.s10} />
      default: return null
    }
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

  if (pageState === 'loading') return (
    <div className="app">
      <div className="content">
        <div className="scroll">
          <StatusMessage type="loading" message="공공데이터와 제품 지식을 대조 중입니다." />
        </div>
      </div>
    </div>
  )

  if (pageState === 'error') return (
    <div className="app">
      <div className="content">
        <div className="scroll">
          <StatusMessage type="error" message="진단 결과를 불러오지 못했습니다. 잠시 후 다시 시도해주세요." />
        </div>
      </div>
    </div>
  )

  if (pageState === 'failed') return (
    <div className="app">
      <div className="content">
        <div className="scroll">
          <StatusMessage type="error" message="진단 실행이 실패했습니다. 입력값과 백엔드 로그를 확인해주세요." />
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
          />
          <SourceTrustBar summary={result.session.sourceTrustSummary} />

          {['pending', 'running'].includes(String(result.session?.status ?? '').toLowerCase()) && (
            <StatusMessage type="loading" message="공공데이터와 제품 지식을 대조 중입니다. 완료되면 이 화면이 자동으로 갱신됩니다." />
          )}
          {reportError && <StatusMessage type="error" message={reportError} />}

          {renderStage(result.stages)}

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
