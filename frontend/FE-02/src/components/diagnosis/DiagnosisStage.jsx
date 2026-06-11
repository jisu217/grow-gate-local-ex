export default function DiagnosisStage({ eyebrow, title, description, right, children }) {
  return (

    <div className="stage">

      <div className="s-eyebrow">{eyebrow}</div>
      <div className="s-head">
        <div>
          <h2>{title}</h2>

          {description && <p>{description}</p>}
        </div>

        {right && <div>{right}</div>}
      </div>

      {children}
    </div>
  )
}
