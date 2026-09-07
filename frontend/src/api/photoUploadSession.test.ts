import { afterEach, describe, expect, it, vi } from 'vitest'
import { ApiError, isTransientUploadError, uploadPhotoWithRetry, waitForApi } from './client'
import { failedUploadItems, pendingUploadItems, runPhotoUploadSession } from './photoUploadSession'

afterEach(() => {
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
    const fetchMock = vi.fn().mockResolvedValue(
      jsonResponse({ id: '1', fileName: 'a.jpg', fileUrl: '/display.jpg', createdAt: '2026-01-01T00:00:00Z' }, 201),
    )
    vi.stubGlobal('fetch', fetchMock)

    await uploadPhotoWithRetry(new File([new Uint8Array([1, 2, 3])], 'a.jpg', { type: 'image/jpeg' }), 'upload-1')

    expect(fetchMock).toHaveBeenCalledTimes(1)
    const init = fetchMock.mock.calls[0][1] as RequestInit
    expect(init.method).toBe('POST')
    const body = init.body as FormData
    expect(body.get('uploadId')).toBe('upload-1')
  })

  it('retries a transient 502 once with the same upload id', async () => {
    const fetchMock = vi
      .fn()
      .mockResolvedValueOnce(jsonResponse({ error: 'bad gateway' }, 502))
      .mockResolvedValueOnce(
        jsonResponse({ id: '1', fileName: 'a.jpg', fileUrl: '/display.jpg', createdAt: '2026-01-01T00:00:00Z' }, 201),
      )
    vi.stubGlobal('fetch', fetchMock)

    await uploadPhotoWithRetry(new File([new Uint8Array([1])], 'a.jpg', { type: 'image/jpeg' }), 'same-id')

    expect(fetchMock).toHaveBeenCalledTimes(2)
    const first = fetchMock.mock.calls[0][1] as RequestInit
    const second = fetchMock.mock.calls[1][1] as RequestInit
    expect((first.body as FormData).get('uploadId')).toBe('same-id')
    expect((second.body as FormData).get('uploadId')).toBe('same-id')
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
})

describe('error kinds', () => {
  it('treats network and gateway errors as transient', () => {
    expect(isTransientUploadError(new ApiError(0, 'x', 'network'))).toBe(true)
    expect(isTransientUploadError(new ApiError(502, 'x'))).toBe(true)
    expect(isTransientUploadError(new ApiError(400, 'x'))).toBe(false)
    expect(isTransientUploadError(new ApiError(429, 'x'))).toBe(false)
  })
})
