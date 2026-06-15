import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { LicenseProvider } from './context/LicenseContext.jsx'
import { NotificationProvider } from './context/NotificationContext.jsx'
import App from './App.jsx'
import './index.css'

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <LicenseProvider>
      <NotificationProvider>
        <App />
      </NotificationProvider>
    </LicenseProvider>
  </StrictMode>,
)
