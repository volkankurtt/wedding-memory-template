/**
 * Yeni bir çift için yalnızca bu dosyayı ve `src/assets/couple/` fotoğraflarını güncelleyin.
 * Bileşenlere çift adı, tarih veya mekan yazmayın.
 */
export const weddingConfig = {
  brideName: 'Gizem',
  groomName: 'Alper',
  weddingDate: '2026-09-11',
  weddingTime: '19:00',
  timezone: 'Europe/Istanbul',
  venueName: 'Anadolu Park Uğurlu Bahçe',
  address: 'Konak Serik Cd. Yan Yolu, 07112 Çıkışı, Aksu / Antalya',
  venueShort: 'Aksu / Antalya',
  mapsQuery: 'Anadolu Park Uğurlu Bahçe, Konak Serik Cd. Yan Yolu, Aksu, Antalya',
  pageTitle: 'Gizem & Alper — Düğünümüz',
  downloadFilePrefix: 'gizem-alper-anisi',
  heroTitle: 'BU ANI BİRLİKTE ÖLÜMSÜZLEŞTİRELİM',
  heroTitleLines: ['BU ANI BİRLİKTE', 'ÖLÜMSÜZLEŞTİRELİM'] as [string, string],
  heroDescription:
    'Bu gece objektifinize takılan güzel anları bizimle paylaşın. QR kodu okutun, fotoğraflarınızı yükleyin.',
  galleryTitle: 'Düğün Anılarımız',
  galleryLead: 'Bu güzel geceden paylaşılan kareleri burada görebilirsiniz.',
  galleryEmpty: 'Henüz fotoğraf yüklenmedi. İlk anıyı siz paylaşın.',
  memoriesEmpty: 'Henüz anı bırakılmadı. İlk güzel mesajı siz yazın.',
  memoryLead: 'Fotoğraflar günü gösterir, yazdıklarınız o günü hatırlatır.',
  backgroundPhotos: [
    { file: '1.jpg', desktopPosition: 'center 46%', mobilePosition: 'center 18%' },
    { file: '2.jpg', desktopPosition: 'center 40%', mobilePosition: 'center 14%' },
    { file: '3.jpg', desktopPosition: 'center 44%', mobilePosition: 'center 16%' },
    { file: '4.jpg', desktopPosition: 'center 42%', mobilePosition: 'center 18%' },
    { file: '5.jpg', desktopPosition: 'center 42%', mobilePosition: 'center 18%' },
  ],
} as const
