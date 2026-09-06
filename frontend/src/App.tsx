import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { BackgroundSlider } from './components/BackgroundSlider'
import { GuestStateProvider } from './context/GuestState'
import { GalleryPage } from './pages/GalleryPage'
import { HomePage } from './pages/HomePage'
import { UploadPage } from './pages/UploadPage'

export default function App() {
  return (
    <GuestStateProvider>
      <BackgroundSlider />
      <div className="page-shell">
        <BrowserRouter>
          <Routes>
            <Route path="/" element={<HomePage />} />
            <Route path="/gallery" element={<GalleryPage />} />
            <Route path="/upload" element={<UploadPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </BrowserRouter>
      </div>
    </GuestStateProvider>
  )
}
