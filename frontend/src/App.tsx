import { useEffect, useState } from 'react'
import { Routes, Route, Navigate } from 'react-router-dom'
import { silentRefresh } from './api/client'
import LoginPage from './pages/LoginPage'
import SignupPage from './pages/SignupPage'
import DashboardPage from './pages/DashboardPage'
import CalendarPage from './pages/CalendarPage'
import PlantPage from './pages/PlantPage'
import FamilyInvitePage from './pages/FamilyInvitePage'
import MessagePage from './pages/MessagePage'
import ChatPage from './pages/ChatPage'
import MyPage from './pages/MyPage'
import FindIdPage from './pages/FindIdPage'
import FindPasswordPage from './pages/FindPasswordPage'
import ResetPasswordPage from './pages/ResetPasswordPage'
import TermsPage from './pages/TermsPage'
import PrivacyPage from './pages/PrivacyPage'

function App() {
  const [booting, setBooting] = useState(true)

  // 앱 로드 시 refresh 쿠키로 access 재발급 시도 (새로고침해도 로그인 유지)
  useEffect(() => {
    silentRefresh().finally(() => setBooting(false))
  }, [])

  if (booting) return null

  return (
    <Routes>
      <Route path="/" element={<Navigate to="/login" />} />
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route path="/find-id" element={<FindIdPage />} />
      <Route path="/find-password" element={<FindPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/terms" element={<TermsPage />} />
      <Route path="/privacy" element={<PrivacyPage />} />
      <Route path="/family-invite" element={<FamilyInvitePage />} />
      <Route path="/dashboard" element={<DashboardPage />} />
      <Route path="/calendar" element={<CalendarPage />} />
      <Route path="/plant" element={<PlantPage />} />
      <Route path="/message" element={<MessagePage />} />
      <Route path="/chat" element={<ChatPage />} />
      <Route path="/mypage" element={<MyPage />} />
    </Routes>
  )
}

export default App
