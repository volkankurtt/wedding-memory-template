import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { useEffect } from 'react'
import { warmupBackendInBackground } from './api/backendReady'
import { BackgroundSlider } from './components/BackgroundSlider'
import { GuestStateProvider } from './context/GuestState'
import { GalleryPage } from './pages/GalleryPage'
import { HomePage } from './pages/HomePage'
import { NotlarPage } from './pages/NotlarPage'
import { UploadPage } from './pages/UploadPage'

export default function App() {
  useEffect(() => {
    warmupBackendInBackground()
  }, [])

  return (
    <GuestStateProvider>
      <BackgroundSlider />
      <div className="page-shell">
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/fotograflar" element={<GalleryPage />} />
            <Route path="/gallery" element={<Navigate to="/fotograflar" replace />} />
            <Route path="/notlar" element={<NotlarPage />} />
            <Route path="/upload" element={<UploadPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </div>
    </GuestStateProvider>
  )
}
