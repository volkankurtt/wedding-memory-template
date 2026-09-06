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

export const MEMORY_LIMITS = {
  maxChars: 600,
}

export const ACCEPT_ATTR = [
  ...UPLOAD_LIMITS.acceptedMimeTypes,
  ...UPLOAD_LIMITS.acceptedExtensions,
].join(',')
