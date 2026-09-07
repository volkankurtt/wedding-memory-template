import { useMemo, useState, type ChangeEvent, type FormEvent } from 'react'
import { toUserMessage, uploadPhoto } from '../api/client'
import { ACCEPT_ATTR, UPLOAD_LIMITS } from '../config/limits'
import { useGuestState } from '../context/GuestState'
import { IconCamera } from './Icons'

type UploadStatus = 'WAITING' | 'UPLOADING' | 'SUCCESS' | 'FAILED'

type QueueItem = {
  id: string
  file: File
  url: string | null
  heic: boolean
  status: UploadStatus
  error: string | null
}

const MAX_CONCURRENCY = 3

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

function statusLabel(item: QueueItem) {
  if (item.status === 'WAITING') return 'Bekliyor'
  if (item.status === 'UPLOADING') return 'Yükleniyor'
  if (item.status === 'SUCCESS') return 'Yüklendi'
  return item.error ?? 'Yüklenemedi'
}

async function runPool<T>(items: T[], worker: (item: T) => Promise<void>) {
  let next = 0
  const runners = Array.from({ length: Math.min(MAX_CONCURRENCY, items.length) }, async () => {
    while (next < items.length) {
      const current = next
      next += 1
      await worker(items[current])
    }
  })
  await Promise.all(runners)
}

type Props = {
  onDone?: () => void
  onCancel?: () => void
}

export function PhotoUpload({ onDone, onCancel }: Props) {
  const { prependPhotos } = useGuestState()
  const [queue, setQueue] = useState<QueueItem[]>([])
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [done, setDone] = useState<string | null>(null)

  const selected = queue.filter((item) => item.status !== 'SUCCESS')
  const successCount = queue.filter((item) => item.status === 'SUCCESS').length
  const countLabel = useMemo(
    () => `${queue.length} / ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçildi`,
    [queue.length],
  )

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

    setQueue((current) => {
      if (current.length + files.length > UPLOAD_LIMITS.maxFilesPerRequest) {
        setError(
          `Tek seferde en fazla ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçebilirsiniz. Şu an ${current.length} fotoğraf seçili.`,
        )
        return current
      }

      const next: QueueItem[] = []
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
          id: crypto.randomUUID(),
          file,
          heic: isHeic(file),
          url: isHeic(file) ? null : URL.createObjectURL(file),
          status: 'WAITING',
          error: null,
        })
      }

      const merged = [...current, ...next]
      if (problems.length) setError(problems.join(' '))
      setInfo(`${merged.length} / ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçildi`)
      return merged
    })
  }

  function removeAt(id: string) {
    setQueue((current) => {
      const removed = current.find((item) => item.id === id)
      if (removed?.url) URL.revokeObjectURL(removed.url)
      const next = current.filter((item) => item.id !== id)
      setInfo(`${next.length} / ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf seçildi`)
      return next
    })
  }

  async function uploadOne(item: QueueItem) {
    const { id, file } = item
    setQueue((current) =>
      current.map((entry) =>
        entry.id === id ? { ...entry, status: 'UPLOADING', error: null } : entry,
      ),
    )

    try {
      const photo = await uploadPhoto(file)
      prependPhotos([photo])
      setQueue((current) =>
        current.map((entry) => (entry.id === id ? { ...entry, status: 'SUCCESS', error: null } : entry)),
      )
    } catch (cause) {
      const message = toUserMessage(cause)
      setQueue((current) =>
        current.map((entry) =>
          entry.id === id ? { ...entry, status: 'FAILED', error: message } : entry,
        ),
      )
    }
  }

  async function uploadIds(items: QueueItem[]) {
    if (!items.length) return
    setBusy(true)
    setError(null)
    setDone(null)
    await runPool(items, uploadOne)
    setQueue((current) => {
      const ok = current.filter((entry) => entry.status === 'SUCCESS').length
      const failed = current.filter((entry) => entry.status === 'FAILED').length
      setInfo(`${ok} / ${current.length} fotoğraf yüklendi`)
      if (failed === 0 && ok > 0) {
        setDone('Fotoğraflarınız başarıyla eklendi')
        window.setTimeout(() => onDone?.(), 1200)
      } else if (failed > 0) {
        setError('Bazı fotoğraflar yüklenemedi. Tekrar deneyebilirsiniz.')
      }
      return current
    })
    setBusy(false)
  }

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    if (!queue.length) {
      setError('Lütfen en az bir fotoğraf seçin.')
      return
    }
    if (queue.length > UPLOAD_LIMITS.maxFilesPerRequest) {
      setError(`Tek seferde en fazla ${UPLOAD_LIMITS.maxFilesPerRequest} fotoğraf yükleyebilirsiniz.`)
      return
    }
    const oversized = queue.find((item) => item.file.size > UPLOAD_LIMITS.maxFileSizeBytes)
    if (oversized) {
      setError(`${oversized.file.name} dosyası ${UPLOAD_LIMITS.maxFileSizeMb} MB sınırını aşıyor.`)
      return
    }
    const pending = queue.filter((item) => item.status !== 'SUCCESS')
    await uploadIds(pending)
  }

  function retryOne(id: string) {
    const item = queue.find((entry) => entry.id === id)
    if (item) void uploadIds([item])
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
        <input type="file" accept={ACCEPT_ATTR} multiple disabled={busy} onChange={onPick} />
        <span>Fotoğraf seç</span>
        <small>{countLabel}</small>
      </label>

      {queue.length ? (
        <ul className="preview-grid">
          {queue.map((item) => (
            <li key={item.id}>
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
              <p className={item.status === 'FAILED' ? 'form-error' : 'preview-size'}>{statusLabel(item)}</p>
              {item.status === 'FAILED' ? (
                <button type="button" className="text-btn" disabled={busy} onClick={() => retryOne(item.id)}>
                  Tekrar Dene
                </button>
              ) : null}
              {item.status === 'WAITING' ? (
                <button type="button" className="text-btn" disabled={busy} onClick={() => removeAt(item.id)}>
                  Kaldır
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      ) : null}

      <div className={onCancel ? 'dialog-actions' : undefined}>
        {onCancel ? (
          <button className="btn btn--outline" type="button" onClick={onCancel} disabled={busy}>
            İptal
          </button>
        ) : null}
        <button className="btn btn--warm" type="submit" disabled={busy || !selected.length}>
          {busy ? 'Yükleniyor…' : 'Yükle'}
        </button>
      </div>
      {busy ? (
        <p className="form-note">
          {successCount} / {queue.length} fotoğraf yüklendi
        </p>
      ) : info && !error ? (
        <p className="form-note">{info}</p>
      ) : null}
      {error ? <p className="form-error">{error}</p> : null}
      {done ? <p className="form-note">{done}</p> : null}
    </form>
  )
}
