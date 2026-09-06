import img1 from '../assets/couple/1.jpg'
import img2 from '../assets/couple/2.jpg'
import img3 from '../assets/couple/3.jpg'
import { coupleByFile } from '../assets/couple'
import type { BackgroundImage, WeddingInfo } from '../types'

const venueQuery = 'Anadolu Park Uğurlu Bahçe, Konak Serik Cd. Yan Yolu, Aksu, Antalya'

const backgroundSettings: Array<{
  file: string
  desktopPosition: string
  mobilePosition: string
}> = [
  { file: '1.jpg', desktopPosition: 'center 46%', mobilePosition: 'center 18%' },
  { file: '2.jpg', desktopPosition: 'center 40%', mobilePosition: 'center 14%' },
  { file: '3.jpg', desktopPosition: 'center 44%', mobilePosition: 'center 16%' },
  { file: '4.jpg', desktopPosition: 'center 42%', mobilePosition: 'center 18%' },
  { file: '5.jpg', desktopPosition: 'center 42%', mobilePosition: 'center 18%' },
]

const srcByFile: Record<string, string> = {
  '1.jpg': img1,
  '2.jpg': img2,
  '3.jpg': img3,
  ...coupleByFile,
}

export const backgroundImages: BackgroundImage[] = backgroundSettings.flatMap((item) => {
  const src = srcByFile[item.file]
  if (!src) return []
  return [{ ...item, src }]
})

export const wedding: WeddingInfo = {
  brideName: 'Gizem',
  groomName: 'Alper',
  weddingDate: '2026-09-11',
  weddingTime: '19:00',
  timezone: 'Europe/Istanbul',
  venueName: 'Anadolu Park Uğurlu Bahçe',
  address: 'Konak Serik Cd. Yan Yolu, 07112 Çıkışı, Aksu / Antalya',
  venueShort: 'Aksu / Antalya',
  googleMapsUrl: `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(venueQuery)}`,
  heroTitle: 'BU ANI BİRLİKTE ÖLÜMSÜZLEŞTİRELİM',
  heroTitleLines: ['BU ANI BİRLİKTE', 'ÖLÜMSÜZLEŞTİRELİM'],
  heroDescription:
    'Bu gece objektifinize takılan güzel anları bizimle paylaşın. QR kodu okutun, fotoğraflarınızı yükleyin.',
  galleryEmpty: 'Henüz fotoğraf yüklenmedi. İlk anıyı siz paylaşın.',
  memoriesEmpty: 'Henüz anı bırakılmadı. İlk güzel mesajı siz yazın.',
  memoryLead: 'Fotoğraflar günü gösterir, yazdıklarınız o günü hatırlatır.',
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
