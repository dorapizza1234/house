import { useState } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import apiClient from '../api/client'

function ResetPasswordPage() {
  const [searchParams] = useSearchParams()
  const token = searchParams.get('token') ?? ''

  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [done, setDone] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (newPassword !== confirmPassword) {
      setError('비밀번호가 일치하지 않습니다')
      return
    }

    setLoading(true)
    try {
      await apiClient.post('/api/auth/password/reset-confirm', {
        token,
        newPassword,
      })
      setDone(true)
    } catch (err: any) {
      setError(err.response?.data?.message || '재설정 실패')
    } finally {
      setLoading(false)
    }
  }

  if (!token) {
    return (
      <div className="min-h-screen flex items-center justify-center px-6">
        <div className="bg-surface rounded-2xl p-6 text-center max-w-sm">
          <p className="text-base font-bold text-terracotta mb-2">유효하지 않은 링크</p>
          <p className="text-sm text-textsub mb-4">
            메일 본문의 링크를 다시 클릭해 주세요.
          </p>
          <button
            onClick={() => navigate('/find-password')}
            className="w-full py-3 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition"
          >
            비밀번호 찾기 다시
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen flex justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <h1 className="text-lg font-bold mb-2">비밀번호 재설정</h1>
        <p className="text-sm text-textsub mb-6">새 비밀번호를 설정해 주세요.</p>

        {!done ? (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label className="block text-sm font-semibold mb-1.5">새 비밀번호</label>
              <input
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                placeholder="8자 이상, 영문 대/소문자 + 숫자"
                className="w-full px-4 py-3.5 bg-surface border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                required
              />
            </div>

            <div>
              <label className="block text-sm font-semibold mb-1.5">비밀번호 확인</label>
              <input
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                placeholder="다시 한 번 입력"
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
              {loading ? '변경 중...' : '비밀번호 변경'}
            </button>
          </form>
        ) : (
          <div className="bg-surface rounded-2xl p-6 text-center space-y-3">
            <p className="text-base font-bold text-sage-dark">변경 완료</p>
            <p className="text-sm text-textsub">새 비밀번호로 로그인해 주세요.</p>
            <button
              onClick={() => navigate('/login')}
              className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition mt-2"
            >
              로그인하러 가기
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default ResetPasswordPage
