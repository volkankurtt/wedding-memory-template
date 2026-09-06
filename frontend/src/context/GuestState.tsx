import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { getMemories, getPhotos, toUserMessage } from '../api/client'
import type { Memory, Photo } from '../types'

type GuestState = {
  photos: Photo[]
  memories: Memory[]
  photosLoading: boolean
  memoriesLoading: boolean
  loadError: string | null
  prependPhotos: (photos: Photo[]) => void
  prependMemory: (memory: Memory) => void
}

const GuestStateContext = createContext<GuestState | null>(null)

function mergeById<T extends { id: string }>(incoming: T[], current: T[]) {
  const seen = new Set(incoming.map((item) => item.id))
  return [...incoming, ...current.filter((item) => !seen.has(item.id))]
}

export function GuestStateProvider({ children }: { children: ReactNode }) {
  const [photos, setPhotos] = useState<Photo[]>([])
  const [memories, setMemories] = useState<Memory[]>([])
  const [photosLoading, setPhotosLoading] = useState(true)
  const [memoriesLoading, setMemoriesLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  useEffect(() => {
    const abort = new AbortController()

    async function load() {
      setLoadError(null)
      const [photoResult, memoryResult] = await Promise.allSettled([
        getPhotos(abort.signal),
        getMemories(abort.signal),
      ])

      if (abort.signal.aborted) return

      if (photoResult.status === 'fulfilled') {
        setPhotos(photoResult.value)
      } else if (!abort.signal.aborted) {
        setLoadError(toUserMessage(photoResult.reason))
      }

      if (memoryResult.status === 'fulfilled') {
        setMemories(memoryResult.value)
      } else if (!abort.signal.aborted) {
        setLoadError((current) => current ?? toUserMessage(memoryResult.reason))
      }

      setPhotosLoading(false)
      setMemoriesLoading(false)
    }

    void load()
    return () => abort.abort()
  }, [])

  const prependPhotos = useCallback((incoming: Photo[]) => {
    setPhotos((current) => mergeById(incoming, current))
  }, [])

  const prependMemory = useCallback((incoming: Memory) => {
    setMemories((current) => mergeById([incoming], current))
  }, [])

  const value = useMemo(
    () => ({
      photos,
      memories,
      photosLoading,
      memoriesLoading,
      loadError,
      prependPhotos,
      prependMemory,
    }),
    [photos, memories, photosLoading, memoriesLoading, loadError, prependPhotos, prependMemory],
  )

  return <GuestStateContext.Provider value={value}>{children}</GuestStateContext.Provider>
}

export function useGuestState() {
  const ctx = useContext(GuestStateContext)
  if (!ctx) throw new Error('useGuestState GuestStateProvider içinde kullanılmalı')
  return ctx
}
