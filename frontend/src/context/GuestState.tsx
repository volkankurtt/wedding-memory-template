import {
  createContext,
  useCallback,
  useContext,
  useMemo,
  useState,
  type ReactNode,
} from 'react'
import { initialMemories } from '../mock/memories'
import { initialPhotos } from '../mock/photos'
import type { Memory, Photo } from '../types'

type GuestState = {
  photos: Photo[]
  memories: Memory[]
  addPhotos: (photos: Photo[]) => void
  addMemory: (memory: Omit<Memory, 'id' | 'createdAt'>) => void
}

const GuestStateContext = createContext<GuestState | null>(null)

export function GuestStateProvider({ children }: { children: ReactNode }) {
  const [photos, setPhotos] = useState<Photo[]>(initialPhotos)
  const [memories, setMemories] = useState<Memory[]>(initialMemories)

  const addPhotos = useCallback((incoming: Photo[]) => {
    setPhotos((current) => [...incoming, ...current])
  }, [])

  const addMemory = useCallback((incoming: Omit<Memory, 'id' | 'createdAt'>) => {
    setMemories((current) => [
      {
        id: crypto.randomUUID(),
        createdAt: new Date().toISOString(),
        ...incoming,
      },
      ...current,
    ])
  }, [])

  const value = useMemo(
    () => ({ photos, memories, addPhotos, addMemory }),
    [photos, memories, addPhotos, addMemory],
  )

  return <GuestStateContext.Provider value={value}>{children}</GuestStateContext.Provider>
}

export function useGuestState() {
  const ctx = useContext(GuestStateContext)
  if (!ctx) throw new Error('useGuestState GuestStateProvider içinde kullanılmalı')
  return ctx
}
