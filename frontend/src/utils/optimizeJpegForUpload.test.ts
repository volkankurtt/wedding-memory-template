import { describe, expect, it } from 'vitest'
import {
  PHOTO_UPLOAD_JPEG_QUALITY,
  PHOTO_UPLOAD_MAX_DIMENSION,
  PHOTO_UPLOAD_SKIP_RECOMPRESS_MAX_BYTES,
} from '../config/limits'
import {
  isJpegUploadFile,
  jpegOutputFileName,
  optimizeJpegForUpload,
  readJpegSofSize,
  scaleToMaxDimension,
  shouldSkipJpegRecompress,
  type DecodedJpeg,
  type JpegOptimizeCodecs,
} from './optimizeJpegForUpload'

function jpegWithSof(width: number, height: number, totalBytes: number, name = 'IMG_1234.jpg') {
  const header = new Uint8Array(20)
  header[0] = 0xff
  header[1] = 0xd8
  header[2] = 0xff
  header[3] = 0xc0
  header[4] = 0x00
  header[5] = 0x11
  header[6] = 0x08
  header[7] = (height >> 8) & 0xff
  header[8] = height & 0xff
  header[9] = (width >> 8) & 0xff
  header[10] = width & 0xff
  header[11] = 1
  const body = new Uint8Array(Math.max(totalBytes, header.length))
  body.set(header)
  return new File([body], name, { type: 'image/jpeg' })
}

function fakeCodecs(decoded: { width: number; height: number }, outputBytes: number): JpegOptimizeCodecs {
  return {
    decodeOriented: async () => {
      const image: DecodedJpeg = {
        width: decoded.width,
        height: decoded.height,
        draw: () => undefined,
        close: () => undefined,
      }
      return image
    },
    encodeJpeg: async (_image, width, height, quality) => {
      expect(quality).toBe(PHOTO_UPLOAD_JPEG_QUALITY)
      expect(Math.max(width, height)).toBeLessThanOrEqual(PHOTO_UPLOAD_MAX_DIMENSION)
      return new Blob([new Uint8Array(outputBytes)], { type: 'image/jpeg' })
    },
  }
}

describe('jpeg upload optimize helpers', () => {
  it('detects jpeg by mime or extension and ignores other formats', () => {
    expect(isJpegUploadFile({ name: 'a.jpg', type: 'image/jpeg' })).toBe(true)
    expect(isJpegUploadFile({ name: 'a.JPG', type: '' })).toBe(true)
    expect(isJpegUploadFile({ name: 'a.jpeg', type: 'application/octet-stream' })).toBe(true)
    expect(isJpegUploadFile({ name: 'a.png', type: 'image/png' })).toBe(false)
    expect(isJpegUploadFile({ name: 'a.webp', type: 'image/webp' })).toBe(false)
    expect(isJpegUploadFile({ name: 'a.heic', type: 'image/heic' })).toBe(false)
    expect(isJpegUploadFile({ name: 'a.heif', type: 'image/heif' })).toBe(false)
  })

  it('keeps the original file name with a jpg extension', () => {
    expect(jpegOutputFileName('IMG_1234.JPG')).toBe('IMG_1234.jpg')
    expect(jpegOutputFileName('IMG_1234.jpeg')).toBe('IMG_1234.jpg')
    expect(jpegOutputFileName('holiday')).toBe('holiday.jpg')
  })

  it('scales 4032x3024 landscape to 2560 on the long edge', () => {
    expect(scaleToMaxDimension(4032, 3024)).toEqual({
      width: 2560,
      height: 1920,
      resized: true,
    })
  })

  it('scales portrait without changing aspect ratio', () => {
    expect(scaleToMaxDimension(3024, 4032)).toEqual({
      width: 1920,
      height: 2560,
      resized: true,
    })
  })

  it('does not upscale a small jpeg', () => {
    expect(scaleToMaxDimension(800, 600)).toEqual({ width: 800, height: 600, resized: false })
    expect(shouldSkipJpegRecompress(800, 600, 400_000)).toBe(true)
  })

  it('reads SOF width and height', () => {
    const file = jpegWithSof(4032, 3024, 64)
    expect(file.size).toBeGreaterThan(0)
    expect(readJpegSofSize(new Uint8Array(64))).toBeNull()
    expect(readJpegSofSize(new Uint8Array([0xff, 0xd8, 0xff, 0xc0, 0x00, 0x11, 0x08, 0x0c, 0x00, 0x0f, 0xc0, 0x01]))).toEqual({
      width: 4032,
      height: 3072,
    })
  })
})

describe('optimizeJpegForUpload', () => {
  it('leaves png, webp and heic bytes unchanged', async () => {
    for (const [name, type] of [
      ['a.png', 'image/png'],
      ['a.webp', 'image/webp'],
      ['a.heic', 'image/heic'],
    ] as const) {
      const file = new File([new Uint8Array([1, 2, 3, 4])], name, { type })
      const result = await optimizeJpegForUpload(file)
      expect(result.skipped).toBe(true)
      expect(result.file).toBe(file)
      expect(result.optimizedFileSize).toBe(file.size)
    }
  })

  it('skips a jpeg that is already small and within max dimension', async () => {
    const file = jpegWithSof(1280, 960, 80_000)
    const result = await optimizeJpegForUpload(file, fakeCodecs({ width: 1280, height: 960 }, 10_000))
    expect(result.skipped).toBe(true)
    expect(result.file).toBe(file)
    expect(file.size).toBeLessThanOrEqual(PHOTO_UPLOAD_SKIP_RECOMPRESS_MAX_BYTES)
  })

  it('resizes a 4032x3024 jpeg and sends image/jpeg', async () => {
    const file = jpegWithSof(4032, 3024, 5_000_000, 'IMG_1234.JPG')
    const result = await optimizeJpegForUpload(file, fakeCodecs({ width: 4032, height: 3024 }, 1_200_000))
    expect(result.skipped).toBe(false)
    expect(result.file.name).toBe('IMG_1234.jpg')
    expect(result.file.type).toBe('image/jpeg')
    expect(result.originalFileSize).toBe(file.size)
    expect(result.optimizedFileSize).toBe(1_200_000)
    expect(result.optimizedFileSize / result.originalFileSize).toBeLessThan(0.3)
  })

  it('uses oriented portrait pixels for EXIF orientation 6', async () => {
    const file = jpegWithSof(4032, 3024, 4_000_000)
    let encoded = { width: 0, height: 0 }
    const codecs: JpegOptimizeCodecs = {
      decodeOriented: async () => ({
        width: 3024,
        height: 4032,
        draw: () => undefined,
        close: () => undefined,
      }),
      encodeJpeg: async (_image, width, height) => {
        encoded = { width, height }
        return new Blob([new Uint8Array(900_000)], { type: 'image/jpeg' })
      },
    }
    const result = await optimizeJpegForUpload(file, codecs)
    expect(result.skipped).toBe(false)
    expect(encoded).toEqual({ width: 1920, height: 2560 })
  })

  it('uses oriented portrait pixels for EXIF orientation 8', async () => {
    const file = jpegWithSof(4032, 3024, 4_000_000)
    let encoded = { width: 0, height: 0 }
    const result = await optimizeJpegForUpload(file, {
      decodeOriented: async () => ({
        width: 3024,
        height: 4032,
        draw: () => undefined,
        close: () => undefined,
      }),
      encodeJpeg: async (_image, width, height) => {
        encoded = { width, height }
        return new Blob([new Uint8Array(900_000)], { type: 'image/jpeg' })
      },
    })
    expect(result.skipped).toBe(false)
    expect(encoded).toEqual({ width: 1920, height: 2560 })
  })

  it('optimizes three jpegs in parallel without sharing decode state', async () => {
    const files = [1, 2, 3].map((n) => jpegWithSof(4032, 3024, 2_000_000, `IMG_${n}.jpg`))
    const results = await Promise.all(
      files.map((file) => optimizeJpegForUpload(file, fakeCodecs({ width: 4032, height: 3024 }, 400_000))),
    )
    expect(results.map((item) => item.file.name)).toEqual(['IMG_1.jpg', 'IMG_2.jpg', 'IMG_3.jpg'])
    expect(results.every((item) => !item.skipped && item.optimizedFileSize === 400_000)).toBe(true)
  })

  it('does not decode a 20-file queue of already-small jpegs', async () => {
    let decoded = 0
    const codecs: JpegOptimizeCodecs = {
      decodeOriented: async () => {
        decoded += 1
        throw new Error('should not decode')
      },
      encodeJpeg: async () => {
        throw new Error('should not encode')
      },
    }
    const files = Array.from({ length: 20 }, (_, i) => jpegWithSof(800, 600, 50_000, `small-${i}.jpg`))
    const results = await Promise.all(files.map((file) => optimizeJpegForUpload(file, codecs)))
    expect(decoded).toBe(0)
    expect(results.every((item, index) => item.skipped && item.file === files[index])).toBe(true)
  })

  it('falls back to the original file when decode is unavailable', async () => {
    const file = jpegWithSof(4032, 3024, 2_000_000)
    const result = await optimizeJpegForUpload(file)
    expect(result.file).toBe(file)
    expect(result.skipped).toBe(true)
  })
})
