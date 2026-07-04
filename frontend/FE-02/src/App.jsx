// 앱 전체 라우터 진입점
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import AppLayout      from './components/layout/AppLayout.jsx'
import ProtectedRoute from './components/route/ProtectedRoute.jsx'

import Home          from './pages/auth/Home.jsx'
import Login         from './pages/auth/Login.jsx'
import Signup        from './pages/auth/Signup.jsx'
import SignupPersonal from './pages/auth/SignupPersonal.jsx'
import SignupCompany  from './pages/auth/SignupCompany.jsx'

import DiagnosisList    from './pages/diagnosis/DiagnosisList.jsx'
import DiagnosisNew     from './pages/diagnosis/DiagnosisNew.jsx'
import DiagnosisResult  from './pages/diagnosis/DiagnosisResult.jsx'
import DiagnosisSources from './pages/diagnosis/DiagnosisSources.jsx'
import DailyPublicData  from './pages/publicdata/DailyPublicData.jsx'

import Mypage          from './pages/mypage/Mypage.jsx'
import DiagnosisDetail from './pages/mypage/DiagnosisDetail.jsx'
import KnowledgeDetail from './pages/mypage/KnowledgeDetail.jsx'
import KnowledgeEdit   from './pages/mypage/KnowledgeEdit.jsx'

export default function App() {
  return (

    <BrowserRouter>
      <Routes>

        {/* 공개 페이지 */}
        <Route path="/"                  element={<Home />} />
        <Route path="/login"             element={<Login />} />
        <Route path="/signup"            element={<Signup />} />
        <Route path="/signup/personal"   element={<SignupPersonal />} />
        <Route path="/signup/company"    element={<SignupCompany />} />

        {/* 공개 proof 뷰어: 심사장/데모에서 로그인 없이 수집 증거 확인 */}
        <Route element={<AppLayout />}>
          <Route path="/public-data/daily-refresh" element={<DailyPublicData />} />
        </Route>

        {/* 로그인 필요 */}
        <Route element={<ProtectedRoute />}>

          {/* 진단 결과·근거 뷰어: 자체 사이드바 레이아웃 (공통 상단바 없음) */}
          <Route path="/diagnosis/:id"          element={<DiagnosisResult />} />
          <Route path="/diagnosis/:id/sources"  element={<DiagnosisSources />} />

          {/* 그 외 화면: 공통 상단 고정 내비게이션 레이아웃 */}
          <Route element={<AppLayout />}>

            <Route path="/diagnosis"              element={<DiagnosisList />} />
            <Route path="/diagnosis/new"          element={<DiagnosisNew />} />

            <Route path="/mypage"                        element={<Mypage />} />
            <Route path="/mypage/diagnosis/:productId"   element={<DiagnosisDetail />} />
            <Route path="/mypage/knowledge/:productId"   element={<KnowledgeDetail />} />

            {/* 제품 정보 상세/수정. 제품 목록·생성은 마이페이지('내 제품 관리' + 제품 추가 모달)로 일원화됨.
                수정 권한(knowledge:edit)은 페이지의 can()이 담당 */}
            <Route path="/mypage/knowledge/:productId/edit"   element={<KnowledgeEdit />} />

          </Route>
        </Route>

      </Routes>
    </BrowserRouter>
  )
}
