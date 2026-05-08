import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import apiClient from '../api/client'
import BottomNav from '../components/BottomNav'

type Contribution = {
  memberId: number
  nickname: string
  wateringCount: number
}

type Plant = {
  id: number
  familyId: number
  name: string
  state: string
  plantedByNickname: string
  plantedAt: string
  lastWateredAt: string
  stateChangedAt: string
  happiness: number
  contributions: Contribution[]
}

const STATE_LABEL: Record<string, string> = {
  ALIVE: '건강해요',
  WILTED: '시들고 있어요',
  DEAD: '죽었어요',
}

function PlantPage() {
  const [plant, setPlant] = useState<Plant | null>(null)
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()

  const familyId = localStorage.getItem('familyId')

  const fetchPlant = async () => {
    try {
      const response = await apiClient.get('/api/families/me/plant')
      setPlant(response.data)
    } catch {
      setPlant(null)
    }
  }

  useEffect(() => {
    if (!familyId) {
      navigate('/family-invite')
      return
    }
    fetchPlant()
  }, [])

  const handlePlant = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      await apiClient.post('/api/families/me/plant', { name })
      setName('')
      await fetchPlant()
    } catch (err: any) {
      setError(err.response?.data?.message || '심기 실패')
    } finally {
      setLoading(false)
    }
  }

  const handleWater = async () => {
    setError('')
    setLoading(true)
    try {
      await apiClient.post('/api/families/me/plant/water')
      await fetchPlant()
    } catch (err: any) {
      setError(err.response?.data?.message || '물주기 실패')
    } finally {
      setLoading(false)
    }
  }

  const handleRevive = async () => {
    setError('')
    setLoading(true)
    try {
      await apiClient.post('/api/families/me/plant/revive')
      await fetchPlant()
    } catch (err: any) {
      setError(err.response?.data?.message || '부활 실패')
    } finally {
      setLoading(false)
    }
  }

  const stateColor = (state: string) =>
    state === 'ALIVE'
      ? 'text-sage-dark'
      : state === 'WILTED'
      ? 'text-terracotta'
      : 'text-textsub'

  return (
    <div className="min-h-screen pb-24">
      <header className="px-6 pt-10 pb-4 border-b border-borderlight bg-ivory max-w-md mx-auto">
        <h1 className="text-lg font-bold">가족 식물</h1>
      </header>

      <main className="px-6 py-6 space-y-6 max-w-md mx-auto">
        {!plant && (
          <section className="bg-surface rounded-2xl p-6">
            <h2 className="text-base font-bold mb-1">식물 심기</h2>
            <p className="text-xs text-textsub mb-5">
              가족이 함께 키우는 식물을 시작해 보세요. <span className="text-sage-dark font-semibold">30점 차감</span>
            </p>
            <form onSubmit={handlePlant} className="space-y-3">
              <div>
                <label className="block text-sm font-semibold mb-1.5">식물 이름</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="예: 초록이"
                  className="w-full px-4 py-3.5 bg-ivory border border-borderlight rounded-xl text-[15px] focus:border-sage transition"
                  required
                />
              </div>
              <button
                type="submit"
                disabled={loading}
                className="w-full py-3.5 bg-sage hover:bg-sage-dark text-white rounded-xl font-semibold transition disabled:opacity-50"
              >
                {loading ? '심는 중...' : '심기'}
              </button>
            </form>
          </section>
        )}

        {plant && (
          <>
            <section
              className="rounded-2xl p-6 text-center"
              style={{
                background:
                  'linear-gradient(180deg, #E8F0E1 0%, rgba(232,240,225,0) 100%)',
              }}
            >
              <h2 className="text-xl font-bold mb-1">{plant.name}</h2>
              <p className={`text-xs font-semibold ${stateColor(plant.state)}`}>
                {STATE_LABEL[plant.state] ?? plant.state}
              </p>
              <p className="text-[11px] text-textsub mt-3">
                심은 사람: {plant.plantedByNickname}
              </p>
              <p className="text-[11px] text-textsub">
                마지막 물준 시각: {new Date(plant.lastWateredAt).toLocaleString()}
              </p>
            </section>

            {plant.state !== 'DEAD' ? (
              <button
                onClick={handleWater}
                disabled={loading}
                className="w-full py-4 bg-sage hover:bg-sage-dark text-white rounded-2xl font-semibold transition disabled:opacity-50"
              >
                {loading ? '물주는 중...' : '물주기 (30점)'}
              </button>
            ) : (
              <button
                onClick={handleRevive}
                disabled={loading}
                className="w-full py-4 bg-terracotta hover:opacity-90 text-white rounded-2xl font-semibold transition disabled:opacity-50"
              >
                {loading ? '부활 중...' : '부활시키기 (50점)'}
              </button>
            )}

            <section>
              <h3 className="text-xs font-bold text-textsub mb-3">멤버별 물주기 기여</h3>
              <div className="bg-surface rounded-2xl divide-y divide-borderlight">
                {plant.contributions.map((c) => (
                  <div
                    key={c.memberId}
                    className="px-4 py-3 flex items-center justify-between text-sm"
                  >
                    <span>{c.nickname}</span>
                    <span className="text-textsub">{c.wateringCount}회</span>
                  </div>
                ))}
              </div>
            </section>
          </>
        )}

        {error && <p className="text-terracotta text-sm">{error}</p>}
      </main>

      <BottomNav />
    </div>
  )
}

export default PlantPage
