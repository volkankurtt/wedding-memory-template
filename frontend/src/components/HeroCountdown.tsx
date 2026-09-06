import { useCountdown } from '../hooks/useCountdown'
import { IconHeart } from './Icons'

function Unit({ value, label }: { value: number; label: string }) {
  return (
    <span className="count-inline__unit">
      <strong>{String(value).padStart(2, '0')}</strong>
      <small>{label}</small>
    </span>
  )
}

export function HeroCountdown() {
  const { status, days, hours, minutes, seconds } = useCountdown()

  if (status === 'today') {
    return (
      <p className="count-inline count-inline--msg">
        Bugün bizim günümüz
        <IconHeart className="count-inline__heart" size={16} />
      </p>
    )
  }

  if (status === 'past') {
    return (
      <p className="count-inline count-inline--msg">
        Bu güzel günü birlikte yaşadık
        <IconHeart className="count-inline__heart" size={16} />
      </p>
    )
  }

  return (
    <div className="count-inline" aria-live="polite">
      <Unit value={days} label="Gün" />
      <Unit value={hours} label="Saat" />
      <Unit value={minutes} label="Dakika" />
      <Unit value={seconds} label="Saniye" />
    </div>
  )
}
