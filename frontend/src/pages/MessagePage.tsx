import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import BottomNav from '../components/BottomNav'

type Member = {
  memberId: number
  nickname: string
}

type Message = {
  id: number
  senderNickname: string
  receiverNickname: string
  messageTypeName: string | null
  content: string
  finalPoints: number
  multiplier: number
  eventReasons: string | null
  createdAt: string
}

const PRESETS = [
  { id: 1, label: '고마워요' },
  { id: 2, label: '사랑해요' },
  { id: 3, label: '좋은 하루' },
  { id: 4, label: '미안해요' },
  { id: 5, label: '힘내요' },
]

function MessagePage() {
  const [members, setMembers] = useState<Member[]>([])
  const [messages, setMessages] = useState<Message[]>([])
  const [receiverId, setReceiverId] = useState<number | ''>('')
  const [messageTypeId, setMessageTypeId] = useState<number | null>(null)
  const [content, setContent] = useState('')
  const [error, setError] = useState('')
  const [info, setInfo] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const familyId = localStorage.getItem('familyId')

  const fetchMembers = async () => {
    const response = await apiClient.get(`/api/families/${familyId}/dashboard`)
    setMembers(response.data)
  }

  const fetchMessages = async () => {
    const response = await apiClient.get(`/api/families/${familyId}/messages`)
    setMessages(response.data)
  }

  useEffect(() => {
    if (!familyId) {
      navigate('/family-invite')
      return
    }
    fetchMembers()
    fetchMessages()
  }, [])

  const handleSend = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setInfo('')
    setLoading(true)
    try {
      const response = await apiClient.post('/api/messages', {
        receiverId,
        messageTypeId,
        content,
      })
      setInfo(
        `+${response.data.finalPoints}점 (×${response.data.multiplier})${
          response.data.eventReasons ? ' · ' + response.data.eventReasons : ''
        } · 오늘 프리셋 ${response.data.todayPresetCount}/3`
      )
      setContent('')
      setMessageTypeId(null)
      await fetchMessages()
    } catch (err: any) {
      setError(err.response?.data?.message || '전송 실패')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen pb-24">
      <header className="px-6 pt-10 pb-4 border-b border-borderlight bg-ivory max-w-md mx-auto">
        <h1 className="text-lg font-bold">가족 메시지</h1>
      </header>

      <main className="px-6 py-6 space-y-7 max-w-md mx-auto">
        <section className="bg-surface rounded-2xl p-5">
          <h3 className="text-base font-bold mb-4">메시지 보내기</h3>

          <form onSubmit={handleSend} className="space-y-4">
            <div>
              <label className="block text-xs font-bold text-textsub mb-2">받는 사람</label>
              <div className="flex gap-2 overflow-x-auto pb-1" style={{ scrollbarWidth: 'none' }}>
                {members.map((m) => (
                  <button
                    type="button"
                    key={m.memberId}
                    onClick={() => setReceiverId(m.memberId)}
                    className={`shrink-0 px-4 py-2 rounded-full text-xs font-semibold border transition ${
                      receiverId === m.memberId
                        ? 'bg-sage text-white border-sage'
                        : 'bg-surface text-textsub border-borderlight'
                    }`}
                  >
                    {m.nickname}
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-textsub mb-2">프리셋 (10점)</label>
              <div className="grid grid-cols-2 gap-2">
                {PRESETS.map((p) => (
                  <button
                    type="button"
                    key={p.id}
                    onClick={() => {
                      setMessageTypeId(p.id)
                      setContent(p.label)
                    }}
                    className={`py-3 rounded-xl text-sm font-semibold border transition ${
                      messageTypeId === p.id
                        ? 'bg-sage text-white border-sage'
                        : 'bg-ivory text-darkbrown border-borderlight hover:border-sage'
                    }`}
                  >
                    {p.label}
                  </button>
                ))}
                <button
                  type="button"
                  onClick={() => {
                    setMessageTypeId(null)
                    setContent('')
                  }}
                  className={`py-3 rounded-xl text-sm font-semibold border transition col-span-2 ${
                    messageTypeId === null
                      ? 'bg-sage text-white border-sage'
                      : 'bg-ivory text-textsub border-borderlight hover:border-sage'
                  }`}
                >
                  자유입력 (0점)
                </button>
              </div>
            </div>

            <div>
              <label className="block text-xs font-bold text-textsub mb-2">내용</label>
              <textarea
                value={content}
                onChange={(e) => setContent(e.target.value)}
                className="w-full px-4 py-3 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition resize-none"
                rows={3}
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading || !receiverId}
              className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition disabled:opacity-50"
            >
              {loading ? '전송 중...' : '보내기'}
            </button>

            {info && (
              <p className="text-sm text-sage-dark bg-[#E8F0E1] rounded-xl px-3 py-2">
                {info}
              </p>
            )}
          </form>
        </section>

        <section>
          <h3 className="text-xs font-bold text-textsub mb-3">메시지함</h3>
          {messages.length === 0 ? (
            <p className="text-sm text-textsub bg-surface rounded-xl p-4 text-center">
              메시지가 없습니다
            </p>
          ) : (
            <ul className="space-y-2">
              {messages.map((m) => (
                <li key={m.id} className="bg-surface rounded-2xl p-4">
                  <div className="flex justify-between items-baseline mb-2">
                    <p className="text-xs text-textsub">
                      <span className="font-semibold text-darkbrown">{m.senderNickname}</span>
                      <span className="mx-1.5">→</span>
                      {m.receiverNickname}
                    </p>
                    <p className="text-[10px] text-textsub">
                      {new Date(m.createdAt).toLocaleString()}
                    </p>
                  </div>
                  <p className="text-sm">{m.content}</p>
                  <div className="text-[11px] text-textsub mt-2">
                    {m.messageTypeName ?? '자유입력'} · +{m.finalPoints}점
                    {m.multiplier > 1 && (
                      <span className="text-terracotta font-semibold"> (×{m.multiplier})</span>
                    )}
                    {m.eventReasons && ` · ${m.eventReasons}`}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>

        {error && <p className="text-terracotta text-sm">{error}</p>}
      </main>

      <BottomNav />
    </div>
  )
}

export default MessagePage
