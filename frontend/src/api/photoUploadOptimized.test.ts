import { afterEach, describe, expect, it, vi } from 'vitest'

vi.mock('../utils/optimizeJpegForUpload', () => ({
  optimizeJpegForUpload: async (file: File) => {
    const optimized = new File([new Uint8Array(1_234_567)], 'IMG_1234.jpg', { type: 'image/jpeg' })
    return {
      file: optimized,
      skipped: false,
      originalFileSize: file.size,
      optimizedFileSize: optimized.size,
      compressionMs: 15,
    }
  },
}))

import { uploadPhotoWithRetry } from './client'

afterEach(() => {
  vi.unstubAllGlobals()
})

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('optimized jpeg upload session', () => {
  it('sends optimized fileName, contentType and sizeBytes to upload-session', async () => {
    const fetchMock = vi.fn(async (url: string, _init?: RequestInit) => {
      const path = String(url)
      if (path.includes('/api/photos/upload-session')) {
        return jsonResponse(
          {
            photoId: 'p1',
            clientUploadId: 'upload-1',
            path: 'p1/original',
            signedUrl: 'https://storage.example/sign/p1',
            token: 'tok',
            expiresInSeconds: 7200,
            alreadyReady: false,
            needsUpload: true,
          },
          201,
        )
      }
      if (path.includes('storage.example')) return new Response(null, { status: 200 })
      if (path.includes('/finalize')) {
        return jsonResponse({ id: 'p1', fileName: 'IMG_1234.jpg', fileUrl: '/display.jpg', createdAt: '2026-01-01T00:00:00Z' })
      }
      return jsonResponse({ error: 'unexpected' }, 500)
    })
    vi.stubGlobal('fetch', fetchMock)

    const original = new File([new Uint8Array(80)], 'IMG_1234.JPG', { type: 'image/jpeg' })
    await uploadPhotoWithRetry(original, 'upload-1')

    const sessionInit = fetchMock.mock.calls.find((call) => String(call[0]).includes('/api/photos/upload-session'))?.[1]
    expect(JSON.parse(String(sessionInit?.body))).toEqual({
      clientUploadId: 'upload-1',
      fileName: 'IMG_1234.jpg',
      contentType: 'image/jpeg',
      sizeBytes: 1_234_567,
    })
    const storageCall = fetchMock.mock.calls.find((call) => String(call[0]).includes('storage.example'))
    expect(storageCall?.[1]?.body).toBeInstanceOf(FormData)
  })
})
