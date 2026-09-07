export const UPLOAD_CONCURRENCY = 3

export function pendingUploadItems<T extends { status: string }>(items: T[]) {
  return items.filter((item) => item.status !== 'SUCCESS')
}

export function failedUploadItems<T extends { status: string }>(items: T[]) {
  return items.filter((item) => item.status === 'FAILED')
}

export async function runPool<T>(items: T[], worker: (item: T) => Promise<void>, concurrency = UPLOAD_CONCURRENCY) {
  let next = 0
  const runners = Array.from({ length: Math.min(concurrency, items.length) }, async () => {
    while (next < items.length) {
      const current = next
      next += 1
      await worker(items[current])
    }
  })
  await Promise.all(runners)
}

export async function runPhotoUploadSession<T>(options: {
  items: T[]
  waitForApi: () => Promise<void>
  upload: (item: T) => Promise<void>
  concurrency?: number
}) {
  await options.waitForApi()
  await runPool(options.items, options.upload, options.concurrency ?? UPLOAD_CONCURRENCY)
}
