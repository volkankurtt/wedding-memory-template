import { useGuestState } from '../context/GuestState'
import { wedding } from '../config/wedding'

function formatWhen(iso: string) {
  return new Intl.DateTimeFormat('tr-TR', {
    day: 'numeric',
    month: 'long',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(iso))
}

type Props = {
  onWrite: () => void
}

export function MemoryBook({ onWrite }: Props) {
  const { memories, memoriesLoading, loadError } = useGuestState()

  return (
    <section className="memory-band" id="memories">
      <p className="eyebrow">Anı Defteri</p>
      <h2 className="memory-band__title">Misafir notları</h2>
      <p className="memory-band__lead">{wedding.memoryLead}</p>
      <button className="btn btn--warm" type="button" onClick={onWrite}>
        Anı Bırak
      </button>
      {loadError ? <p className="form-error">{loadError}</p> : null}
      {memoriesLoading && memories.length === 0 ? (
        <p className="memory-band__empty">Notlar yükleniyor…</p>
      ) : memories.length === 0 ? (
        <p className="memory-band__empty">{wedding.memoriesEmpty}</p>
      ) : (
        <ul className="memory-notes">
          {memories.map((memory) => (
            <li className="memory-card" key={memory.id}>
              <p className="memory-card__text">{memory.message}</p>
              <p className="memory-card__meta">
                <span>{memory.name || 'Anonim'}</span>
                <time dateTime={memory.createdAt}>{formatWhen(memory.createdAt)}</time>
              </p>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
