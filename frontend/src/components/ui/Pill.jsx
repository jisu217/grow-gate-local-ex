export default function Pill({ type, children, style }) {
  return (

    <span className={`pill ${type}`} style={style}>
      {children}
    </span>
  )
}
