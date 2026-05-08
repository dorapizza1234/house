import { useNavigate, useLocation } from 'react-router-dom'

const TABS = [
  { label: '집', path: '/dashboard' },
  { label: '캘린더', path: '/calendar' },
  { label: '메시지', path: '/message' },
  { label: '식물', path: '/plant' },
  { label: '마이', path: '/mypage' },
]

function BottomNav() {
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <nav className="fixed bottom-0 inset-x-0 bg-surface border-t border-borderlight grid grid-cols-5 max-w-md mx-auto pb-2 z-10">
      {TABS.map((tab) => {
        const active = location.pathname === tab.path
        return (
          <button
            key={tab.path}
            onClick={() => navigate(tab.path)}
            className={`py-3.5 text-xs font-semibold transition ${
              active ? 'text-sage-dark' : 'text-textsub'
            }`}
          >
            {tab.label}
          </button>
        )
      })}
    </nav>
  )
}

export default BottomNav
