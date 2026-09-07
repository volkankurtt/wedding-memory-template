import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from 'react'
import { getMemories, getPhotos, PHOTO_PAGE_SIZE, toUserMessage } from '../api/client'
import { markBackendReady } from '../api/backendReadyState'
import type { Memory, Photo } from '../types'

type GuestState = {
  photos: Photo[]
  memories: Memory[]
  photosLoading: boolean
  photosLoadingMore: boolean
  photosHasNext: boolean
  photosHydrated: boolean
  photosPageError: string | null
  memoriesLoading: boolean
  loadError: string | null
  photosError: string | null
  prependPhotos: (photos: Photo[]) => void
  prependMemory: (memory: Memory) => void
  ensurePhotosLoaded: () => void
  loadMorePhotos: () => void
  retryPhotosPage: () => void
}

const GuestStateContext = createContext<GuestState | null>(null)

function mergeById<T extends { id: string }>(incoming: T[], current: T[]) {
  const seen = new Set(incoming.map((item) => item.id))
  return [...incoming, ...current.filter((item) => !seen.has(item.id))]
}

function appendUnique<T extends { id: string }>(current: T[], incoming: T[]) {
  const seen = new Set(current.map((item) => item.id))
  return [...current, ...incoming.filter((item) => !seen.has(item.id))]
}

export function GuestStateProvider({ children }: { children: ReactNode }) {
  const [photos, setPhotos] = useState<Photo[]>([])
  const [memories, setMemories] = useState<Memory[]>([])
  const [photosLoading, setPhotosLoading] = useState(false)
  const [photosLoadingMore, setPhotosLoadingMore] = useState(false)
  const [photosHasNext, setPhotosHasNext] = useState(true)
  const [photosHydrated, setPhotosHydrated] = useState(false)
  const [photosError, setPhotosError] = useState<string | null>(null)
  const [photosPageError, setPhotosPageError] = useState<string | null>(null)
  const [memoriesLoading, setMemoriesLoading] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)
  const nextPageRef = useRef(0)
  const hasNextRef = useRef(true)
  const inFlightPageRef = useRef<number | null>(null)

  useEffect(() => {
    const abort = new AbortController()

    async function loadMemories() {
      setLoadError(null)
      try {
        const items = await getMemories(abort.signal)
        if (abort.signal.aborted) return
        markBackendReady()
        setMemories(items)
      } catch (error) {
        if (!abort.signal.aborted) setLoadError(toUserMessage(error))
      } finally {
        if (!abort.signal.aborted) setMemoriesLoading(false)
      }
    }

    void loadMemories()
    return () => abort.abort()
  }, [])

  const loadPhotoPage = useCallback(async (page: number) => {
    if (inFlightPageRef.current != null) return
    if (page !== nextPageRef.current) return
    if (page > 0 && !hasNextRef.current) return

    inFlightPageRef.current = page
    if (page === 0) {
      setPhotosLoading(true)
      setPhotosError(null)
    } else {
      setPhotosLoadingMore(true)
      setPhotosPageError(null)
    }

    try {
      const result = await getPhotos({ page, size: PHOTO_PAGE_SIZE })
      const items = result.photos
      markBackendReady()
      setPhotos((current) => appendUnique(current, items))
      hasNextRef.current = result.hasNext
      nextPageRef.current = page + 1
      setPhotosHasNext(result.hasNext)
    } catch (error) {
      const message = toUserMessage(error)
      if (page === 0) setPhotosError(message)
      else setPhotosPageError(message)
    } finally {
      inFlightPageRef.current = null
      setPhotosLoading(false)
      setPhotosLoadingMore(false)
      setPhotosHydrated(true)
    }
  }, [])

  const ensurePhotosLoaded = useCallback(() => {
    if (photosHydrated || inFlightPageRef.current != null) return
    void loadPhotoPage(0)
  }, [loadPhotoPage, photosHydrated])

  const loadMorePhotos = useCallback(() => {
    if (!hasNextRef.current || photosPageError) return
    void loadPhotoPage(nextPageRef.current)
  }, [loadPhotoPage, photosPageError])

  const retryPhotosPage = useCallback(() => {
    void loadPhotoPage(nextPageRef.current)
  }, [loadPhotoPage])

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
      photosLoadingMore,
      photosHasNext,
      photosHydrated,
      photosPageError,
      memoriesLoading,
      loadError,
      photosError,
      prependPhotos,
      prependMemory,
      ensurePhotosLoaded,
      loadMorePhotos,
      retryPhotosPage,
    }),
    [
      photos,
      memories,
      photosLoading,
      photosLoadingMore,
      photosHasNext,
      photosHydrated,
      photosPageError,
      memoriesLoading,
      loadError,
      photosError,
      prependPhotos,
      prependMemory,
      ensurePhotosLoaded,
      loadMorePhotos,
      retryPhotosPage,
    ],
  )

  return <GuestStateContext.Provider value={value}>{children}</GuestStateContext.Provider>
}

export function useGuestState() {
  const ctx = useContext(GuestStateContext)
  if (!ctx) throw new Error('useGuestState GuestStateProvider içinde kullanılmalı')
  return ctx
}
