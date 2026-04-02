import { Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import DefaultCard0 from './pages/DefaultCard0.jsx'
import DefaultQuestion0 from './pages/DefaultQuestion0.jsx'
import CreateQuestionPage from './pages/CreateQuestionPage.jsx'
import CreateRoomPage from './pages/CreateRoomPage.jsx'
import MainPage from './pages/MainPage.jsx'
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
      <Route path="/create-room" element={<CreateRoomPage />} />
      <Route path="/profile" element={<ProfilePage />} />
      <Route path="/qa" element={<QADataPage />} />
      <Route path="/qa/new" element={<CreateQuestionPage />} />
      <Route path="/cards/default-0" element={<DefaultCard0 />} />
      <Route path="/questions/default-0" element={<DefaultQuestion0 />} />
      <Route path="*" element={<Navigate to="/sign-in" replace />} />
    </Routes>
  )
}

export default App
