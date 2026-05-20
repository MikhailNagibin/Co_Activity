import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import './styles/app.scss'
import App from './App.jsx'
import { AuthSessionProvider } from './auth/AuthSessionProvider.jsx'
import { ThemeProvider } from './theme/ThemeProvider.jsx'
import './styles/dark-theme.scss'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <AuthSessionProvider>
        <ThemeProvider>
          <App />
        </ThemeProvider>
      </AuthSessionProvider>
    </BrowserRouter>
  </StrictMode>,
)
