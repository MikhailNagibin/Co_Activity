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
import ProfilePage from './pages/ProfilePage.jsx'
import QADataPage from './pages/QADataPage.jsx'
import SignIn from './pages/SignIn.jsx'
import SignUp from './pages/SignUp.jsx'

function LandingPage() {
  const { isAuthenticated, isLoading } = useAuthSession()
  if (isLoading) {
    return null
  }
  return <Navigate to={isAuthenticated ? '/main' : '/sign-in'} replace />
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
      <Route path="/main" element={<RequireAuth><MainPage /></RequireAuth>} />
      <Route path="/rooms/:roomId" element={<RequireAuth><RoomActivityPage /></RequireAuth>} />
      <Route path="/create-room" element={<RequireAuth><CreateRoomPage /></RequireAuth>} />
      <Route path="/profile" element={<RequireAuth><ProfilePage /></RequireAuth>} />
      <Route path="/profile/notifications" element={<RequireAuth><NotificationSettingsPage /></RequireAuth>} />
      <Route path="/qa" element={<RequireAuth><QADataPage /></RequireAuth>} />
      <Route path="/qa/new" element={<RequireAuth><CreateQuestionPage /></RequireAuth>} />
      <Route path="/cards/default-0" element={<RequireAuth><DefaultCard0 /></RequireAuth>} />
      <Route path="/questions/default-0" element={<Navigate to="/questions/demo" replace />} />
      <Route path="/questions/demo" element={<RequireAuth><DefaultQuestion0 /></RequireAuth>} />
      <Route path="/questions/:questionId" element={<RequireAuth><QuestionThreadPage /></RequireAuth>} />
      <Route path="*" element={<Navigate to="/sign-in" replace />} />
    </Routes>
  )
}

export default App
