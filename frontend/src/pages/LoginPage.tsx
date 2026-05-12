import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import mainImage from '../assets/main.png'

function LoginPage() {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [rememberEmail, setRememberEmail] = useState(false)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  // mount 시 저장된 이메일 복구
  useEffect(() => {
    const savedEmail = localStorage.getItem('rememberedEmail')
    if (savedEmail) {
      setEmail(savedEmail)
      setRememberEmail(true)
    }
  }, [])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await apiClient.post('/api/auth/login', {
        email,
        password,
      })

      // 이메일 기억하기 처리 (로그인 성공 후)
      if (rememberEmail) {
        localStorage.setItem('rememberedEmail', email)
      } else {
        localStorage.removeItem('rememberedEmail')
      }

      localStorage.setItem('accessToken', response.data.accessToken)
      localStorage.setItem('refreshToken', response.data.refreshToken)

      if (response.data.familyId) {
        localStorage.setItem('familyId', String(response.data.familyId))
        navigate('/dashboard')
      } else {
        localStorage.removeItem('familyId')
        navigate('/family-invite')
      }
    } catch (err: any) {
      setError(err.response?.data?.message || '로그인 실패')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex items-center justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-sage-dark tracking-tight">우리집</h1>
          <p className="text-sm text-textsub mt-2">함께라서 더 따뜻한</p>
          <img
            src={mainImage}
            alt="우리집 가족"
            className="w-full max-w-xs mx-auto mt-6"
          />
        </div>

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

          <div>
            <label className="block text-sm font-semibold mb-1.5">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="비밀번호"
              className="w-full px-4 py-3.5 bg-surface border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
              required
            />
          </div>

          <label className="flex items-center gap-2 text-sm text-textsub cursor-pointer">
            <input
              type="checkbox"
              checked={rememberEmail}
              onChange={(e) => setRememberEmail(e.target.checked)}
              className="w-4 h-4 accent-sage"
            />
            이메일 기억하기
          </label>

          {error && <p className="text-terracotta text-sm">{error}</p>}

          <button
            type="submit"
            disabled={loading}
            className="w-full py-4 bg-sage hover:bg-sage-dark text-white rounded-2xl font-semibold transition disabled:opacity-50"
          >
            {loading ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <div className="flex justify-center gap-4 mt-4 text-xs text-textsub">
          <button
            onClick={() => navigate('/find-id')}
            className="hover:underline"
          >
            아이디 찾기
          </button>
          <span>·</span>
          <button
            onClick={() => navigate('/find-password')}
            className="hover:underline"
          >
            비밀번호 찾기
          </button>
        </div>

        <p className="text-center mt-6 text-sm text-textsub">
          계정이 없으세요?{' '}
          <button
            onClick={() => navigate('/signup')}
            className="text-sage-dark font-semibold hover:underline"
          >
            회원가입
          </button>
        </p>
      </div>
    </div>
  )
}

export default LoginPage
