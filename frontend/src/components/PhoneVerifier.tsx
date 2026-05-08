import { useEffect, useState } from 'react'
import apiClient from '../api/client'

type Props = {
  purpose: 'SIGNUP' | 'FIND_ID'
  onVerified: (token: string, phone: string) => void
}

function PhoneVerifier({ purpose, onVerified }: Props) {
  const [phone, setPhone] = useState('')
  const [code, setCode] = useState('')
  const [codeSent, setCodeSent] = useState(false)
  const [verified, setVerified] = useState(false)
  const [secondsLeft, setSecondsLeft] = useState(0)
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  // 카운트다운 (3분)
  useEffect(() => {
    if (secondsLeft <= 0) return
    const interval = setInterval(() => {
      setSecondsLeft((s) => s - 1)
    }, 1000)
    return () => clearInterval(interval)
  }, [secondsLeft])

  const sendCode = async () => {
    if (!/^01[0-9]{8,9}$/.test(phone)) {
      setError('올바른 휴대폰 번호 형식이 아닙니다')
      return
    }
    setError('')
    setLoading(true)
    try {
      await apiClient.post('/api/auth/phone/send-code', { phone, purpose })
      setCodeSent(true)
      setSecondsLeft(180)
      setCode('')
    } catch (err: any) {
      setError(err.response?.data?.message || '발송 실패')
    } finally {
      setLoading(false)
    }
  }

  const verifyCode = async () => {
    setError('')
    setLoading(true)
    try {
      const response = await apiClient.post('/api/auth/phone/verify-code', {
        phone,
        code,
        purpose,
      })
      setVerified(true)
      setSecondsLeft(0)
      onVerified(response.data.verificationToken, phone)
    } catch (err: any) {
      setError(err.response?.data?.message || '인증 실패')
    } finally {
      setLoading(false)
    }
  }

  const formatTime = (s: number) => {
    const m = Math.floor(s / 60)
    const ss = s % 60
    return `${m}:${ss.toString().padStart(2, '0')}`
  }

  return (
    <div className="space-y-3">
      <div>
        <label className="block text-sm font-semibold mb-1.5">휴대폰 번호</label>
        <div className="flex gap-2">
          <input
            type="tel"
            value={phone}
            onChange={(e) => setPhone(e.target.value.replace(/[^0-9]/g, ''))}
            placeholder="01012345678"
            disabled={verified}
            className="flex-1 px-4 py-3.5 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition disabled:opacity-50"
          />
          <button
            type="button"
            onClick={sendCode}
            disabled={loading || verified}
            className="px-4 py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold text-sm disabled:opacity-50 whitespace-nowrap"
          >
            {codeSent ? '재전송' : '인증번호'}
          </button>
        </div>
      </div>

      {codeSent && !verified && (
        <div>
          <label className="block text-sm font-semibold mb-1.5">
            인증번호
            {secondsLeft > 0 && (
              <span className="ml-2 text-terracotta font-normal">{formatTime(secondsLeft)}</span>
            )}
            {secondsLeft === 0 && codeSent && (
              <span className="ml-2 text-textsub font-normal">만료됨 — 재전송</span>
            )}
          </label>
          <div className="flex gap-2">
            <input
              type="text"
              inputMode="numeric"
              value={code}
              onChange={(e) => setCode(e.target.value.replace(/[^0-9]/g, '').slice(0, 6))}
              placeholder="6자리 숫자"
              maxLength={6}
              className="flex-1 px-4 py-3.5 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition tracking-widest"
            />
            <button
              type="button"
              onClick={verifyCode}
              disabled={loading || code.length !== 6}
              className="px-5 py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold text-sm disabled:opacity-50"
            >
              확인
            </button>
          </div>
        </div>
      )}

      {verified && (
        <p className="text-sage-dark text-sm font-semibold">인증 완료</p>
      )}

      {error && <p className="text-terracotta text-sm">{error}</p>}
    </div>
  )
}

export default PhoneVerifier
