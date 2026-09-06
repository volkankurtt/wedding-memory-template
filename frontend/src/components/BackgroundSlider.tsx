import { useEffect, useState } from 'react'
import { BACKGROUND_INTERVAL_MS, backgroundImages } from '../config/wedding'

export function BackgroundSlider() {
  const [index, setIndex] = useState(0)
  const [ready, setReady] = useState(() => new Set(backgroundImages.length ? [0] : []))

  useEffect(() => {
    if (backgroundImages.length < 2) return
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return

    const id = window.setInterval(() => {
      setIndex((current) => (current + 1) % backgroundImages.length)
    }, BACKGROUND_INTERVAL_MS)

    return () => window.clearInterval(id)
  }, [])

  useEffect(() => {
    if (!backgroundImages.length) return
    const next = (index + 1) % backgroundImages.length
    const src = backgroundImages[next]?.src
    if (!src) return

    const image = new Image()
    image.src = src
    image.onload = () => {
      setReady((current) => {
        if (current.has(next)) return current
        const copy = new Set(current)
        copy.add(next)
        return copy
      })
    }
  }, [index])

  return (
    <div className="bg-slider" aria-hidden>
      {backgroundImages.map((image, i) =>
        ready.has(i) ? (
          <img
            key={image.file}
            className={i === index ? 'is-active' : ''}
            src={image.src}
            alt=""
            decoding={i === 0 ? 'sync' : 'async'}
            fetchPriority={i === 0 ? 'high' : 'low'}
            style={{
              ['--pos-m' as string]: image.mobilePosition,
              ['--pos-d' as string]: image.desktopPosition,
            }}
          />
        ) : null,
      )}
      <div className="bg-slider__veil" />
    </div>
  )
}
