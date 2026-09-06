import { useCallback, useState } from 'react'
import { coupleNames, formatWeddingDate } from '../config/wedding'

function siteUrl() {
  return window.location.origin
}

export function useShare() {
  const [copied, setCopied] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const copyLink = useCallback(async () => {
    const url = siteUrl()
    try {
      await navigator.clipboard.writeText(url)
      setCopied(true)
      setMessage('Bağlantı kopyalandı')
    } catch {
      setMessage('Kopyalanamadı. Adresi elle seçebilirsiniz.')
    }
    window.setTimeout(() => {
      setCopied(false)
      setMessage(null)
    }, 2400)
  }, [])

  const shareInvite = useCallback(async () => {
    const url = siteUrl()
    const text = `${coupleNames} düğününe davetlisiniz — ${formatWeddingDate('numeric')}`

    if (typeof navigator.share === 'function') {
      try {
        await navigator.share({ title: `${coupleNames} · Düğün`, text, url })
        return
      } catch (error) {
        if (error instanceof DOMException && error.name === 'AbortError') return
      }
    }

    await copyLink()
  }, [copyLink])

  return { copied, message, copyLink, shareInvite, siteUrl }
}
