import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import PhoneVerifier from '../components/PhoneVerifier'

type EmailStatus = 'idle' | 'checking' | 'available' | 'taken'

function SignupPage() {
  const [email, setEmail] = useState('')
  const [emailStatus, setEmailStatus] = useState<EmailStatus>('idle')
  const [password, setPassword] = useState('')
  const [nickname, setNickname] = useState('')
  const [birthDate, setBirthDate] = useState('')

  const [phone, setPhone] = useState('')
  const [verificationToken, setVerificationToken] = useState('')

  const [agree14, setAgree14] = useState(false)
  const [agreeTerms, setAgreeTerms] = useState(false)
  const [agreePrivacy, setAgreePrivacy] = useState(false)
  const [agreeMarketing, setAgreeMarketing] = useState(false)

  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const checkEmailAvailability = async () => {
    if (!email || !/\S+@\S+\.\S+/.test(email)) {
      setError('올바른 이메일 형식이 아닙니다')
      setEmailStatus('idle')
      return
    }
    setError('')
    setEmailStatus('checking')
    try {
      const res = await apiClient.get(
        `/api/auth/email/check?email=${encodeURIComponent(email)}`
      )
      setEmailStatus(res.data.available ? 'available' : 'taken')
    } catch {
      setEmailStatus('idle')
    }
  }

  const handlePhoneVerified = (token: string, verifiedPhone: string) => {
    setVerificationToken(token)
    setPhone(verifiedPhone)
  }

  const allRequiredAgreed = agree14 && agreeTerms && agreePrivacy
  const allChecked = allRequiredAgreed && agreeMarketing

  const toggleAll = (checked: boolean) => {
    setAgree14(checked)
    setAgreeTerms(checked)
    setAgreePrivacy(checked)
    setAgreeMarketing(checked)
  }

  const canSubmit =
    emailStatus === 'available' &&
    !!verificationToken &&
    !!password &&
    !!nickname &&
    !!birthDate &&
    allRequiredAgreed

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!canSubmit) return
    setError('')
    setLoading(true)
    try {
      await apiClient.post('/api/auth/signup', {
        email,
        password,
        nickname,
        birthDate,
        phone,
        phoneVerificationToken: verificationToken,
      })
      navigate('/login')
    } catch (err: any) {
      setError(extractErrorMessage(err, '회원가입 실패'))
    } finally {
      setLoading(false)
    }
  }

  function extractErrorMessage(err: any, fallback: string): string {
    const data = err.response?.data
    if (!data) return fallback
    if (typeof data === 'string') return data
    if (data.message) return data.message
    // validation 에러: { fieldName: "메시지", ... } 형태 — 첫 메시지 표시
    const firstError = Object.values(data).find((v) => typeof v === 'string')
    return (firstError as string) || fallback
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
          <h1 className="text-lg font-bold">회원가입</h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* 이메일 + 중복확인 */}
          <div>
            <label className="block text-sm font-semibold mb-1.5">이메일</label>
            <div className="flex gap-2">
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value)
                  setEmailStatus('idle')
                }}
                placeholder="email@example.com"
                className="flex-1 px-4 py-3.5 bg-surface border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                required
              />
              <button
                type="button"
                onClick={checkEmailAvailability}
                disabled={emailStatus === 'checking'}
                className="px-4 py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold text-sm disabled:opacity-50 whitespace-nowrap"
              >
                중복확인
              </button>
            </div>
            {emailStatus === 'available' && (
              <p className="text-sage-dark text-xs mt-1.5">사용 가능한 이메일입니다</p>
            )}
            {emailStatus === 'taken' && (
              <p className="text-terracotta text-xs mt-1.5">이미 가입된 이메일입니다</p>
            )}
            {emailStatus === 'checking' && (
              <p className="text-textsub text-xs mt-1.5">확인 중...</p>
            )}
          </div>

          {/* 비밀번호 */}
          <div>
            <label className="block text-sm font-semibold mb-1.5">비밀번호</label>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="8자 이상, 영문 대/소문자 + 숫자"
              className="w-full px-4 py-3.5 bg-surface border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
              required
            />
          </div>

          {/* 닉네임 */}
          <div>
            <label className="block text-sm font-semibold mb-1.5">닉네임</label>
            <input
              type="text"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="가족이 부를 이름"
              className="w-full px-4 py-3.5 bg-surface border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
              required
            />
          </div>

          {/* 생년월일 */}
          <div>
            <label className="block text-sm font-semibold mb-1.5">생년월일</label>
            <input
              type="date"
              value={birthDate}
              onChange={(e) => setBirthDate(e.target.value)}
              className="w-full px-4 py-3.5 bg-surface border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
              required
            />
            <span className="block text-xs text-textsub mt-1.5">
              가족 캘린더에 자동으로 표시돼요
            </span>
          </div>

          {/* 핸드폰 인증 */}
          <div className="bg-surface rounded-2xl p-4">
            <h2 className="text-sm font-bold mb-3">본인 인증</h2>
            <PhoneVerifier purpose="SIGNUP" onVerified={handlePhoneVerified} />
          </div>

          {/* 약관 동의 */}
          <div className="bg-surface rounded-2xl p-4 space-y-3">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={allChecked}
                onChange={(e) => toggleAll(e.target.checked)}
                className="w-5 h-5 accent-sage"
              />
              <span className="font-bold">전체 동의</span>
            </label>

            <hr className="border-borderlight" />

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={agree14}
                onChange={(e) => setAgree14(e.target.checked)}
                className="w-5 h-5 accent-sage"
              />
              <span className="text-sm">
                <span className="text-terracotta">[필수]</span> 만 14세 이상입니다
              </span>
            </label>

            <label className="flex items-center justify-between gap-3 cursor-pointer">
              <span className="flex items-center gap-3">
                <input
                  type="checkbox"
                  checked={agreeTerms}
                  onChange={(e) => setAgreeTerms(e.target.checked)}
                  className="w-5 h-5 accent-sage"
                />
                <span className="text-sm">
                  <span className="text-terracotta">[필수]</span> 이용약관 동의
                </span>
              </span>
              <button
                type="button"
                onClick={() => navigate('/terms')}
                className="text-xs text-textsub underline"
              >
                보기
              </button>
            </label>

            <label className="flex items-center justify-between gap-3 cursor-pointer">
              <span className="flex items-center gap-3">
                <input
                  type="checkbox"
                  checked={agreePrivacy}
                  onChange={(e) => setAgreePrivacy(e.target.checked)}
                  className="w-5 h-5 accent-sage"
                />
                <span className="text-sm">
                  <span className="text-terracotta">[필수]</span> 개인정보 수집·이용 동의
                </span>
              </span>
              <button
                type="button"
                onClick={() => navigate('/privacy')}
                className="text-xs text-textsub underline"
              >
                보기
              </button>
            </label>

            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="checkbox"
                checked={agreeMarketing}
                onChange={(e) => setAgreeMarketing(e.target.checked)}
                className="w-5 h-5 accent-sage"
              />
              <span className="text-sm text-textsub">
                [선택] 마케팅 정보 수신 동의
              </span>
            </label>
          </div>

          {error && <p className="text-terracotta text-sm">{error}</p>}

          <button
            type="submit"
            disabled={!canSubmit || loading}
            className="w-full py-4 bg-sage hover:bg-sage-dark text-white rounded-2xl font-semibold transition disabled:opacity-50"
          >
            {loading ? '가입 중...' : '회원가입'}
          </button>
        </form>

        <p className="text-center mt-6 text-sm text-textsub">
          이미 계정이 있으세요?{' '}
          <button
            onClick={() => navigate('/login')}
            className="text-sage-dark font-semibold hover:underline"
          >
            로그인
          </button>
        </p>
      </div>
    </div>
  )
}

export default SignupPage
