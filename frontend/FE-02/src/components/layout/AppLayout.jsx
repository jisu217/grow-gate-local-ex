import { Outlet } from 'react-router-dom'
import TopNav from './TopNav.jsx'

// 앱 공통 레이아웃 — 상단 고정 내비게이션 + 페이지 본문
// (진단 결과/근거 뷰어는 자체 사이드바 레이아웃이라 이 레이아웃을 쓰지 않음)
export default function AppLayout() {
  return (
    <>
      <TopNav />
      <Outlet />
    </>
  )
}
