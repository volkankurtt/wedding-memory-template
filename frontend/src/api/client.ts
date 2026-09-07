import type { Memory, Photo } from '../types'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number

  constructor(status: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.status = status
  }
}

function networkMessage() {
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

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, init)
  } catch (error) {
    if (init?.signal?.aborted || (error instanceof DOMException && error.name === 'AbortError')) {
      throw error
    }
    throw new ApiError(0, networkMessage())
  }

  if (!response.ok) {
    throw new ApiError(response.status, await readError(response))
  }

  try {
    return (await response.json()) as T
  } catch {
    throw new ApiError(response.status, 'Sunucu yanıtı okunamadı. Lütfen tekrar deneyin.')
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
  throw new ApiError(0, 'Sunucu yanıtı okunamadı. Lütfen tekrar deneyin.')
}

export async function getPhotos(options?: { page?: number; size?: number; signal?: AbortSignal }) {
  const page = options?.page ?? 0
  const size = options?.size ?? PHOTO_PAGE_SIZE
  const params = new URLSearchParams({ page: String(page), size: String(size) })
  const data = await request<unknown>(`/api/photos?${params}`, { signal: options?.signal })
  return parsePhotoPage(data)
}

export function uploadPhoto(file: File) {
  const body = new FormData()
  body.append('file', file)
  return request<Photo>('/api/photos', { method: 'POST', body })
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
