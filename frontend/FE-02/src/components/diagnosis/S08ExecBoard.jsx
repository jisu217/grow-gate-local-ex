// S08 실행 보드 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

export default function S08ExecBoard({ execData }) {
  return (
    <DiagnosisStage
      eyebrow="S08 GATE ACTION PLAN"
      title="Gate 통과 업무 보드"
      right={<span className="s-tag">이번 주 · 견적 전 · 출하 전</span>}
    >

      <div className="exec-cols">
        {execData.map((col) => (
          <div key={col.day} className="exec-col">

            <div className="exec-col-head">
              <div className="exec-day">{col.day}</div>
              <div className="exec-day-lbl">{col.label}</div>
            </div>

            {col.tasks.map((task) => (

              <div
                key={task.title}
                className={`exec-task ${task.urgent ? 'urgent' : ''}`}
              >
                <div className="etag">{task.tag}</div>
                <div className="etitle">{task.title}</div>
                <div className="ewho">{task.who}</div>
                {task.output && <div className="ewho">산출물 · {task.output}</div>}
                {task.dueDate && <div className="ewho">기한 · {task.dueDate}</div>}
              </div>
            ))}
          </div>
        ))}
      </div>
    </DiagnosisStage>
  )
}
