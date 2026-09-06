import { useEffect, type ReactNode } from 'react'

type Props = {
  title: string
  onClose: () => void
  children: ReactNode
  sheet?: boolean
  hideTitle?: boolean
}

export function Dialog({ title, onClose, children, sheet = false, hideTitle = false }: Props) {
  useEffect(() => {
    function onKey(event: KeyboardEvent) {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = previous
    }
  }, [onClose])

  return (
    <div className={`dialog ${sheet ? 'dialog--sheet' : ''}`} role="presentation">
      <button className="dialog__backdrop" type="button" aria-label="Kapat" onClick={onClose} />
      <div
        className={`dialog__panel ${hideTitle ? 'dialog__panel--form' : ''}`}
        role="dialog"
        aria-modal="true"
        aria-label={title}
        aria-labelledby={hideTitle ? undefined : 'dialog-title'}
      >
        <header className={`dialog__head ${hideTitle ? 'dialog__head--bare' : ''}`}>
          {hideTitle ? <span className="dialog__sr">{title}</span> : <h2 id="dialog-title">{title}</h2>}
          <button className="dialog__close" type="button" onClick={onClose} aria-label="Kapat">
            ×
          </button>
        </header>
        <div className="dialog__body">{children}</div>
      </div>
    </div>
  )
}
