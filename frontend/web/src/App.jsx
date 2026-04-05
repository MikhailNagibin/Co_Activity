import { Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
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

function App() {
  return (
    <Routes>
      <Route path="/" element={<Navigate to="/sign-in" replace />} />
      <Route path="/login" element={<Navigate to="/sign-in" replace />} />
      <Route path="/register" element={<Navigate to="/sign-up" replace />} />
      <Route path="/sign-in" element={<SignIn />} />
      <Route path="/sign-up" element={<SignUp />} />
      <Route path="/main" element={<MainPage />} />
      <Route path="/rooms/:roomId" element={<RoomActivityPage />} />
      <Route path="/create-room" element={<CreateRoomPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/profile/notifications" element={<NotificationSettingsPage />} />
      <Route path="/qa" element={<QADataPage />} />
      <Route path="/qa/new" element={<CreateQuestionPage />} />
      <Route path="/cards/default-0" element={<DefaultCard0 />} />
      <Route path="/questions/default-0" element={<Navigate to="/questions/demo" replace />} />
      <Route path="/questions/demo" element={<DefaultQuestion0 />} />
      <Route path="/questions/:questionId" element={<QuestionThreadPage />} />
      <Route path="*" element={<Navigate to="/sign-in" replace />} />
    </Routes>
  )
}

export default App
