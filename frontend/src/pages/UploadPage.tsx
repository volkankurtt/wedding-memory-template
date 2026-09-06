import { PhotoUpload } from '../components/PhotoUpload'
import { BackHome } from '../components/BackHome'

export function UploadPage() {
  return (
    <div className="subpage">
      <BackHome />
      <PhotoUpload />
    </div>
  )
}
