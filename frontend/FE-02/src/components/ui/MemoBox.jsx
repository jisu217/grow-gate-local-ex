// 메모 박스 컴포넌트
export default function MemoBox({ children, style }) {
  return (

    <div className="memo" style={style}>
      {children}
    </div>
  )
}
