import { QrCard } from './QrCard'
import { useShare } from '../hooks/useShare'

export function SharePanel() {
  const { copied, message, copyLink, shareInvite, siteUrl } = useShare()

  return (
    <div className="share-panel">
      <QrCard value={siteUrl()} caption="Davetiyeyi okutun" hint="Ana sayfa bağlantısı" />
      <button className="btn btn--warm" type="button" onClick={() => void shareInvite()}>
        Davetiyeyi Paylaş
      </button>
      <button className="btn btn--outline" type="button" onClick={() => void copyLink()}>
        {copied ? 'Kopyalandı' : 'Linki Kopyala'}
      </button>
      {message ? <p className="form-note">{message}</p> : null}
      <p className="invite-hint">WhatsApp, Instagram veya Mesajlar ile paylaşabilirsiniz.</p>
    </div>
  )
}
