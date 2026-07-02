import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient, { setAccessToken } from '../api/client'
import BottomNav from '../components/BottomNav'

type DashboardMember = {
  memberId: number
  nickname: string
  presenceStatus: string
  presenceUpdatedAt: string
}

type CalendarItem = {
  id: number | null
  type: string
  title: string
  date: string
  isYearly: boolean
  memo: string | null
  creatorId: number | null
}

const TYPE_LABEL: Record<string, string> = {
  SCHEDULE: '일정',
  ANNIVERSARY: '기념일',
  JESA: '제사',
  BIRTHDAY: '생일',
}

function DashboardPage() {
  const [members, setMembers] = useState<DashboardMember[]>([])
  const [upcoming, setUpcoming] = useState<CalendarItem[]>([])
  const [inviteToken, setInviteToken] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const familyId = localStorage.getItem('familyId')

  const fetchDashboard = async () => {
    try {
      const response = await apiClient.get(`/api/families/${familyId}/dashboard`)
      setMembers(response.data)
    } catch (err: any) {
      setError(err.response?.data?.message || '대시보드 불러오기 실패')
    }
  }

  const fetchUpcoming = async () => {
    try {
      const today = new Date()
      const thisYear = today.getFullYear()
      const thisMonth = today.getMonth() + 1
      const nextDate = new Date(thisYear, thisMonth, 1) // next month
      const nextYear = nextDate.getFullYear()
      const nextMonth = nextDate.getMonth() + 1

      const [thisRes, nextRes] = await Promise.all([
        apiClient.get(`/api/families/${familyId}/calendar?year=${thisYear}&month=${thisMonth}`),
        apiClient.get(`/api/families/${familyId}/calendar?year=${nextYear}&month=${nextMonth}`),
      ])

      const all: CalendarItem[] = [...thisRes.data.items, ...nextRes.data.items]
      const todayStr = today.toISOString().split('T')[0]
      const futureOnly = all
        .filter((item) => item.date >= todayStr)
        .sort((a, b) => a.date.localeCompare(b.date))
        .slice(0, 3)
      setUpcoming(futureOnly)
    } catch {
      // 다가오는 일정 fetch 실패는 dashboard 흐름 막지 않음
    }
  }

  useEffect(() => {
    if (!familyId) {
      navigate('/family-invite')
      return
    }
    fetchDashboard()
    fetchUpcoming()
  }, [])

  const handleToggle = async () => {
    setError('')
    setLoading(true)
    try {
      await apiClient.post('/api/me/presence/toggle')
      await fetchDashboard()
    } catch (err: any) {
      setError(err.response?.data?.message || '토글 실패')
    } finally {
      setLoading(false)
    }
  }

  const handleCreateInvite = async () => {
    setError('')
    setLoading(true)
    try {
      const response = await apiClient.post(`/api/families/${familyId}/invites`)
      setInviteToken(response.data.token)
    } catch (err: any) {
      setError(err.response?.data?.message || '초대 생성 실패')
    } finally {
      setLoading(false)
    }
  }

  const handleLogout = async () => {
    try {
      await apiClient.post('/api/auth/logout')   // 서버: refresh 쿠키 만료 + Redis 삭제
    } catch { /* 무시 */ }
    setAccessToken(null)
    localStorage.clear()
    navigate('/login')
  }

  const copyToken = () => {
    navigator.clipboard?.writeText(inviteToken).catch(() => {})
  }

  const dDay = (dateStr: string): string => {
    const target = new Date(dateStr)
    const today = new Date()
    today.setHours(0, 0, 0, 0)
    const diff = Math.round((target.getTime() - today.getTime()) / (1000 * 60 * 60 * 24))
    if (diff === 0) return 'D-DAY'
    if (diff > 0) return `D-${diff}`
    return `D+${-diff}`
  }

  return (
    <div className="min-h-screen pb-24">
      <header className="px-6 pt-10 pb-4 border-b border-borderlight bg-ivory flex items-center justify-between max-w-md mx-auto">
        <h1 className="text-lg font-bold">가족 대시보드</h1>
        <button onClick={handleLogout} className="text-xs text-textsub hover:underline">
          로그아웃
        </button>
      </header>

      <main className="px-6 py-6 space-y-7 max-w-md mx-auto">
        <section className="bg-surface rounded-2xl p-5 shadow-sm">
          <p className="text-xs text-textsub mb-1">오늘은</p>
          <h2 className="text-base font-bold mb-4">지금 어디 계세요?</h2>
          <button
            onClick={handleToggle}
            disabled={loading}
            className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition disabled:opacity-50"
          >
            집 / 밖 전환
          </button>
        </section>

        <section>
          <h3 className="text-xs font-bold text-textsub mb-3">가족 구성원</h3>
          {members.length === 0 ? (
            <p className="text-sm text-textsub">아직 멤버가 없어요</p>
          ) : (
            <div className="flex gap-3 overflow-x-auto pb-1" style={{ scrollbarWidth: 'none' }}>
              {members.map((m) => (
                <div
                  key={m.memberId}
                  className="min-w-[88px] bg-surface rounded-2xl p-3.5 text-center shrink-0"
                >
                  <div className="w-12 h-12 rounded-full bg-ivory flex items-center justify-center text-base font-bold text-sage-dark mx-auto mb-2">
                    {m.nickname.slice(0, 1)}
                  </div>
                  <div className="text-xs font-semibold mb-1">{m.nickname}</div>
                  <div
                    className={`text-[10px] ${
                      m.presenceStatus === 'HOME' ? 'text-sage-dark font-semibold' : 'text-textsub'
                    }`}
                  >
                    {m.presenceStatus === 'HOME' ? '집' : '밖'}
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        <section>
          <div className="flex items-center justify-between mb-3">
            <h3 className="text-xs font-bold text-textsub">다가오는 일정</h3>
            <button
              onClick={() => navigate('/calendar')}
              className="text-xs text-sage-dark hover:underline"
            >
              전체 보기 →
            </button>
          </div>
          {upcoming.length === 0 ? (
            <p className="text-sm text-textsub bg-surface rounded-xl p-4 text-center">
              예정된 일정이 없어요
            </p>
          ) : (
            <ul className="space-y-2">
              {upcoming.map((item, i) => (
                <li
                  key={i}
                  className="bg-surface rounded-2xl p-4 flex items-center justify-between"
                >
                  <div>
                    <div className="text-xs text-textsub mb-1">
                      {item.date}
                      <span className="mx-1.5">·</span>
                      <span
                        className={
                          item.type === 'BIRTHDAY' ? 'text-terracotta' : 'text-sage-dark'
                        }
                      >
                        {TYPE_LABEL[item.type] ?? item.type}
                      </span>
                    </div>
                    <div className="font-semibold text-sm">{item.title}</div>
                  </div>
                  <span className="bg-sage text-white text-xs font-bold px-3 py-1.5 rounded-full">
                    {dDay(item.date)}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          <h3 className="text-xs font-bold text-textsub mb-3">가족 초대</h3>
          <button
            onClick={handleCreateInvite}
            disabled={loading}
            className="w-full py-3 bg-surface border border-borderlight rounded-xl font-semibold text-sm text-textsub hover:border-sage transition disabled:opacity-50"
          >
            {loading ? '생성 중...' : '초대 토큰 만들기'}
          </button>
          {inviteToken && (
            <div className="mt-3 bg-surface border border-borderlight rounded-xl p-3 space-y-2">
              <p className="text-xs text-textsub">24시간 동안 1회 사용 가능</p>
              <p className="text-[11px] break-all bg-ivory p-2 rounded font-mono">{inviteToken}</p>
              <button
                onClick={copyToken}
                className="w-full py-2 bg-sage hover:bg-sage-dark text-white rounded-lg text-xs font-semibold transition"
              >
                복사하기
              </button>
            </div>
          )}
        </section>

        {error && <p className="text-terracotta text-sm">{error}</p>}
      </main>

      <BottomNav />
    </div>
  )
}

export default DashboardPage
