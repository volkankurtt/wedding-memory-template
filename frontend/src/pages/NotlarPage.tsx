import { BackHome } from '../components/BackHome'
import { MemoryBook } from '../components/MemoryBook'

export function NotlarPage() {
  return (
    <div className="subpage">
      <BackHome />
      <MemoryBook showList />
    </div>
  )
}
