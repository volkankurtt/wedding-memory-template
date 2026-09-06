import { Link } from 'react-router-dom'
import { coupleNames, formatWeddingDate, wedding } from '../config/wedding'
import { HeroCountdown } from './HeroCountdown'
import { IconCamera, IconGallery, IconHeart, IconPin } from './Icons'

type Props = {
  onUpload: () => void
  onShare: () => void
}

export function HomeHero({ onUpload, onShare }: Props) {
  return (
    <section className="home-hero">
      <div className="home-hero__inner">
        <p className="home-hero__pair">
          <span className="home-hero__names">{coupleNames}</span>
          <IconHeart className="home-hero__heart" size={20} />
          <time className="home-hero__date" dateTime={wedding.weddingDate}>
            {formatWeddingDate('numeric')}
          </time>
        </p>

        <h1 className="home-hero__title">
          <span>BU ANI BİRLİKTE</span>
          <span>ÖLÜMSÜZLEŞTİRELİM</span>
        </h1>
        <p className="home-hero__text">{wedding.heroDescription}</p>

        <HeroCountdown />

        <div className="cta-row">
          <button className="btn btn--warm" type="button" onClick={onUpload}>
            <IconCamera className="btn-ico" size={18} />
            Fotoğraf Yükle
          </button>
          <Link className="btn btn--glass" to="/gallery">
            <IconGallery className="btn-ico" size={18} />
            Anıları Gör
          </Link>
        </div>

        <div className="home-venue">
          <p className="home-venue__name">{wedding.venueName}</p>
          <p className="home-venue__addr">{wedding.address}</p>
          <a className="btn btn--maps" href={wedding.googleMapsUrl} target="_blank" rel="noreferrer">
            <IconPin className="btn-ico" size={18} />
            Yol Tarifi Al
          </a>
        </div>

        <button className="btn btn--ghost-wide" type="button" onClick={onShare}>
          Davetiyeyi Paylaş
        </button>
      </div>
    </section>
  )
}
