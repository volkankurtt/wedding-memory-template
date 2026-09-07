import { useEffect, useRef, useState, type MouseEvent } from 'react'
import type { Photo } from '../types'
import { downloadPhoto, photoDisplayUrl, photoDownloadName, photoOriginalUrl } from '../utils/downloadPhoto'
import { IconDownload } from './Icons'

type Props = {
  photos: Photo[]
  index: number
  onClose: () => void
  onIndex: (index: number) => void
}

const DOWNLOAD_ERROR = 'Fotoğraf indirilemedi. Lütfen tekrar deneyin.'

export function Lightbox({ photos, index, onClose, onIndex }: Props) {
  const [startX, setStartX] = useState<number | null>(null)
  const [downloading, setDownloading] = useState(false)
  const [downloadError, setDownloadError] = useState<string | null>(null)
  const downloadGen = useRef(0)
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

  useEffect(() => {
    downloadGen.current += 1
    setDownloading(false)
    setDownloadError(null)
  }, [photo?.id])

  if (!photo) return null

  function go(delta: number) {
    onIndex((index + delta + photos.length) % photos.length)
  }

  async function onDownload(event: MouseEvent<HTMLButtonElement>) {
    event.stopPropagation()
    const originalUrl = photoOriginalUrl(photo)
    if (!originalUrl || downloading) return

    const gen = downloadGen.current + 1
    downloadGen.current = gen
    setDownloading(true)
    setDownloadError(null)

    try {
      await downloadPhoto(originalUrl, photoDownloadName(photo.fileName, photo.id))
    } catch {
      if (downloadGen.current === gen) {
        setDownloadError(DOWNLOAD_ERROR)
      }
    } finally {
      if (downloadGen.current === gen) {
        setDownloading(false)
      }
    }
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
      {photoDisplayUrl(photo) ? (
        <img src={photoDisplayUrl(photo)} alt={photo.fileName} onClick={(event) => event.stopPropagation()} />
      ) : (
        <p className="lightbox__empty">Bu görüntü önizlenemiyor</p>
      )}
      {photoOriginalUrl(photo) || photoDisplayUrl(photo) ? (
        <div className="lightbox__download-wrap" onClick={(event) => event.stopPropagation()}>
          <button
            className="lightbox__download"
            type="button"
            onClick={onDownload}
            disabled={downloading}
            aria-label="İndir"
          >
            <IconDownload size={16} />
            {downloading ? 'İndiriliyor...' : 'İndir'}
          </button>
          {downloadError ? <p className="lightbox__download-error">{downloadError}</p> : null}
        </div>
      ) : null}
    </div>
  )
}
