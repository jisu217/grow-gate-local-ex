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

import Mypage          from './pages/mypage/Mypage.jsx'
import Knowledge       from './pages/mypage/Knowledge.jsx'
import KnowledgeNew    from './pages/mypage/KnowledgeNew.jsx'
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

        {/* 로그인 필요 */}
        <Route element={<ProtectedRoute />}>
          <Route element={<AppLayout />}>

            <Route path="/diagnosis"              element={<DiagnosisList />} />
            <Route path="/diagnosis/new"          element={<DiagnosisNew />} />
            <Route path="/diagnosis/:id"          element={<DiagnosisResult />} />
            <Route path="/diagnosis/:id/sources"  element={<DiagnosisSources />} />

            <Route path="/mypage"                        element={<Mypage />} />
            <Route path="/mypage/knowledge"              element={<Knowledge />} />
            <Route path="/mypage/knowledge/:productId"   element={<KnowledgeDetail />} />

            {/* 회사 계정 필요 */}
            <Route element={<ProtectedRoute requireCompany />}>
              <Route path="/mypage/knowledge/new"               element={<KnowledgeNew />} />
              <Route path="/mypage/knowledge/:productId/edit"   element={<KnowledgeEdit />} />
            </Route>

          </Route>
        </Route>

      </Routes>
    </BrowserRouter>
  )
}
