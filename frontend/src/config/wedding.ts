import { coupleByFile } from '../assets/couple'
import type { BackgroundImage, WeddingInfo } from '../types'
import { weddingConfig } from './weddingConfig'

const srcByFile: Record<string, string> = { ...coupleByFile }

export const backgroundImages: BackgroundImage[] = weddingConfig.backgroundPhotos.flatMap((item) => {
  const src = srcByFile[item.file]
  if (!src) return []
  return [{ ...item, src }]
})

export const wedding: WeddingInfo = {
  brideName: weddingConfig.brideName,
  groomName: weddingConfig.groomName,
  weddingDate: weddingConfig.weddingDate,
  weddingTime: weddingConfig.weddingTime,
  timezone: weddingConfig.timezone,
  venueName: weddingConfig.venueName,
  address: weddingConfig.address,
  venueShort: weddingConfig.venueShort,
  googleMapsUrl: `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(weddingConfig.mapsQuery)}`,
  pageTitle: weddingConfig.pageTitle,
  downloadFilePrefix: weddingConfig.downloadFilePrefix,
  heroTitle: weddingConfig.heroTitle,
  heroTitleLines: [weddingConfig.heroTitleLines[0], weddingConfig.heroTitleLines[1]],
  heroDescription: weddingConfig.heroDescription,
  galleryTitle: weddingConfig.galleryTitle,
  galleryLead: weddingConfig.galleryLead,
  galleryEmpty: weddingConfig.galleryEmpty,
  memoriesEmpty: weddingConfig.memoriesEmpty,
  memoryLead: weddingConfig.memoryLead,
}

export const coupleNames = `${wedding.brideName} & ${wedding.groomName}`

export function formatWeddingDate(style: 'numeric' | 'long' = 'numeric') {
  return new Intl.DateTimeFormat('tr-TR', {
    day: style === 'numeric' ? '2-digit' : 'numeric',
    month: style === 'numeric' ? '2-digit' : 'long',
    year: 'numeric',
    timeZone: wedding.timezone,
  }).format(new Date(`${wedding.weddingDate}T12:00:00+03:00`))
}

export { ACCEPT_ATTR, MEMORY_LIMITS, UPLOAD_LIMITS } from './limits'

export const ACCEPTED_IMAGE_TYPES = [
  'image/jpeg',
  'image/png',
  'image/webp',
  'image/heic',
  'image/heif',
] as const

export const ACCEPTED_EXTENSIONS = ['.jpg', '.jpeg', '.png', '.webp', '.heic', '.heif'] as const

export const MAX_FILE_SIZE_MB = 25
export const MAX_MEMORY_CHARS = 600
export const BACKGROUND_INTERVAL_MS = 5000
