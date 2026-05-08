import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import BottomNav from '../components/BottomNav'

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

function CalendarPage() {
  const today = new Date()
  const [year, setYear] = useState(today.getFullYear())
  const [month, setMonth] = useState(today.getMonth() + 1)
  const [items, setItems] = useState<CalendarItem[]>([])
  const [showForm, setShowForm] = useState(false)
  const [type, setType] = useState('SCHEDULE')
  const [title, setTitle] = useState('')
  const [eventDate, setEventDate] = useState('')
  const [isYearly, setIsYearly] = useState(false)
  const [memo, setMemo] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const familyId = localStorage.getItem('familyId')

  const fetchCalendar = async () => {
    try {
      const response = await apiClient.get(
        `/api/families/${familyId}/calendar?year=${year}&month=${month}`
      )
      setItems(response.data.items)
    } catch (err: any) {
      setError(err.response?.data?.message || '캘린더 불러오기 실패')
    }
  }

  useEffect(() => {
    if (!familyId) {
      navigate('/family-invite')
      return
    }
    fetchCalendar()
  }, [year, month])

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await apiClient.post(`/api/families/${familyId}/events`, {
        type,
        title,
        eventDate,
        isYearly,
        memo,
      })
      setTitle('')
      setEventDate('')
      setMemo('')
      setIsYearly(false)
      setShowForm(false)
      await fetchCalendar()
    } catch (err: any) {
      setError(err.response?.data?.message || '이벤트 생성 실패')
    } finally {
      setLoading(false)
    }
  }

  const handleDelete = async (eventId: number) => {
    if (!confirm('정말 삭제할까요?')) return
    setError('')
    try {
      await apiClient.delete(`/api/families/${familyId}/events/${eventId}`)
      await fetchCalendar()
    } catch (err: any) {
      setError(err.response?.data?.message || '삭제 실패')
    }
  }

  const prevMonth = () => {
    if (month === 1) {
      setYear(year - 1)
      setMonth(12)
    } else {
      setMonth(month - 1)
    }
  }

  const nextMonth = () => {
    if (month === 12) {
      setYear(year + 1)
      setMonth(1)
    } else {
      setMonth(month + 1)
    }
  }

  return (
    <div className="min-h-screen pb-24">
      <header className="px-6 pt-10 pb-4 border-b border-borderlight bg-ivory max-w-md mx-auto">
        <h1 className="text-lg font-bold">가족 캘린더</h1>
      </header>

      <main className="px-6 py-6 space-y-6 max-w-md mx-auto">
        <div className="flex items-center justify-between">
          <button
            onClick={prevMonth}
            className="px-3 py-1 text-textsub hover:text-darkbrown"
          >
            ← 이전
          </button>
          <span className="text-base font-bold">{year}년 {month}월</span>
          <button
            onClick={nextMonth}
            className="px-3 py-1 text-textsub hover:text-darkbrown"
          >
            다음 →
          </button>
        </div>

        <section>
          <h3 className="text-xs font-bold text-textsub mb-3">이번 달 일정</h3>
          {items.length === 0 ? (
            <p className="text-sm text-textsub bg-surface rounded-xl p-4 text-center">
              일정이 없습니다
            </p>
          ) : (
            <ul className="space-y-2">
              {items.map((item, i) => (
                <li
                  key={i}
                  className="bg-surface rounded-2xl p-4 flex items-start justify-between gap-3"
                >
                  <div className="flex-1">
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
                      {item.isYearly && (
                        <span className="ml-1.5 text-textsub">· 매년</span>
                      )}
                    </div>
                    <div className="font-semibold text-sm">{item.title}</div>
                    {item.memo && (
                      <div className="text-xs text-textsub mt-1">{item.memo}</div>
                    )}
                  </div>
                  {item.id && (
                    <button
                      onClick={() => handleDelete(item.id!)}
                      className="text-xs text-terracotta hover:underline shrink-0"
                    >
                      삭제
                    </button>
                  )}
                </li>
              ))}
            </ul>
          )}
        </section>

        <section>
          {!showForm ? (
            <button
              onClick={() => setShowForm(true)}
              className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition"
            >
              + 새 일정 추가
            </button>
          ) : (
            <div className="bg-surface rounded-2xl p-5">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-base font-bold">새 일정 추가</h3>
                <button
                  onClick={() => setShowForm(false)}
                  className="text-xs text-textsub"
                >
                  취소
                </button>
              </div>
              <form onSubmit={handleCreate} className="space-y-3">
                <div>
                  <label className="block text-sm font-semibold mb-1.5">종류</label>
                  <select
                    value={type}
                    onChange={(e) => setType(e.target.value)}
                    className="w-full px-4 py-3 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                  >
                    <option value="SCHEDULE">일정</option>
                    <option value="ANNIVERSARY">기념일</option>
                    <option value="JESA">제사</option>
                  </select>
                </div>

                <div>
                  <label className="block text-sm font-semibold mb-1.5">제목</label>
                  <input
                    type="text"
                    value={title}
                    onChange={(e) => setTitle(e.target.value)}
                    className="w-full px-4 py-3 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                    required
                  />
                </div>

                <div>
                  <label className="block text-sm font-semibold mb-1.5">날짜</label>
                  <input
                    type="date"
                    value={eventDate}
                    onChange={(e) => setEventDate(e.target.value)}
                    className="w-full px-4 py-3 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                    required
                  />
                </div>

                <label className="flex items-center gap-2 text-sm">
                  <input
                    type="checkbox"
                    checked={isYearly}
                    onChange={(e) => setIsYearly(e.target.checked)}
                    className="accent-sage"
                  />
                  매년 반복
                </label>

                <div>
                  <label className="block text-sm font-semibold mb-1.5">메모 (선택)</label>
                  <textarea
                    value={memo}
                    onChange={(e) => setMemo(e.target.value)}
                    className="w-full px-4 py-3 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition resize-none"
                    rows={2}
                  />
                </div>

                <button
                  type="submit"
                  disabled={loading}
                  className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition disabled:opacity-50"
                >
                  {loading ? '추가 중...' : '추가하기'}
                </button>
              </form>
            </div>
          )}
        </section>

        {error && <p className="text-terracotta text-sm">{error}</p>}
      </main>

      <BottomNav />
    </div>
  )
}

export default CalendarPage
