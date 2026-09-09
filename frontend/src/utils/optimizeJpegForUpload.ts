import {
  PHOTO_UPLOAD_JPEG_QUALITY,
  PHOTO_UPLOAD_MAX_DIMENSION,
  PHOTO_UPLOAD_SKIP_RECOMPRESS_MAX_BYTES,
} from '../config/limits'

export type JpegOptimizeResult = {
  file: File
  skipped: boolean
  originalFileSize: number
  optimizedFileSize: number
  compressionMs: number
}

type Canvas2D = {
  drawImage: (
    image: CanvasImageSource,
    dx: number,
    dy: number,
    dw: number,
    dh: number,
  ) => void
}

export type DecodedJpeg = {
  width: number
  height: number
  draw: (ctx: Canvas2D, width: number, height: number) => void
  close: () => void
}

export type JpegOptimizeCodecs = {
  decodeOriented: (file: File) => Promise<DecodedJpeg>
  encodeJpeg: (
    image: DecodedJpeg,
    width: number,
    height: number,
    quality: number,
  ) => Promise<Blob>
}

export function isJpegUploadFile(file: { name: string; type: string }) {
  const type = file.type.toLowerCase()
  if (type === 'image/jpeg' || type === 'image/jpg') return true
  if (type && type !== 'application/octet-stream') return false
  const name = file.name.toLowerCase()
  return name.endsWith('.jpg') || name.endsWith('.jpeg')
}

export function jpegOutputFileName(name: string) {
  const trimmed = name.trim() || 'photo.jpg'
  const withoutExt = trimmed.replace(/\.(jpe?g)$/i, '')
  return `${withoutExt || 'photo'}.jpg`
}

export function scaleToMaxDimension(width: number, height: number, maxDimension = PHOTO_UPLOAD_MAX_DIMENSION) {
  const longEdge = Math.max(width, height)
  if (longEdge <= maxDimension) {
    return { width, height, resized: false }
  }
  const scale = maxDimension / longEdge
  return {
    width: Math.max(1, Math.round(width * scale)),
    height: Math.max(1, Math.round(height * scale)),
    resized: true,
  }
}

export function shouldSkipJpegRecompress(
  width: number,
  height: number,
  sizeBytes: number,
  maxDimension = PHOTO_UPLOAD_MAX_DIMENSION,
  skipMaxBytes = PHOTO_UPLOAD_SKIP_RECOMPRESS_MAX_BYTES,
) {
  return Math.max(width, height) <= maxDimension && sizeBytes <= skipMaxBytes
}

export function readJpegSofSize(bytes: Uint8Array): { width: number; height: number } | null {
  if (bytes.length < 4 || bytes[0] !== 0xff || bytes[1] !== 0xd8) return null
  let offset = 2
  while (offset + 9 < bytes.length) {
    if (bytes[offset] !== 0xff) {
      offset += 1
      continue
    }
    const marker = bytes[offset + 1]
    if (marker === 0xd8 || marker === 0xff) {
      offset += 1
      continue
    }
    if (marker === 0xd9 || marker === 0xda) break
    if (offset + 3 >= bytes.length) break
    const size = (bytes[offset + 2] << 8) | bytes[offset + 3]
    if (size < 2) return null
    const isSof =
      (marker >= 0xc0 && marker <= 0xc3) ||
      (marker >= 0xc5 && marker <= 0xc7) ||
      (marker >= 0xc9 && marker <= 0xcb) ||
      (marker >= 0xcd && marker <= 0xcf)
    if (isSof) {
      if (offset + 8 >= bytes.length) return null
      const height = (bytes[offset + 5] << 8) | bytes[offset + 6]
      const width = (bytes[offset + 7] << 8) | bytes[offset + 8]
      if (width < 1 || height < 1) return null
      return { width, height }
    }
    offset += 2 + size
  }
  return null
}

function nowMs() {
  return typeof performance !== 'undefined' ? performance.now() : Date.now()
}

function unchanged(file: File, started: number, skipped: boolean): JpegOptimizeResult {
  return {
    file,
    skipped,
    originalFileSize: file.size,
    optimizedFileSize: file.size,
    compressionMs: Math.round(nowMs() - started),
  }
}

async function decodeWithHtmlImage(file: File): Promise<DecodedJpeg> {
  const url = URL.createObjectURL(file)
  try {
    const image = await new Promise<HTMLImageElement>((resolve, reject) => {
      const img = new Image()
      img.onload = () => resolve(img)
      img.onerror = () => reject(new Error('image decode failed'))
      img.src = url
    })
    return {
      width: image.naturalWidth || image.width,
      height: image.naturalHeight || image.height,
      draw: (ctx, width, height) => ctx.drawImage(image, 0, 0, width, height),
      close: () => {
        image.src = ''
      },
    }
  } finally {
    URL.revokeObjectURL(url)
  }
}

function wrapBitmap(bitmap: ImageBitmap): DecodedJpeg {
  return {
    width: bitmap.width,
    height: bitmap.height,
    draw: (ctx, width, height) => ctx.drawImage(bitmap, 0, 0, width, height),
    close: () => bitmap.close(),
  }
}

function preferHtmlImageDecode() {
  if (typeof navigator === 'undefined') return false
  const ua = navigator.userAgent
  return /iP(hone|od|ad)/.test(ua) || (/Safari/i.test(ua) && !/Chrome|CriOS|Android|Edg|OPR|Firefox/i.test(ua))
}

async function decodeOrientedBrowser(file: File): Promise<DecodedJpeg> {
  const canUseBitmap = typeof createImageBitmap === 'function' && !preferHtmlImageDecode()
  if (canUseBitmap) {
    try {
      return wrapBitmap(await createImageBitmap(file, { imageOrientation: 'from-image' }))
    } catch {
      // HTMLImageElement applies EXIF orientation in Safari / Chrome.
    }
  }
  if (typeof Image === 'undefined' || typeof URL === 'undefined' || typeof URL.createObjectURL !== 'function') {
    throw new Error('no image decoder')
  }
  return decodeWithHtmlImage(file)
}

async function encodeJpegBrowser(
  image: DecodedJpeg,
  width: number,
  height: number,
  quality: number,
): Promise<Blob> {
  if (typeof document === 'undefined') {
    throw new Error('no canvas')
  }
  const canvas = document.createElement('canvas')
  canvas.width = width
  canvas.height = height
  const ctx = canvas.getContext('2d', { alpha: false })
  if (!ctx) throw new Error('no 2d context')
  try {
    image.draw(ctx, width, height)
    const blob = await new Promise<Blob | null>((resolve) => {
      canvas.toBlob(resolve, 'image/jpeg', quality)
    })
    if (!blob) throw new Error('jpeg encode failed')
    return blob
  } finally {
    canvas.width = 0
    canvas.height = 0
  }
}

const browserCodecs: JpegOptimizeCodecs = {
  decodeOriented: decodeOrientedBrowser,
  encodeJpeg: encodeJpegBrowser,
}

export async function optimizeJpegForUpload(
  file: File,
  codecs: JpegOptimizeCodecs = browserCodecs,
): Promise<JpegOptimizeResult> {
  const started = nowMs()
  if (!isJpegUploadFile(file)) {
    return unchanged(file, started, true)
  }

  try {
    const header = new Uint8Array(await file.slice(0, 128 * 1024).arrayBuffer())
    const sof = readJpegSofSize(header)
    if (sof && shouldSkipJpegRecompress(sof.width, sof.height, file.size)) {
      return unchanged(file, started, true)
    }

    const decoded = await codecs.decodeOriented(file)
    try {
      if (decoded.width < 1 || decoded.height < 1) {
        return unchanged(file, started, true)
      }
      if (shouldSkipJpegRecompress(decoded.width, decoded.height, file.size)) {
        return unchanged(file, started, true)
      }

      const scaled = scaleToMaxDimension(decoded.width, decoded.height)
      const blob = await codecs.encodeJpeg(
        decoded,
        scaled.width,
        scaled.height,
        PHOTO_UPLOAD_JPEG_QUALITY,
      )
      if (!scaled.resized && blob.size >= file.size) {
        return unchanged(file, started, true)
      }

      const optimized = new File([blob], jpegOutputFileName(file.name), {
        type: 'image/jpeg',
        lastModified: file.lastModified,
      })
      return {
        file: optimized,
        skipped: false,
        originalFileSize: file.size,
        optimizedFileSize: optimized.size,
        compressionMs: Math.round(nowMs() - started),
      }
    } finally {
      decoded.close()
    }
  } catch {
    return unchanged(file, started, true)
  }
}
