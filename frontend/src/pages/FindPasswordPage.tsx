import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'

function FindPasswordPage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await apiClient.post('/api/auth/password/reset-request', { email })
      setSent(true)
    } catch (err: any) {
      setError(err.response?.data?.message || '요청 실패')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <div className="flex items-center mb-8">
          <button
            onClick={() => navigate('/login')}
            className="text-2xl text-darkbrown mr-3"
            aria-label="뒤로"
          >
            ←
          </button>
          <h1 className="text-lg font-bold">비밀번호 찾기</h1>
        </div>

        {!sent ? (
          <>
            <p className="text-sm text-textsub mb-6">
              가입한 이메일을 입력하시면 비밀번호 재설정 링크를 보내드려요.
            </p>
            <form onSubmit={handleSubmit} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold mb-1.5">이메일</label>
                <input
                  type="email"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  placeholder="email@example.com"
                  className="w-full px-4 py-3.5 bg-surface border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                  required
                />
              </div>

              {error && <p className="text-terracotta text-sm">{error}</p>}

              <button
                type="submit"
                disabled={loading}
                className="w-full py-4 bg-sage hover:bg-sage-dark text-white rounded-2xl font-semibold transition disabled:opacity-50"
              >
                {loading ? '전송 중...' : '재설정 메일 받기'}
              </button>
            </form>

            <p className="text-center text-sm text-textsub mt-6">
              이메일이 기억나지 않나요?{' '}
              <button
                type="button"
                onClick={() => navigate('/find-id')}
                className="text-sage-dark font-semibold hover:underline"
              >
                아이디 찾기
              </button>
            </p>
          </>
        ) : (
          <div className="bg-surface rounded-2xl p-6 text-center space-y-3">
            <p className="text-base font-bold text-sage-dark">메일을 보냈어요</p>
            <p className="text-sm text-textsub">
              입력하신 이메일이 가입된 계정이라면<br />
              비밀번호 재설정 링크가 발송됐어요.<br />
              메일함을 확인해 주세요.
            </p>
            <p className="text-xs text-textsub mt-2">
              링크는 1시간 동안 유효합니다.
            </p>
            <button
              onClick={() => navigate('/login')}
              className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition mt-4"
            >
              로그인으로 돌아가기
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default FindPasswordPage
