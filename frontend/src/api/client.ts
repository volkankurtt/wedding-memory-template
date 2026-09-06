import type { Memory, Photo } from '../types'

const API_BASE = '/api'

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
    response = await fetch(`${API_BASE}${path}`, init)
  } catch {
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

export function getPhotos(signal?: AbortSignal) {
  return request<Photo[]>('/photos', { signal })
}

export function uploadPhoto(file: File) {
  const body = new FormData()
  body.append('file', file)
  return request<Photo>('/photos', { method: 'POST', body })
}

export function getMemories(signal?: AbortSignal) {
  return request<Memory[]>('/memories', { signal })
}

export function createMemory(data: { name: string; message: string }) {
  return request<Memory>('/memories', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
    body: JSON.stringify(data),
  })
}

export function toUserMessage(error: unknown) {
  if (error instanceof ApiError) return error.message
  return networkMessage()
}
