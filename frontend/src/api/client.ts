import type { Memory, Photo } from '../types'
import { optimizeJpegForUpload } from '../utils/optimizeJpegForUpload'
import { markBackendReady } from './backendReadyState'

function nowMs() {
  return typeof performance !== 'undefined' ? performance.now() : Date.now()
}

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export const HEALTH_WAIT_MS = 90_000
export const HEALTH_RETRY_MS = 4_000
export const HEALTH_ATTEMPT_MS = 15_000

export type ApiErrorKind = 'http' | 'network' | 'timeout'

export class ApiError extends Error {
  readonly status: number
  readonly kind: ApiErrorKind

  constructor(status: number, message: string, kind: ApiErrorKind = status > 0 ? 'http' : 'network') {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.kind = kind
  }
}

function networkMessage() {
  return 'Sunucuya ulaşılamadı. Lütfen tekrar deneyin.'
}

function timeoutMessage() {
  return 'Sunucuya ulaşılamadı. Lütfen tekrar deneyin.'
}

async function readError(response: Response): Promise<string> {
  try {
    const body: unknown = await response.json()
    if (
      body &&
      typeof body === 'object' &&
      'error' in body &&
      typeof (body as { error: unknown }).error === 'string'
    ) {
      return (body as { error: string }).error
    }
  } catch {
    // fall through
  }
  return 'İstek tamamlanamadı. Lütfen tekrar deneyin.'
}

function isAbortError(error: unknown) {
  return (
    (error instanceof DOMException && error.name === 'AbortError') ||
    (error instanceof Error && error.name === 'AbortError')
  )
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, init)
  } catch (error) {
    if (init?.signal?.aborted || isAbortError(error)) {
      throw new ApiError(0, timeoutMessage(), 'timeout')
    }
    throw new ApiError(0, networkMessage(), 'network')
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response), 'http')
  }

  try {
    return (await response.json()) as T
  } catch {
    throw new ApiError(response.status, 'Sunucu yanıtı okunamadı. Lütfen tekrar deneyin.', 'http')
  }
}

export type PhotoPageResponse = {
  photos: Photo[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasNext: boolean
}

export const PHOTO_PAGE_SIZE = 30

function parsePhotoPage(data: unknown): PhotoPageResponse {
  if (Array.isArray(data)) {
    return {
      photos: data as Photo[],
      page: 0,
      size: data.length,
      totalElements: data.length,
      totalPages: 1,
      hasNext: false,
    }
  }
  if (data && typeof data === 'object') {
    const body = data as Record<string, unknown>
    const list = body.photos ?? body.content
    if (Array.isArray(list)) {
      return {
        photos: list as Photo[],
        page: typeof body.page === 'number' ? body.page : 0,
        size: typeof body.size === 'number' ? body.size : list.length,
        totalElements: typeof body.totalElements === 'number' ? body.totalElements : list.length,
        totalPages: typeof body.totalPages === 'number' ? body.totalPages : 1,
        hasNext: Boolean(body.hasNext),
      }
    }
  }
  throw new ApiError(0, 'Sunucu yanıtı okunamadı. Lütfen tekrar deneyin.', 'network')
}

export async function getPhotos(options?: { page?: number; size?: number; signal?: AbortSignal }) {
  const page = options?.page ?? 0
  const size = options?.size ?? PHOTO_PAGE_SIZE
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  const data = await request<unknown>(`/api/photos?${params}`, { signal: options?.signal })
  return parsePhotoPage(data)
}

export function uploadPhoto(file: File, uploadId?: string) {
  const body = new FormData()
  body.append('file', file)
  if (uploadId) body.append('uploadId', uploadId)
  if (import.meta.env.DEV) {
    console.info('[dugun-anisi] upload POST start', { uploadId, name: file.name, bytes: file.size })
  }
  return request<Photo>('/api/photos', { method: 'POST', body })
}

export type UploadSessionResponse = {
  photoId: string
  clientUploadId: string
  path: string
  signedUrl?: string | null
  token?: string | null
  expiresInSeconds: number
  alreadyReady: boolean
  needsUpload: boolean
  photo?: Photo | null
}

export type UploadPhotoPhase = 'session' | 'storage' | 'preparing' | 'ready'

export type UploadPhotoOptions = {
  onPhase?: (phase: UploadPhotoPhase) => void
}

function createUploadSession(file: File, uploadId: string) {
  return request<UploadSessionResponse>('/api/photos/upload-session', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({
      clientUploadId: uploadId,
      fileName: file.name,
      contentType: file.type || 'application/octet-stream',
      sizeBytes: file.size,
    }),
  })
}

function finalizePhoto(photoId: string, uploadId: string) {
  return request<Photo>(`/api/photos/${photoId}/finalize`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify({ clientUploadId: uploadId }),
  })
}

async function putOriginalToStorage(file: File, signedUrl: string, token: string) {
  const url = new URL(signedUrl)
  url.searchParams.set('token', token)
  const body = new FormData()
  body.append('cacheControl', '3600')
  body.append('', file)
  let response: Response
  try {
    response = await fetch(url.toString(), {
      method: 'PUT',
      headers: {
        'x-upsert': 'false',
      },
      body,
    })
  } catch (error) {
    if (isAbortError(error)) {
      throw new ApiError(0, timeoutMessage(), 'timeout')
    }
    throw new ApiError(0, networkMessage(), 'network')
  }
  if (response.ok || response.status === 409) return
  // Same object already present from a previous attempt.
  if (response.status === 400) {
    const message = await readError(response)
    if (/exist/i.test(message) || /already/i.test(message)) return
    throw new ApiError(response.status, 'Fotoğraf depolanamadı. Lütfen tekrar deneyin.', 'http')
  }
  if (response.status === 502 || response.status === 503 || response.status === 504) {
    throw new ApiError(response.status, await readError(response), 'http')
  }
  throw new ApiError(response.status, 'Fotoğraf depolanamadı. Lütfen tekrar deneyin.', 'http')
}

async function uploadPhotoDirect(file: File, uploadId: string, options?: UploadPhotoOptions) {
  const sessionStarted = nowMs()
  options?.onPhase?.('session')
  const session = await createUploadSession(file, uploadId)
  let sessionHost = ''
  try {
    sessionHost = session.signedUrl ? new URL(session.signedUrl).hostname : ''
  } catch {
    sessionHost = ''
  }
  console.info('[dugun-anisi] uploadSession', {
    uploadId,
    ms: Math.round(nowMs() - sessionStarted),
    host: sessionHost,
    needsUpload: session.needsUpload,
  })
  if (session.alreadyReady && session.photo) {
    options?.onPhase?.('ready')
    return session.photo
  }
  if (session.needsUpload) {
    if (!session.signedUrl || !session.token) {
      throw new ApiError(500, 'Yükleme adresi üretilemedi. Lütfen tekrar deneyin.', 'http')
    }
    options?.onPhase?.('storage')
    const storageStarted = nowMs()
    await putOriginalToStorage(file, session.signedUrl, session.token)
    const storageMs = Math.round(nowMs() - storageStarted)
    const seconds = storageMs / 1000
    let host = ''
    try {
      host = new URL(session.signedUrl).hostname
    } catch {
      host = ''
    }
    console.info('[dugun-anisi] directStorageUpload', {
      uploadId,
      host,
      bytes: file.size,
      optimizedFileSize: file.size,
      ms: storageMs,
      mbPerSec: seconds > 0 ? Number((file.size / 1048576 / seconds).toFixed(2)) : 0,
    })
  }
  options?.onPhase?.('preparing')
  const finalizeStarted = nowMs()
  const photo = await finalizePhoto(session.photoId, uploadId)
  if (import.meta.env.DEV) {
    console.info('[dugun-anisi] finalizeMs', {
      uploadId,
      ms: Math.round(nowMs() - finalizeStarted),
    })
  }
  options?.onPhase?.('ready')
  return photo
}

export function isTransientUploadError(error: unknown) {
  if (!(error instanceof ApiError)) return false
  if (error.kind === 'network' || error.kind === 'timeout') return true
  return error.status === 502 || error.status === 503 || error.status === 504
}

export function isRetryableHealthError(error: unknown) {
  if (!(error instanceof ApiError)) return true
  if (error.kind === 'network' || error.kind === 'timeout') return true
  return error.status === 500 || error.status === 502 || error.status === 503 || error.status === 504
}

export async function getHealth(signal?: AbortSignal) {
  return request<{ status: string }>('/api/health', { signal })
}

export type WaitForApiOptions = {
  timeoutMs?: number
  retryDelayMs?: number
  attemptTimeoutMs?: number
  now?: () => number
  sleep?: (ms: number) => Promise<void>
}

export async function waitForApi(options?: WaitForApiOptions) {
  const timeoutMs = options?.timeoutMs ?? HEALTH_WAIT_MS
  const retryDelayMs = options?.retryDelayMs ?? HEALTH_RETRY_MS
  const attemptTimeoutMs = options?.attemptTimeoutMs ?? HEALTH_ATTEMPT_MS
  const now = options?.now ?? Date.now
  const sleep = options?.sleep ?? ((ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms)))
  const deadline = now() + timeoutMs
  let lastError: unknown = new ApiError(0, networkMessage(), 'network')

  while (now() < deadline) {
    const remaining = deadline - now()
    const attemptMs = Math.min(attemptTimeoutMs, Math.max(1, remaining))
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), attemptMs)
    try {
      const health = await getHealth(controller.signal)
      if (health.status === 'ok' || health.status === 'UP') {
        markBackendReady(now())
        return
      }
      lastError = new ApiError(503, networkMessage(), 'http')
    } catch (error) {
      lastError = error
      if (!isRetryableHealthError(error)) throw error
    } finally {
      clearTimeout(timer)
    }
    const wait = Math.min(retryDelayMs, Math.max(0, deadline - now()))
    if (wait <= 0) break
    await sleep(wait)
  }

  if (lastError instanceof ApiError) {
    throw new ApiError(lastError.status, networkMessage(), lastError.kind)
  }
  throw new ApiError(0, networkMessage(), 'network')
}

export async function uploadPhotoWithRetry(file: File, uploadId: string, options?: UploadPhotoOptions) {
  const totalStarted = nowMs()
  const prepared = await optimizeJpegForUpload(file)
  console.info('[dugun-anisi] jpegOptimize', {
    uploadId,
    originalFileSize: prepared.originalFileSize,
    optimizedFileSize: prepared.optimizedFileSize,
    compressionMs: prepared.compressionMs,
    compressionRatio: prepared.originalFileSize
      ? prepared.optimizedFileSize / prepared.originalFileSize
      : 1,
    skipped: prepared.skipped,
  })

  const logTotal = () => {
    if (import.meta.env.DEV) {
      console.info('[dugun-anisi] totalMs', {
        uploadId,
        totalMs: Math.round(nowMs() - totalStarted),
      })
    }
  }

  try {
    const photo = await uploadPhotoDirect(prepared.file, uploadId, options)
    markBackendReady()
    logTotal()
    return photo
  } catch (error) {
    if (!isTransientUploadError(error)) throw error
    const photo = await uploadPhotoDirect(prepared.file, uploadId, options)
    markBackendReady()
    logTotal()
    return photo
  }
}

export function getMemories(signal?: AbortSignal) {
  return request<Memory[]>('/api/memories', { signal })
}

export function createMemory(data: { name: string; message: string }) {
  return request<Memory>('/api/memories', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(data),
  })
}

export function toUserMessage(error: unknown) {
  if (error instanceof ApiError) return error.message
  return networkMessage()
}
