import { useEffect, useState } from 'react'
import type { Photo } from '../types'

type Props = {
  photos: Photo[]
  index: number
  onClose: () => void
  onIndex: (index: number) => void
}

export function Lightbox({ photos, index, onClose, onIndex }: Props) {
  const [startX, setStartX] = useState<number | null>(null)
  const photo = photos[index]

  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
      if (event.key === 'ArrowRight') onIndex((index + 1) % photos.length)
      if (event.key === 'ArrowLeft') onIndex((index - 1 + photos.length) % photos.length)
    }
    window.addEventListener('keydown', onKey)
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = previous
    }
  }, [index, photos.length, onClose, onIndex])

  if (!photo) return null

  function go(delta: number) {
    onIndex((index + delta + photos.length) % photos.length)
  }

  return (
    <div
      className="lightbox"
      role="dialog"
      aria-modal="true"
      aria-label="Fotoğraf"
      onClick={onClose}
      onTouchStart={(event) => setStartX(event.touches[0]?.clientX ?? null)}
      onTouchEnd={(event) => {
        if (startX == null) return
        const end = event.changedTouches[0]?.clientX ?? startX
        const dx = end - startX
        if (dx > 56) go(-1)
        if (dx < -56) go(1)
        setStartX(null)
      }}
    >
      <button className="lightbox__close" type="button" onClick={onClose} aria-label="Kapat">
        ×
      </button>
      {photos.length > 1 ? (
        <>
          <button
            className="lightbox__nav lightbox__nav--prev"
            type="button"
            aria-label="Önceki"
            onClick={(event) => {
              event.stopPropagation()
              go(-1)
            }}
          >
            ‹
          </button>
          <button
            className="lightbox__nav lightbox__nav--next"
            type="button"
            aria-label="Sonraki"
            onClick={(event) => {
              event.stopPropagation()
              go(1)
            }}
          >
            ›
          </button>
        </>
      ) : null}
      {photo.fileUrl ? (
        <img src={photo.fileUrl} alt={photo.fileName} onClick={(event) => event.stopPropagation()} />
      ) : (
        <p className="lightbox__empty">Bu görüntü önizlenemiyor</p>
      )}
    </div>
  )
}
