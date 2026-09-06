import { useState } from 'react'
import { Dialog } from '../components/Dialog'
import { HomeHero } from '../components/HomeHero'
import { MemoryBook } from '../components/MemoryBook'
import { MemoryForm } from '../components/MemoryForm'
import { PhotoUpload } from '../components/PhotoUpload'
import { SharePanel } from '../components/SharePanel'

type Modal = 'upload' | 'share' | 'memory' | null

export function HomePage() {
  const [modal, setModal] = useState<Modal>(null)

  return (
    <>
      <HomeHero onUpload={() => setModal('upload')} onShare={() => setModal('share')} />
      <MemoryBook onWrite={() => setModal('memory')} />

      {modal === 'upload' ? (
        <Dialog title="Fotoğraflarınızı Bizimle Paylaşın" hideTitle onClose={() => setModal(null)}>
          <PhotoUpload onCancel={() => setModal(null)} onDone={() => setModal(null)} />
        </Dialog>
      ) : null}

      {modal === 'share' ? (
        <Dialog title="Davetiyeyi Paylaş" sheet onClose={() => setModal(null)}>
          <SharePanel />
        </Dialog>
      ) : null}

      {modal === 'memory' ? (
        <Dialog title="Bir not bırakır mısınız?" hideTitle onClose={() => setModal(null)}>
          <MemoryForm onDone={() => setModal(null)} />
        </Dialog>
      ) : null}
    </>
  )
}
