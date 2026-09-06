import { useState } from 'react'
import { useGuestState } from '../context/GuestState'
import { wedding } from '../config/wedding'
import { BackHome } from '../components/BackHome'
import { IconGallery } from '../components/Icons'
import { Lightbox } from '../components/Lightbox'

export function GalleryPage() {
  const { photos } = useGuestState()
  const [openIndex, setOpenIndex] = useState<number | null>(null)
  const visible = photos.filter((photo) => photo.fileUrl)

  return (
    <div className="subpage">
      <BackHome />
      <header className="subpage__head">
        <h1 className="title-with-icon">
          Düğün Anılarımız
          <IconGallery size={20} />
        </h1>
        <p>Bu güzel geceden paylaşılan kareleri burada görebilirsiniz.</p>
        {visible.length === 0 ? <p className="subpage__empty">{wedding.galleryEmpty}</p> : null}
      </header>
      {visible.length ? (
        <ul className="gallery-grid">
          {visible.map((photo, index) => (
            <li key={photo.id}>
              <button type="button" className="gallery-item" onClick={() => setOpenIndex(index)}>
                <img src={photo.fileUrl} alt="" loading="lazy" />
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {openIndex != null ? (
        <Lightbox
          photos={visible}
          index={openIndex}
          onClose={() => setOpenIndex(null)}
          onIndex={setOpenIndex}
        />
      ) : null}
    </div>
  )
}
