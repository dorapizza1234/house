import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'

function FamilyInvitePage() {
  const [familyName, setFamilyName] = useState('')
  const [inviteToken, setInviteToken] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await apiClient.post('/api/families', {
        name: familyName,
      })

      localStorage.setItem('familyId', String(response.data.familyId))
      navigate('/dashboard')
    } catch (err: any) {
      setError(err.response?.data?.message || '가족 생성 실패')
    } finally {
      setLoading(false)
    }
  }

  const handleAccept = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)

    try {
      const response = await apiClient.post('/api/families/invites/accept', {
        token: inviteToken,
      })

      localStorage.setItem('familyId', String(response.data.familyId))
      navigate('/dashboard')
    } catch (err: any) {
      setError(err.response?.data?.message || '초대 수락 실패')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen flex justify-center px-6 py-12">
      <div className="w-full max-w-sm">
        <h1 className="text-xl font-bold mb-2">가족 시작하기</h1>
        <p className="text-sm text-textsub mb-8">가족을 새로 만들거나 초대를 수락해서 시작해 보세요.</p>

        <section className="bg-surface border border-borderlight rounded-2xl p-5 mb-4">
          <h2 className="text-base font-bold mb-1">새 가족 만들기</h2>
          <p className="text-xs text-textsub mb-4">내가 OWNER가 되어 가족을 시작합니다.</p>
          <form onSubmit={handleCreate} className="space-y-3">
            <div>
              <label className="block text-sm font-semibold mb-1.5">가족 이름</label>
              <input
                type="text"
                value={familyName}
                onChange={(e) => setFamilyName(e.target.value)}
                placeholder="예: 도라네"
                className="w-full px-4 py-3.5 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition disabled:opacity-50"
            >
              {loading ? '생성 중...' : '가족 만들기'}
            </button>
          </form>
        </section>

        <section className="bg-surface border border-borderlight rounded-2xl p-5">
          <h2 className="text-base font-bold mb-1">초대 받았어요</h2>
          <p className="text-xs text-textsub mb-4">가족이 보낸 토큰으로 합류합니다.</p>
          <form onSubmit={handleAccept} className="space-y-3">
            <div>
              <label className="block text-sm font-semibold mb-1.5">초대 토큰</label>
              <input
                type="text"
                value={inviteToken}
                onChange={(e) => setInviteToken(e.target.value)}
                placeholder="가족이 보내준 토큰"
                className="w-full px-4 py-3.5 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                required
              />
            </div>

            <button
              type="submit"
              disabled={loading}
              className="w-full py-3.5 bg-surface hover:bg-ivory text-darkbrown border border-borderlight rounded-xl font-semibold transition disabled:opacity-50"
            >
              {loading ? '수락 중...' : '초대 수락'}
            </button>
          </form>
        </section>

        {error && <p className="text-terracotta text-sm mt-4">{error}</p>}
      </div>
    </div>
  )
}

export default FamilyInvitePage
