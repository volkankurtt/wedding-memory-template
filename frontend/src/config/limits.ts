/** Shared guest limits. Backend upload/memory APIs must enforce the same values. */
export const UPLOAD_LIMITS = {
  maxFilesPerRequest: 30,
  maxFileSizeMb: 25,
  maxFileSizeBytes: 25 * 1024 * 1024,
  acceptedExtensions: ['.jpg', '.jpeg', '.png', '.webp', '.heic', '.heif'] as const,
  acceptedMimeTypes: [
    'image/jpeg',
    'image/png',
    'image/webp',
    'image/heic',
    'image/heif',
  ] as const,
}

/** Browser JPEG optimize before signed upload. Gallery display remains backend max 1920. */
export const PHOTO_UPLOAD_MAX_DIMENSION = 2560
export const PHOTO_UPLOAD_JPEG_QUALITY = 0.82
/** Skip re-encode when already within max dimension and already compact. */
export const PHOTO_UPLOAD_SKIP_RECOMPRESS_MAX_BYTES = 1_500_000

export const MEMORY_LIMITS = {
  maxChars: 600,
}

export const ACCEPT_ATTR = [
  ...UPLOAD_LIMITS.acceptedMimeTypes,
  ...UPLOAD_LIMITS.acceptedExtensions,
].join(',')
