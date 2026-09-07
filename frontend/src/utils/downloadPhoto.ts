import { wedding } from '../config/wedding'

export function photoDisplayUrl(photo: { displayUrl?: string; fileUrl?: string }) {
  return photo.displayUrl || photo.fileUrl || ''
}

export function photoOriginalUrl(photo: { originalUrl?: string; displayUrl?: string; fileUrl?: string }) {
  if (photo.originalUrl) return photo.originalUrl
  const displayUrl = photoDisplayUrl(photo)
  return displayUrl.replace(/\/display\.jpg(?:\?.*)?$/i, '/original')
}

export function photoDownloadName(fileName: string | undefined, photoId: string) {
  const fromOriginal = sanitizeDownloadName(fileName)
  if (fromOriginal) return fromOriginal
  const safeId = photoId.replace(/[^a-zA-Z0-9_-]+/g, '-').replace(/^-+|-+$/g, '') || 'foto'
  return `${wedding.downloadFilePrefix}-${safeId}`
}

function sanitizeDownloadName(name: string | undefined) {
  if (!name?.trim()) return ''
  const base = name.replaceAll('\\', '/').split('/').pop()?.trim() ?? ''
  return base.replace(/[<>:"|?*\u0000-\u001f]/g, '').trim()
}

function triggerAnchorDownload(href: string, fileName: string) {
  const link = document.createElement('a')
  link.href = href
  link.download = fileName
  link.rel = 'noopener'
  link.style.display = 'none'
  document.body.appendChild(link)
  link.click()
  link.remove()
}

export async function downloadPhoto(url: string, fileName: string) {
  const response = await fetch(url, { mode: 'cors', credentials: 'omit' })
  if (!response.ok) {
    throw new Error('download failed')
  }

  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  try {
    triggerAnchorDownload(objectUrl, fileName)
  } finally {
    window.setTimeout(() => URL.revokeObjectURL(objectUrl), 2_000)
  }
}
