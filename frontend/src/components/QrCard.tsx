import { QRCodeSVG } from 'qrcode.react'

type Props = {
  value: string
  caption: string
  hint?: string
}

export function QrCard({ value, caption, hint }: Props) {
  return (
    <figure className="qr-card">
      <div className="qr-card__frame">
        <QRCodeSVG value={value} size={200} level="M" includeMargin fgColor="#3d342c" bgColor="#ffffff" />
      </div>
      <figcaption>
        <p className="qr-card__caption">{caption}</p>
        {hint ? <p className="qr-card__hint">{hint}</p> : null}
      </figcaption>
    </figure>
  )
}
