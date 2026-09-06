import { Link } from 'react-router-dom'
import { IconArrowLeft } from './Icons'

export function BackHome() {
  return (
    <Link className="back-home" to="/">
      <IconArrowLeft size={16} />
      Ana Sayfaya Dön
    </Link>
  )
}
