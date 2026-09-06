import { useEffect, useState } from 'react'
import { wedding } from '../config/wedding'

export type CountdownStatus = 'upcoming' | 'today' | 'past'

export type CountdownState = {
  status: CountdownStatus
  days: number
  hours: number
  minutes: number
  seconds: number
}

function istanbulYmd(date: Date): string {
  return new Intl.DateTimeFormat('en-CA', {
    timeZone: wedding.timezone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(date)
}

function remainingParts(ms: number) {
  const total = Math.max(0, Math.floor(ms / 1000))
  const days = Math.floor(total / 86400)
  const hours = Math.floor((total % 86400) / 3600)
  const minutes = Math.floor((total % 3600) / 60)
  const seconds = total % 60
  return { days, hours, minutes, seconds }
}

export function weddingTargetDate() {
  return new Date(`${wedding.weddingDate}T${wedding.weddingTime}:00+03:00`)
}

export function getCountdownState(now: Date): CountdownState {
  const today = istanbulYmd(now)
  const target = weddingTargetDate()
  const remaining = target.getTime() - now.getTime()

  if (today > wedding.weddingDate) {
    return { status: 'past', days: 0, hours: 0, minutes: 0, seconds: 0 }
  }

  if (remaining <= 0) {
    return { status: 'today', days: 0, hours: 0, minutes: 0, seconds: 0 }
  }

  return { status: 'upcoming', ...remainingParts(remaining) }
}

export function useCountdown(): CountdownState {
  const [state, setState] = useState<CountdownState>(() => getCountdownState(new Date()))

  useEffect(() => {
    const id = window.setInterval(() => setState(getCountdownState(new Date())), 1000)
    return () => window.clearInterval(id)
  }, [])

  return state
}
