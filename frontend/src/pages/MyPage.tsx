import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import BottomNav from '../components/BottomNav'

type CheckInLog = {
  id: number
  memberNickname: string
  status: string
  checkedAt: string
}

type HourPrediction = {
  averageHour: number | null
  sampleCount: number
  days: number
}

type TodayComparison = {
  todayReturnHour: number | null
  averageReturnHour: number
  diffMinutes: number | null
  isLate: boolean
  hasComparison: boolean
  days: number
}

const formatHour = (h: number | null): string => {
  if (h === null) return '데이터 없음'
  const hour = Math.floor(h)
  const minute = Math.round((h - hour) * 60)
  return `${hour}시 ${minute.toString().padStart(2, '0')}분`
}

function MyPage() {
  const [logs, setLogs] = useState<CheckInLog[]>([])
  const [leave, setLeave] = useState<HourPrediction | null>(null)
  const [ret, setRet] = useState<HourPrediction | null>(null)
  const [today, setToday] = useState<TodayComparison | null>(null)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  useEffect(() => {
    const token = localStorage.getItem('accessToken')
    if (!token) {
      navigate('/login')
      return
    }
    fetchAll()
  }, [])

  const fetchAll = async () => {
    try {
      const [logsRes, leaveRes, retRes, todayRes] = await Promise.all([
        apiClient.get('/api/me/check-in-logs?days=30'),
        apiClient.get('/api/me/predictions/leave-time?days=14'),
        apiClient.get('/api/me/predictions/return-time?days=14'),
        apiClient.get('/api/me/predictions/today-comparison?days=14'),
      ])
      setLogs(logsRes.data)
      setLeave(leaveRes.data)
      setRet(retRes.data)
      setToday(todayRes.data)
    } catch (err: any) {
      setError(err.response?.data?.message || '불러오기 실패')
    }
  }

  const handleLogout = () => {
    localStorage.clear()
    navigate('/login')
  }

  return (
    <div className="min-h-screen pb-24">
      <header className="px-6 pt-10 pb-4 border-b border-borderlight bg-ivory max-w-md mx-auto">
        <h1 className="text-lg font-bold">마이페이지</h1>
      </header>

      <main className="px-6 py-6 space-y-7 max-w-md mx-auto">
        <section>
          <h3 className="text-xs font-bold text-textsub mb-3">생활 패턴 (최근 14일)</h3>
          <div className="grid grid-cols-2 gap-2">
            <div className="bg-surface rounded-2xl p-4 text-center">
              <p className="text-[11px] text-textsub mb-1.5">외출 평균</p>
              <p className="text-lg font-bold text-sage-dark">
                {leave ? formatHour(leave.averageHour) : '...'}
              </p>
              {leave && (
                <p className="text-[10px] text-textsub mt-1">{leave.sampleCount}회 기준</p>
              )}
            </div>
            <div className="bg-surface rounded-2xl p-4 text-center">
              <p className="text-[11px] text-textsub mb-1.5">귀가 평균</p>
              <p className="text-lg font-bold text-sage-dark">
                {ret ? formatHour(ret.averageHour) : '...'}
              </p>
              {ret && (
                <p className="text-[10px] text-textsub mt-1">{ret.sampleCount}회 기준</p>
              )}
            </div>
          </div>
        </section>

        <section>
          <h3 className="text-xs font-bold text-textsub mb-3">오늘 비교</h3>
          <div className="bg-surface rounded-2xl p-4 text-sm">
            {today === null && <p className="text-textsub">...</p>}
            {today && !today.hasComparison && (
              <p className="text-textsub">비교할 데이터가 부족해요</p>
            )}
            {today && today.hasComparison && (
              <div className="space-y-1.5">
                <div className="flex justify-between">
                  <span className="text-textsub">오늘 첫 귀가</span>
                  <span className="font-semibold">{formatHour(today.todayReturnHour)}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-textsub">평균</span>
                  <span className="font-semibold">{formatHour(today.averageReturnHour)}</span>
                </div>
                <div
                  className={`text-center pt-2 mt-2 border-t border-borderlight font-semibold ${
                    today.isLate ? 'text-terracotta' : 'text-sage-dark'
                  }`}
                >
                  {today.diffMinutes !== null && today.diffMinutes > 0
                    ? `평소보다 ${today.diffMinutes}분 늦었어요`
                    : today.diffMinutes !== null && today.diffMinutes < 0
                    ? `평소보다 ${-today.diffMinutes}분 빨랐어요`
                    : '평소와 동일'}
                </div>
              </div>
            )}
          </div>
        </section>

        <section>
          <h3 className="text-xs font-bold text-textsub mb-3">체크인 이력 (30일)</h3>
          {logs.length === 0 ? (
            <p className="text-sm text-textsub bg-surface rounded-xl p-4 text-center">
              기록이 없습니다
            </p>
          ) : (
            <div className="bg-surface rounded-2xl divide-y divide-borderlight">
              {logs.map((l) => (
                <div
                  key={l.id}
                  className="px-4 py-3 flex items-center justify-between text-sm"
                >
                  <span
                    className={`font-semibold ${
                      l.status === 'HOME' ? 'text-sage-dark' : 'text-textsub'
                    }`}
                  >
                    {l.status === 'HOME' ? '집' : '밖'}
                  </span>
                  <span className="text-xs text-textsub">
                    {new Date(l.checkedAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          )}
        </section>

        <button
          onClick={handleLogout}
          className="w-full py-3 bg-surface border border-borderlight text-terracotta rounded-xl font-semibold hover:border-terracotta transition"
        >
          로그아웃
        </button>

        {error && <p className="text-terracotta text-sm">{error}</p>}
      </main>

      <BottomNav />
    </div>
  )
}

export default MyPage
