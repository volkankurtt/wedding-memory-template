import { useEffect, useRef, useState } from 'react'
import { useGuestState } from '../context/GuestState'
import { wedding } from '../config/wedding'
import { BackHome } from '../components/BackHome'
import { IconGallery } from '../components/Icons'
import { Lightbox } from '../components/Lightbox'
import { photoDisplayUrl } from '../utils/downloadPhoto'

export function GalleryPage() {
  const {
    photos,
    photosLoading,
    photosLoadingMore,
    photosHasNext,
    photosHydrated,
    photosError,
    photosPageError,
    ensurePhotosLoaded,
    loadMorePhotos,
    retryPhotosPage,
  } = useGuestState()
  const [openIndex, setOpenIndex] = useState<number | null>(null)
  const sentinelRef = useRef<HTMLDivElement | null>(null)
  const visible = photos.filter((photo) => photoDisplayUrl(photo))

  useEffect(() => {
    ensurePhotosLoaded()
  }, [ensurePhotosLoaded])

  useEffect(() => {
    const node = sentinelRef.current
    if (
      !node ||
      !photosHydrated ||
      !photosHasNext ||
      photosPageError ||
      photosError ||
      photosLoading ||
      photosLoadingMore
    ) {
      return
    }

    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) loadMorePhotos()
      },
      { rootMargin: '480px 0px' },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [loadMorePhotos, photosHasNext, photosPageError, photosError, photosLoading, photosLoadingMore, photosHydrated, visible.length])

  return (
    <div className="subpage">
      <BackHome />
      <header className="subpage__head">
        <h1 className="title-with-icon">
          {wedding.galleryTitle}
          <IconGallery size={20} />
        </h1>
        <p>{wedding.galleryLead}</p>
        {photosError ? <p className="form-error">{photosError}</p> : null}
        {(photosLoading || !photosHydrated) && visible.length === 0 && !photosError ? (
          <p className="subpage__empty">Fotoğraflar yükleniyor…</p>
        ) : visible.length === 0 && !photosError ? (
          <p className="subpage__empty">{wedding.galleryEmpty}</p>
        ) : null}
      </header>
      {visible.length ? (
        <ul className="gallery-grid">
          {visible.map((photo, index) => (
            <li key={photo.id}>
              <button type="button" className="gallery-item" onClick={() => setOpenIndex(index)}>
                <img src={photoDisplayUrl(photo)} alt="" loading="lazy" />
              </button>
            </li>
          ))}
        </ul>
      ) : null}
      {photosLoadingMore ? <p className="subpage__empty">Daha fazla fotoğraf yükleniyor…</p> : null}
      {photosPageError ? (
        <p className="form-error">
          Yeni sayfa yüklenemedi. Lütfen tekrar deneyin.{' '}
          <button className="text-btn" type="button" onClick={retryPhotosPage}>
            Tekrar dene
          </button>
        </p>
      ) : null}
      {photosHasNext && !photosPageError && !photosError && photosHydrated ? (
        <div ref={sentinelRef} aria-hidden="true" />
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
