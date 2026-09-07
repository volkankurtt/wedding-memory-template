import { waitForApi, type WaitForApiOptions } from './client'
import {
  BACKEND_READY_TTL_MS,
  isBackendReady,
  lastBackendReadyAt,
  markBackendReady,
  resetBackendReadyState,
} from './backendReadyState'

export { BACKEND_READY_TTL_MS, isBackendReady, markBackendReady, resetBackendReadyState }

let inFlight: Promise<void> | null = null

function devLog(message: string, details?: Record<string, unknown>) {
  if (!import.meta.env.DEV) return
  if (details) {
    console.info(`[dugun-anisi] ${message}`, details)
    return
  }
  console.info(`[dugun-anisi] ${message}`)
}

export function resetBackendReady() {
  inFlight = null
  resetBackendReadyState()
}

export type EnsureBackendReadyResult = 'skipped' | 'waited'

export async function ensureBackendReady(
  options?: WaitForApiOptions & { now?: () => number },
): Promise<EnsureBackendReadyResult> {
  const now = options?.now ?? Date.now
  if (isBackendReady(now())) {
    devLog('backend ready, skip health wait', { ageMs: now() - (lastBackendReadyAt() ?? now()) })
    return 'skipped'
  }
  if (inFlight) {
    devLog('join in-flight health wait')
    await inFlight
    return 'waited'
  }

  const started = now()
  devLog('health wait start')
  const run = waitForApi(options)
    .then(() => {
      markBackendReady(now())
      devLog('health wait success', { totalWarmupMs: now() - started })
    })
    .finally(() => {
      if (inFlight === run) inFlight = null
    })
  inFlight = run
  await run
  return 'waited'
}

export function warmupBackendInBackground() {
  const started = Date.now()
  devLog('background health start')
  void ensureBackendReady()
    .then((result) => {
      devLog('background health done', { result, totalWarmupMs: Date.now() - started })
    })
    .catch((error) => {
      devLog('background health failed', {
        totalWarmupMs: Date.now() - started,
        status: error instanceof Error ? error.message : 'error',
      })
    })
}
