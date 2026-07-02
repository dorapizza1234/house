import { useEffect, useRef, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'

type ChatMessage = { role: 'user' | 'assistant'; text: string }

const SUGGESTIONS = [
  '우리 가족 누구누구 있어?',
  '엄마 요즘 귀가 어때?',
]

function ChatPage() {
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const endRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    endRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, loading])

  const send = async (text: string) => {
    const trimmed = text.trim()
    if (!trimmed || loading) return
    setMessages((prev) => [...prev, { role: 'user', text: trimmed }])
    setInput('')
    setLoading(true)
    try {
      const res = await apiClient.post('/api/chat', { message: trimmed })
      setMessages((prev) => [...prev, { role: 'assistant', text: res.data.reply }])
    } catch {
      setMessages((prev) => [
        ...prev,
        { role: 'assistant', text: '오류가 발생했어요. 잠시 후 다시 시도해 주세요.' },
      ])
    } finally {
      setLoading(false)
    }
  }

  const onKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      send(input)
    }
  }

  return (
    <div className="min-h-screen flex flex-col max-w-md mx-auto bg-ivory">
      {/* 헤더 */}
      <header className="px-5 pt-8 pb-4 border-b border-borderlight flex items-center gap-3">
        <button onClick={() => navigate('/dashboard')} className="text-textsub text-xl">‹</button>
        <div>
          <h1 className="text-lg font-bold text-sage-dark">안부 도우미</h1>
          <p className="text-xs text-textsub">가족 안부를 물어보세요</p>
        </div>
      </header>

      {/* 대화 영역 */}
      <div className="flex-1 overflow-y-auto px-5 py-4 space-y-3">
        {messages.length === 0 && (
          <div className="mt-6 text-center text-sm text-textsub">
            <p className="mb-4">이렇게 물어보실 수 있어요 👇</p>
            <div className="flex flex-col gap-2 items-center">
              {SUGGESTIONS.map((s) => (
                <button
                  key={s}
                  onClick={() => send(s)}
                  className="px-4 py-2 bg-surface border border-borderlight rounded-full text-[13px] hover:border-sage transition"
                >
                  {s}
                </button>
              ))}
            </div>
          </div>
        )}

        {messages.map((m, i) => (
          <div key={i} className={m.role === 'user' ? 'flex justify-end' : 'flex justify-start'}>
            <div
              className={
                m.role === 'user'
                  ? 'max-w-[80%] px-4 py-2.5 rounded-2xl rounded-br-sm bg-sage text-white text-[15px] whitespace-pre-wrap'
                  : 'max-w-[80%] px-4 py-2.5 rounded-2xl rounded-bl-sm bg-surface border border-borderlight text-[15px] whitespace-pre-wrap'
              }
            >
              {m.text}
            </div>
          </div>
        ))}

        {loading && (
          <div className="flex justify-start">
            <div className="px-4 py-2.5 rounded-2xl rounded-bl-sm bg-surface border border-borderlight text-textsub text-sm">
              생각 중…
            </div>
          </div>
        )}
        <div ref={endRef} />
      </div>

      {/* 입력창 */}
      <div className="px-4 py-3 border-t border-borderlight flex gap-2">
        <input
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={onKeyDown}
          placeholder="가족 안부를 물어보세요"
          className="flex-1 px-4 py-3 bg-surface border border-borderlight rounded-full text-[15px] focus:border-sage transition"
        />
        <button
          onClick={() => send(input)}
          disabled={loading || !input.trim()}
          className="px-5 py-3 bg-sage hover:bg-sage-dark text-white rounded-full font-semibold transition disabled:opacity-50"
        >
          전송
        </button>
      </div>
    </div>
  )
}

export default ChatPage
