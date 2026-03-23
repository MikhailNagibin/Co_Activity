import { Navigate, Route, Routes } from 'react-router-dom'
import './App.css'
import DefaultCard0 from './pages/DefaultCard0.jsx'
import DefaultQuestion0 from './pages/DefaultQuestion0.jsx'
import MainPage from './pages/MainPage.jsx'
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
      <Route path="/qa" element={<QADataPage />} />
      <Route path="/cards/default-0" element={<DefaultCard0 />} />
      <Route path="/questions/default-0" element={<DefaultQuestion0 />} />
      <Route path="*" element={<Navigate to="/sign-in" replace />} />
    </Routes>
  )
}

export default App
