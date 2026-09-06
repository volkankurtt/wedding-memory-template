import { useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { ACCEPT_ATTR, UPLOAD_LIMITS } from '../config/limits'
import { useGuestState } from '../context/GuestState'
import type { Photo } from '../types'
import { IconCamera } from './Icons'

type Preview = {
  file: File
  url: string | null
  heic: boolean
}

function isHeic(file: File) {
  const name = file.name.toLowerCase()
  return (
    name.endsWith('.heic') ||
    name.endsWith('.heif') ||
    file.type === 'image/heic' ||
    file.type === 'image/heif'
  )
}

function isAllowed(file: File) {
  const name = file.name.toLowerCase()
  const extOk = UPLOAD_LIMITS.acceptedExtensions.some((ext) => name.endsWith(ext))
  const typeOk =
    !file.type ||
    file.type === 'application/octet-stream' ||
    file.type.startsWith('image/jpeg') ||
    file.type === 'image/png' ||
    file.type === 'image/webp' ||
    file.type === 'image/heic' ||
    file.type === 'image/heif'
  return extOk && typeOk
}

function formatSize(bytes: number) {
  if (bytes < 1024 * 1024) return `${Math.max(1, Math.round(bytes / 1024))} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

type Props = {
  onDone?: () => void
  onCancel?: () => void
}

export function PhotoUpload({ onDone, onCancel }: Props) {
  const { addPhotos } = useGuestState()
  const [previews, setPreviews] = useState<Preview[]>([])
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState<string | null>(null)

  const countLabel = useMemo(() => `${previews.length} / ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçildi`, [previews.length])

  function onPick(event: ChangeEvent<HTMLInputElement>) {
    setError(null)
    setDone(null)
    const files = Array.from(event.target.files ?? [])
    event.target.value = ''

    if (!files.length) return

    if (files.length > UPLOAD_LIMITS.maxFilesPerRequest) {
      setError(`Tek seferde en fazla ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçebilirsiniz.`)
      return
    }

    setPreviews((current) => {
      if (current.length + files.length > UPLOAD_LIMITS.maxFilesPerRequest) {
        setError(
          `Tek seferde en fazla ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçebilirsiniz. Şu an ${current.length} fotoğraf seçili.`,
        )
        return current
      }

      const next: Preview[] = []
      const problems: string[] = []
      for (const file of files) {
        if (!isAllowed(file)) {
          problems.push(`${file.name} desteklenmeyen bir format. JPG, PNG, WEBP veya HEIC kullanın.`)
          continue
        }
        if (file.size > UPLOAD_LIMITS.maxFileSizeBytes) {
          problems.push(`${file.name} dosyası ${UPLOAD_LIMITS.maxFileSizeMb} MB sınırını aşıyor.`)
          continue
        }
        next.push({
          file,
          heic: isHeic(file),
          url: isHeic(file) ? null : URL.createObjectURL(file),
        })
      }

      const merged = [...current, ...next]
      if (problems.length) setError(problems.join(' '))
      setInfo(`${merged.length} / ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçildi`)
      return merged
    })
  }

  function removeAt(index: number) {
    setPreviews((current) => {
      const copy = [...current]
      const [removed] = copy.splice(index, 1)
      if (removed?.url) URL.revokeObjectURL(removed.url)
      setInfo(`${copy.length} / ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçildi`)
      return copy
    })
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!previews.length) {
      setError('Lütfen en az bir fotoğraf seçin.')
      return
    }
    if (previews.length > UPLOAD_LIMITS.maxFilesPerRequest) {
      setError(`Tek seferde en fazla ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf yükleyebilirsiniz.`)
      return
    }
    const oversized = previews.find((item) => item.file.size > UPLOAD_LIMITS.maxFileSizeBytes)
    if (oversized) {
      setError(`${oversized.file.name} dosyası ${UPLOAD_LIMITS.maxFileSizeMb} MB sınırını aşıyor.`)
      return
    }

    setBusy(true)
    setError(null)
    await new Promise((resolve) => window.setTimeout(resolve, 500))

    const uploaded: Photo[] = previews.map((item) => ({
      id: crypto.randomUUID(),
      fileName: item.file.name,
      fileUrl: item.url ?? '',
      createdAt: new Date().toISOString(),
      isLocal: true,
    }))

    addPhotos(uploaded.filter((photo) => photo.fileUrl))
    previews.forEach((item) => {
      if (item.heic && item.url) URL.revokeObjectURL(item.url)
    })
    setPreviews([])
    setBusy(false)
    setDone('Fotoğraflarınız başarıyla eklendi')
    window.setTimeout(() => onDone?.(), 1200)
  }

  return (
    <form className="upload-card" onSubmit={onSubmit}>
      <div className="upload-intro">
        <h3 className="upload-intro__title">
          Fotoğraflarınızı Bizimle Paylaşın
          <IconCamera size={18} />
        </h3>
        <p>Bu gece çektiğiniz güzel kareleri seçin ve düğün albümümüze ekleyin.</p>
        <p className="upload-intro__meta">
          Tek seferde en fazla {UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf • Fotoğraf başına maksimum{' '}
          {UPLOAD_LIMITS.maxFileSizeMb} MB
        </p>
        <p className="upload-intro__meta">Desteklenen formatlar: JPG, JPEG, PNG, WEBP, HEIC</p>
      </div>

      <label className="file-drop">
        <input type="file" accept={ACCEPT_ATTR} multiple onChange={onPick} />
        <span>Fotoğraf seç</span>
        <small>{countLabel}</small>
      </label>

      {previews.length ? (
        <ul className="preview-grid">
          {previews.map((item, index) => (
            <li key={`${item.file.name}-${item.file.size}-${index}`}>
              {item.url ? (
                <img src={item.url} alt="" />
              ) : (
                <div className="preview-fallback">
                  <strong>HEIC</strong>
                  <span>Bu fotoğraf yüklendikten sonra görüntülenecektir.</span>
                </div>
              )}
              <p>{item.file.name}</p>
              <p className="preview-size">{formatSize(item.file.size)}</p>
              <button type="button" className="text-btn" onClick={() => removeAt(index)}>
                Kaldır
              </button>
            </li>
          ))}
        </ul>
      ) : null}

      <div className={onCancel ? 'dialog-actions' : undefined}>
        {onCancel ? (
          <button className="btn btn--outline" type="button" onClick={onCancel}>
            İptal
          </button>
        ) : null}
        <button className="btn btn--warm" type="submit" disabled={busy || !previews.length}>
          {busy ? 'Yükleniyor…' : 'Yükle'}
        </button>
      </div>
      {info && !error ? <p className="form-note">{info}</p> : null}
      {error ? <p className="form-error">{error}</p> : null}
      {done ? <p className="form-note">{done}</p> : null}
    </form>
  )
}
