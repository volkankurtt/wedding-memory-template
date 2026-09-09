import { afterEach, describe, expect, it, vi } from 'vitest'
import {
  BACKEND_READY_TTL_MS,
  ensureBackendReady,
  isBackendReady,
  markBackendReady,
  resetBackendReady,
} from './backendReady'
import { ApiError, isTransientUploadError, uploadPhotoWithRetry, waitForApi } from './client'
import {
  canViewUploadedPhotos,
  failedUploadItems,
  pendingUploadItems,
  runPhotoUploadSession,
} from './photoUploadSession'

afterEach(() => {
  resetBackendReady()
  vi.unstubAllGlobals()
  vi.restoreAllMocks()
})

function jsonResponse(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  })
}

describe('waitForApi', () => {
  it('returns on first successful health without sleeping', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ status: 'ok' }))
    vi.stubGlobal('fetch', fetchMock)
    const sleep = vi.fn(async () => undefined)

    await waitForApi({ timeoutMs: 90_000, retryDelayMs: 4_000, sleep, now: () => 0 })

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(sleep).not.toHaveBeenCalled()
    expect(isBackendReady(0)).toBe(true)
  })

  it('retries cold-start health errors then continues', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ error: 'bad gateway' }, 502))
      .mockRejectedValueOnce(new TypeError('Failed to fetch'))
      .mockResolvedValueOnce(jsonResponse({ status: 'ok' }))
    vi.stubGlobal('fetch', fetchMock)
    const sleep = vi.fn(async () => undefined)

    await waitForApi({ timeoutMs: 90_000, retryDelayMs: 4_000, sleep, now: () => 0 })

    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/health')
    expect(sleep).toHaveBeenCalled()
  })

  it('does not start uploads when health never succeeds', async () => {
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ error: 'unavailable' }, 503))
    vi.stubGlobal('fetch', fetchMock)
    let clock = 0
    const upload = vi.fn(async () => undefined)

    await expect(
      runPhotoUploadSession({
        items: [{ id: 'a' }],
        waitForApi: () =>
          waitForApi({
            timeoutMs: 8_000,
            retryDelayMs: 4_000,
            attemptTimeoutMs: 1_000,
            now: () => clock,
            sleep: async (ms) => {
              clock += ms
            },
          }),
        upload,
      }),
    ).rejects.toBeInstanceOf(ApiError)

    expect(upload).not.toHaveBeenCalled()
    expect(fetchMock.mock.calls.every((call) => String(call[0]).includes('/api/health'))).toBe(true)
  })
})

describe('uploadPhotoWithRetry', () => {
  it('uploads one jpeg when backend is ready', async () => {
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
      if (path.includes('/api/photos/p1/finalize')) {
        return jsonResponse(
          { id: 'p1', fileName: 'a.jpg', fileUrl: '/display.jpg', createdAt: '2026-01-01T00:00:00Z' },
          200,
        )
      }
      return jsonResponse({ error: 'unexpected' }, 500)
    })
    vi.stubGlobal('fetch', fetchMock)

    const original = new File([new Uint8Array([1, 2, 3])], 'a.jpg', { type: 'image/jpeg' })
    await uploadPhotoWithRetry(original, 'upload-1')

    expect(fetchMock).toHaveBeenCalledTimes(3)
    expect(isBackendReady()).toBe(true)
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/photos/upload-session')
    const sessionInit = fetchMock.mock.calls[0][1]
    expect(JSON.parse(String(sessionInit?.body))).toEqual({
      clientUploadId: 'upload-1',
      fileName: 'a.jpg',
      contentType: 'image/jpeg',
      sizeBytes: original.size,
    })
    expect(String(fetchMock.mock.calls[1][0])).toContain('https://storage.example/sign/p1')
    expect(String(fetchMock.mock.calls[1][0])).toContain('token=tok')
    expect(fetchMock.mock.calls[1][1]?.method).toBe('PUT')
    const uploadHeaders = fetchMock.mock.calls[1][1]?.headers as Record<string, string>
    expect(uploadHeaders['x-upsert']).toBe('false')
    expect(uploadHeaders.Authorization).toBeUndefined()
    expect(fetchMock.mock.calls[1][1]?.body).toBeInstanceOf(FormData)
    expect(String(fetchMock.mock.calls[2][0])).toContain('/api/photos/p1/finalize')
  })

  it('retries a transient 502 once with the same upload id', async () => {
    let sessions = 0
    const fetchMock = vi.fn(async (url: string, _init?: RequestInit) => {
      const path = String(url)
      if (path.includes('/api/photos/upload-session')) {
        sessions += 1
        if (sessions === 1) return jsonResponse({ error: 'bad gateway' }, 502)
        return jsonResponse(
          {
            photoId: 'p1',
            clientUploadId: 'same-id',
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
        return jsonResponse(
          { id: 'p1', fileName: 'a.jpg', fileUrl: '/display.jpg', createdAt: '2026-01-01T00:00:00Z' },
          200,
        )
      }
      return jsonResponse({ error: 'unexpected' }, 500)
    })
    vi.stubGlobal('fetch', fetchMock)

    await uploadPhotoWithRetry(new File([new Uint8Array([1])], 'a.jpg', { type: 'image/jpeg' }), 'same-id')

    const sessionBodies = fetchMock.mock.calls
      .filter((call) => String(call[0]).includes('upload-session'))
      .map((call) => JSON.parse(String(call[1]?.body)))
    expect(sessionBodies).toHaveLength(2)
    expect(sessionBodies[0].clientUploadId).toBe('same-id')
    expect(sessionBodies[1].clientUploadId).toBe('same-id')
  })

  it('does not retry validation or rate-limit errors', async () => {
    for (const status of [400, 413, 415, 429]) {
      const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ error: 'no' }, status))
      vi.stubGlobal('fetch', fetchMock)
      await expect(
        uploadPhotoWithRetry(new File([new Uint8Array([1])], 'a.jpg', { type: 'image/jpeg' }), 'id'),
      ).rejects.toMatchObject({ status })
      expect(fetchMock).toHaveBeenCalledTimes(1)
    }
  })

  it('uploads three jpegs in parallel without duplicate session ids', async () => {
    const seen = new Set<string>()
    const fetchMock = vi.fn(async (url: string, init?: RequestInit) => {
      const path = String(url)
      if (path.includes('/api/photos/upload-session')) {
        const body = JSON.parse(String(init?.body)) as { clientUploadId: string }
        seen.add(body.clientUploadId)
        return jsonResponse(
          {
            photoId: `p-${body.clientUploadId}`,
            clientUploadId: body.clientUploadId,
            path: `p-${body.clientUploadId}/original`,
            signedUrl: `https://storage.example/sign/${body.clientUploadId}`,
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
        const id = path.split('/api/photos/')[1].split('/finalize')[0]
        return jsonResponse({ id, fileName: 'a.jpg', fileUrl: '/display.jpg', createdAt: '2026-01-01T00:00:00Z' })
      }
      return jsonResponse({ error: 'unexpected' }, 500)
    })
    vi.stubGlobal('fetch', fetchMock)
    const file = () => new File([new Uint8Array([1])], 'a.jpg', { type: 'image/jpeg' })
    await Promise.all([
      uploadPhotoWithRetry(file(), '1'),
      uploadPhotoWithRetry(file(), '2'),
      uploadPhotoWithRetry(file(), '3'),
    ])
    expect([...seen].sort()).toEqual(['1', '2', '3'])
    expect(fetchMock.mock.calls.filter((call) => String(call[0]).includes('upload-session'))).toHaveLength(3)
    expect(fetchMock.mock.calls.filter((call) => String(call[0]).includes('storage.example'))).toHaveLength(3)
    expect(fetchMock.mock.calls.filter((call) => String(call[0]).includes('/finalize'))).toHaveLength(3)
  })

  it('does not finalize when signed upload fails', async () => {
    const fetchMock = vi.fn(async (url: string) => {
      const path = String(url)
      if (path.includes('/api/photos/upload-session')) {
        return jsonResponse(
          {
            photoId: 'p1',
            clientUploadId: 'id',
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
      if (path.includes('storage.example')) return new Response('denied', { status: 403 })
      return jsonResponse({ error: 'unexpected' }, 500)
    })
    vi.stubGlobal('fetch', fetchMock)
    await expect(
      uploadPhotoWithRetry(new File([new Uint8Array([1])], 'a.jpg', { type: 'image/jpeg' }), 'id'),
    ).rejects.toMatchObject({ status: 403 })
    expect(fetchMock.mock.calls.some((call) => String(call[0]).includes('/finalize'))).toBe(false)
  })

  it('returns existing photo when session is already ready', async () => {
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse(
        {
          photoId: 'p1',
          clientUploadId: 'dup',
          path: 'p1/original',
          alreadyReady: true,
          needsUpload: false,
          photo: { id: 'p1', fileName: 'a.jpg', fileUrl: '/display.jpg', createdAt: '2026-01-01T00:00:00Z' },
        },
        201,
      ),
    )
    vi.stubGlobal('fetch', fetchMock)
    const photo = await uploadPhotoWithRetry(
      new File([new Uint8Array([1])], 'a.jpg', { type: 'image/jpeg' }),
      'dup',
    )
    expect(photo.id).toBe('p1')
    expect(fetchMock).toHaveBeenCalledTimes(1)
  })
})

describe('runPhotoUploadSession', () => {
  it('uploads three jpegs after health succeeds', async () => {
    const uploaded: string[] = []
    await runPhotoUploadSession({
      items: [{ id: '1' }, { id: '2' }, { id: '3' }],
      waitForApi: async () => undefined,
      upload: async (item) => {
        uploaded.push(item.id)
      },
    })
    expect(uploaded.sort()).toEqual(['1', '2', '3'])
  })

  it('skips successful files and retries only failed ones', () => {
    const queue = [
      { id: 'ok', status: 'SUCCESS' },
      { id: 'bad', status: 'FAILED' },
      { id: 'wait', status: 'WAITING' },
    ]
    expect(pendingUploadItems(queue).map((item) => item.id)).toEqual(['bad', 'wait'])
    expect(failedUploadItems(queue).map((item) => item.id)).toEqual(['bad'])
  })

  it('shows gallery cta only when uploads finished with at least one success', () => {
    const mixed = [
      { id: 'ok', status: 'SUCCESS' },
      { id: 'bad', status: 'FAILED' },
    ]
    expect(canViewUploadedPhotos(mixed, false)).toBe(true)
    expect(canViewUploadedPhotos(mixed, true)).toBe(false)
    expect(canViewUploadedPhotos([{ id: 'a', status: 'SUCCESS' }], false)).toBe(true)
    expect(canViewUploadedPhotos([{ id: 'a', status: 'FAILED' }], false)).toBe(false)
    expect(canViewUploadedPhotos([{ id: 'a', status: 'UPLOADING' }], false)).toBe(false)
    expect(canViewUploadedPhotos([], false)).toBe(false)
  })
})

describe('ensureBackendReady', () => {
  it('skips health wait when backend was recently ready', async () => {
    markBackendReady(1_000)
    const fetchMock = vi.fn()
    vi.stubGlobal('fetch', fetchMock)

    const result = await ensureBackendReady({ now: () => 1_000 + 30_000 })

    expect(result).toBe('skipped')
    expect(fetchMock).not.toHaveBeenCalled()
  })

  it('does a short health check after the ready ttl expires', async () => {
    markBackendReady(1_000)
    const fetchMock = vi.fn().mockResolvedValue(jsonResponse({ status: 'ok' }))
    vi.stubGlobal('fetch', fetchMock)
    const sleep = vi.fn(async () => undefined)

    const result = await ensureBackendReady({
      now: () => 1_000 + BACKEND_READY_TTL_MS + 1,
      sleep,
    })

    expect(result).toBe('waited')
    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(String(fetchMock.mock.calls[0][0])).toContain('/api/health')
    expect(sleep).not.toHaveBeenCalled()
  })
})

describe('error kinds', () => {
  it('treats network and gateway errors as transient', () => {
    expect(isTransientUploadError(new ApiError(0, 'x', 'network'))).toBe(true)
    expect(isTransientUploadError(new ApiError(502, 'x'))).toBe(true)
    expect(isTransientUploadError(new ApiError(400, 'x'))).toBe(false)
    expect(isTransientUploadError(new ApiError(429, 'x'))).toBe(false)
  })
})
