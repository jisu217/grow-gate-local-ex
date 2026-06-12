export default function MemoBox({ children, style }) {
  return (

    <div className="memo" style={style}>
      {children}
    </div>
  )
}
