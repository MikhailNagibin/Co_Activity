import { Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import { useAuthSession } from './auth/authSessionContext.js'
import RequireAuth from './auth/RequireAuth.jsx'
import RoomActivityPage from './pages/RoomActivityPage.jsx'
import DefaultCard0 from './pages/DefaultCard0.jsx'
import DefaultQuestion0 from './pages/DefaultQuestion0.jsx'
import QuestionThreadPage from './pages/QuestionThreadPage.jsx'
import CreateQuestionPage from './pages/CreateQuestionPage.jsx'
import CreateRoomPage from './pages/CreateRoomPage.jsx'
import MainPage from './pages/MainPage.jsx'
import NotificationSettingsPage from './pages/NotificationSettingsPage.jsx'
import PasswordResetPage from './pages/PasswordResetPage.jsx'
import ProfilePage from './pages/ProfilePage.jsx'
import PublicUserProfilePage from './pages/PublicUserProfilePage.jsx'
import QADataPage from './pages/QADataPage.jsx'
import SignIn from './pages/SignIn.jsx'
import SignUp from './pages/SignUp.jsx'

function LandingPage() {
  const { isLoading } = useAuthSession()
  if (isLoading) {
    return null
  }
  return <Navigate to="/main" replace />
}

function PublicOnlyRoute({ children }) {
  const { isAuthenticated, isLoading } = useAuthSession()
  if (isLoading) {
    return null
  }
  return isAuthenticated ? <Navigate to="/main" replace /> : children
}

function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<Navigate to="/sign-in" replace />} />
      <Route path="/register" element={<Navigate to="/sign-up" replace />} />
      <Route path="/sign-in" element={<PublicOnlyRoute><SignIn /></PublicOnlyRoute>} />
      <Route path="/sign-up" element={<PublicOnlyRoute><SignUp /></PublicOnlyRoute>} />
      <Route path="/password-reset" element={<PublicOnlyRoute><PasswordResetPage /></PublicOnlyRoute>} />
      <Route path="/main" element={<MainPage />} />
      <Route path="/rooms/:roomId" element={<RoomActivityPage />} />
      <Route path="/create-room" element={<CreateRoomPage />} />
      <Route path="/profile" element={<RequireAuth><ProfilePage /></RequireAuth>} />
      <Route path="/profile/notifications" element={<RequireAuth><NotificationSettingsPage /></RequireAuth>} />
      <Route path="/users/:userId" element={<RequireAuth><PublicUserProfilePage /></RequireAuth>} />
      <Route path="/qa" element={<QADataPage />} />
      <Route path="/qa/new" element={<CreateQuestionPage />} />
      <Route path="/cards/default-0" element={<DefaultCard0 />} />
      <Route path="/questions/default-0" element={<Navigate to="/questions/demo" replace />} />
      <Route path="/questions/demo" element={<DefaultQuestion0 />} />
      <Route path="/questions/:questionId" element={<QuestionThreadPage />} />
      <Route path="*" element={<Navigate to="/main" replace />} />
    </Routes>
  )
}

export default App
