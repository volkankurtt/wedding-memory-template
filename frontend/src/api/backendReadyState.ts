export const BACKEND_READY_TTL_MS = 8 * 60 * 1000

let lastReadyAt: number | null = null

export function isBackendReady(now = Date.now()) {
  return lastReadyAt != null && now - lastReadyAt < BACKEND_READY_TTL_MS
}

export function markBackendReady(now = Date.now()) {
  lastReadyAt = now
}

export function lastBackendReadyAt() {
  return lastReadyAt
}

export function resetBackendReadyState() {
  lastReadyAt = null
}
