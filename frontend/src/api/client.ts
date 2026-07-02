import axios from 'axios'

const baseURL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// access 토큰은 메모리(JS 변수)에만 보관 — localStorage X → XSS로 토큰 탈취 방지
let accessToken: string | null = null
export const setAccessToken = (token: string | null) => { accessToken = token }
export const getAccessToken = () => accessToken

const apiClient = axios.create({
  baseURL,
  withCredentials: true,                 // refresh 쿠키를 크로스사이트로 주고받음
  headers: { 'Content-Type': 'application/json' },
})

// 요청마다 메모리의 access 토큰을 Bearer 헤더로 첨부
apiClient.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

// 401이면 /refresh로 새 access 받아 원 요청을 1회만 재시도
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config
    if (error.response?.status === 401 && !original._retry) {
      original._retry = true
      try {
        // ⚠️ apiClient 아닌 raw axios로 호출 (무한루프 방지)
        const { data } = await axios.post(
          `${baseURL}/api/auth/refresh`, {}, { withCredentials: true }
        )
        setAccessToken(data.accessToken)
        original.headers.Authorization = `Bearer ${data.accessToken}`
        return apiClient(original)
      } catch (refreshError) {
        setAccessToken(null)
        window.location.href = '/login'
        return Promise.reject(refreshError)
      }
    }
    return Promise.reject(error)
  }
)

// 앱 로드 시 호출: refresh 쿠키가 살아있으면 access 재발급
export const silentRefresh = async () => {
  try {
    const { data } = await axios.post(
      `${baseURL}/api/auth/refresh`, {}, { withCredentials: true }
    )
    setAccessToken(data.accessToken)
    return true
  } catch {
    setAccessToken(null)
    return false
  }
}

export default apiClient