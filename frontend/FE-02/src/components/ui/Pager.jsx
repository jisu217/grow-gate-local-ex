// 단계 이동 페이저 컴포넌트
export default function Pager({ currentStep, totalSteps, steps, onPrev, onNext }) {
  const isFirst = currentStep === 0
  const isLast  = currentStep === totalSteps - 1

  return (

    <div className="pager">

      <button onClick={onPrev} disabled={isFirst}>
        ← 이전{!isFirst ? ` · ${steps[currentStep - 1].nm}` : ''}
      </button>

      <button className="next" onClick={onNext} disabled={isLast}>
        다음{!isLast ? ` · ${steps[currentStep + 1].nm}` : ''} →
      </button>
    </div>
  )
}
