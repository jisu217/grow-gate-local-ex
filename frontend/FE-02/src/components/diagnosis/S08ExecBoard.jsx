// S08 실행 보드 스테이지
import DiagnosisStage from './DiagnosisStage.jsx'

export default function S08ExecBoard({ execData }) {
  return (
    <DiagnosisStage
      eyebrow="S08 EXECUTION ROADMAP"
      title="30·60·90일 실행 보드"
      right={<span className="s-tag">우선 액션 12건 · 문서 이관 3건</span>}
    >

      <div className="exec-cols">
        {execData.map((col) => (
          <div key={col.day} className="exec-col">

            <div className="exec-col-head">
              <div className="exec-day">{col.day}</div>
              <div className="exec-day-lbl">Days · {col.label}</div>
            </div>

            {col.tasks.map((task) => (

              <div
                key={task.title}
                className={`exec-task ${task.urgent ? 'urgent' : ''}`}
              >
                <div className="etag">{task.tag}</div>
                <div className="etitle">{task.title}</div>
                <div className="ewho">{task.who}</div>
              </div>
            ))}
          </div>
        ))}
      </div>
    </DiagnosisStage>
  )
}
