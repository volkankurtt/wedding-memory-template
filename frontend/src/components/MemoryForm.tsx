import { useState, type FormEvent } from 'react'
import { createMemory, toUserMessage } from '../api/client'
import { MEMORY_LIMITS } from '../config/limits'
import { IconHeart } from './Icons'

type Props = {
  onDone?: () => void
}

export function MemoryForm({ onDone }: Props) {
  const [name, setName] = useState('')
  const [message, setMessage] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [ok, setOk] = useState(false)
  const [busy, setBusy] = useState(false)

  const canSubmit = message.trim().length > 0 && message.length <= MEMORY_LIMITS.maxChars && !busy

  async function onSubmit(event: FormEvent) {
    event.preventDefault()
    setOk(false)
    const trimmedMessage = message.trim()
    if (!trimmedMessage) {
      setError('Lütfen bir anı veya mesaj yazın.')
      return
    }
    if (trimmedMessage.length > MEMORY_LIMITS.maxChars) {
      setError(`Anı en fazla ${MEMORY_LIMITS.maxChars} karakter olabilir.`)
      return
    }

    setBusy(true)
    setError(null)
    try {
      await createMemory({
        name: name.trim(),
        message: trimmedMessage,
      })
      setName('')
      setMessage('')
      setOk(true)
      window.setTimeout(() => onDone?.(), 1400)
    } catch (cause) {
      setError(toUserMessage(cause))
    } finally {
      setBusy(false)
    }
  }

  return (
    <form className="memory-form" onSubmit={onSubmit}>
      <p className="memory-form__heart">
        <IconHeart size={20} />
      </p>
      <h2 className="memory-form__title">Bir not bırakır mısınız?</h2>
      <p className="memory-form__lead">
        Fotoğraflar günü gösterir, yazdıklarınız o günü hatırlatır. İki satır bile olsa, yıllar sonra
        en çok bunlar okunacak.
      </p>
      <p className="memory-form__prompt">Bu çifte tek bir cümle söyleseniz, ne söylerdiniz?</p>
      <textarea
        name="message"
        rows={6}
        maxLength={MEMORY_LIMITS.maxChars}
        value={message}
        placeholder="Buraya yazın..."
        disabled={busy}
        onChange={(event) => {
          setMessage(event.target.value.slice(0, MEMORY_LIMITS.maxChars))
          setOk(false)
          setError(null)
        }}
      />
      <p className="char-count">
        {message.length} / {MEMORY_LIMITS.maxChars}
      </p>
      <input
        type="text"
        name="name"
        autoComplete="name"
        maxLength={80}
        value={name}
        placeholder="Adınız (isteğe bağlı)"
        disabled={busy}
        onChange={(event) => setName(event.target.value)}
      />
      <button className="btn btn--cream" type="submit" disabled={!canSubmit}>
        {busy ? 'Gönderiliyor…' : 'Deftere Yaz'}
      </button>
      {error ? <p className="form-error">{error}</p> : null}
      {ok ? (
        <p className="form-note form-note--icon">
          Notunuz çiftimize ulaştı. Güzel dilekleriniz için teşekkür ederiz.
          <IconHeart size={14} />
        </p>
      ) : null}
    </form>
  )
}
