import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import PhoneVerifier from '../components/PhoneVerifier'

function FindIdPage() {
  const [maskedEmail, setMaskedEmail] = useState('')
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const handleVerified = async (token: string) => {
    setError('')
    try {
      const response = await apiClient.post('/api/auth/find-id', {
        verificationToken: token,
      })
      setMaskedEmail(response.data.maskedEmail)
    } catch (err: any) {
      setError(err.response?.data?.message || 'ID 조회 실패')
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
          <h1 className="text-lg font-bold">아이디 찾기</h1>
        </div>

        {!maskedEmail ? (
          <>
            <p className="text-sm text-textsub mb-6">
              가입 시 등록한 휴대폰 번호로 본인 인증 후 이메일을 확인할 수 있어요.
            </p>
            <PhoneVerifier purpose="FIND_ID" onVerified={handleVerified} />
            {error && <p className="text-terracotta text-sm mt-3">{error}</p>}
          </>
        ) : (
          <div className="bg-surface rounded-2xl p-6 text-center space-y-3">
            <p className="text-sm text-textsub">회원님의 이메일은</p>
            <p className="text-xl font-bold text-sage-dark">{maskedEmail}</p>
            <p className="text-xs text-textsub">입니다</p>
            <button
              onClick={() => navigate('/login')}
              className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition mt-4"
            >
              로그인하러 가기
            </button>
          </div>
        )}
      </div>
    </div>
  )
}

export default FindIdPage
