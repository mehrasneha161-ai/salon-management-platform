import axios from 'axios'
import type {
  AxiosInstance,
  AxiosResponse,
  InternalAxiosRequestConfig,
} from 'axios'
import { logout, setTokens } from '../features/auth/authSlice'
import { API_ROUTES } from '../constants'

interface AxiosInterceptorStore {
  getState: () => {
    auth: {
      accessToken: string | null
      refreshToken: string | null
    }
  }
  dispatch: (
    action: ReturnType<typeof setTokens> | ReturnType<typeof logout>
  ) => unknown
}

interface RetryableRequestConfig extends InternalAxiosRequestConfig {
  _retry?: boolean
}

const axiosInstance: AxiosInstance = axios.create({
  baseURL: '/',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' },
})

let interceptorsInstalled = false
let isRefreshing = false
let failedQueue: Array<{
  resolve: (token: string) => void
  reject: (reason?: unknown) => void
}> = []

const processQueue = (error: unknown, token?: string) => {
  failedQueue.forEach((promise) => {
    if (error) promise.reject(error)
    else if (token) promise.resolve(token)
  })
  failedQueue = []
}

/**
 * Installs auth interceptors after the Redux store has been created.
 * Keeping store injection here prevents the store -> API -> Axios -> store cycle.
 */
export const setupAxiosInterceptors = (store: AxiosInterceptorStore) => {
  if (interceptorsInstalled) return
  interceptorsInstalled = true

  axiosInstance.interceptors.request.use(
    (config: InternalAxiosRequestConfig) => {
      const token = store.getState().auth.accessToken
      if (token && config.headers) {
        config.headers.Authorization = `Bearer ${token}`
      }
      return config
    },
    (error) => Promise.reject(error)
  )

  axiosInstance.interceptors.response.use(
    (response: AxiosResponse) => response,
    async (error) => {
      const originalRequest = error.config as RetryableRequestConfig | undefined

      if (error.response?.status === 401 && originalRequest && !originalRequest._retry) {
        if (isRefreshing) {
          return new Promise<string>((resolve, reject) => {
            failedQueue.push({ resolve, reject })
          })
            .then((token) => {
              originalRequest.headers.Authorization = `Bearer ${token}`
              return axiosInstance(originalRequest)
            })
            .catch((queueError) => Promise.reject(queueError))
        }

        originalRequest._retry = true
        isRefreshing = true
        const refreshToken = store.getState().auth.refreshToken

        try {
          const response = await axios.post(
            `${API_ROUTES.AUTH.REFRESH}?token=${refreshToken}`
          )
          const { accessToken, refreshToken: newRefreshToken } = response.data.data

          store.dispatch(setTokens({ accessToken, refreshToken: newRefreshToken }))
          processQueue(null, accessToken)
          originalRequest.headers.Authorization = `Bearer ${accessToken}`
          return axiosInstance(originalRequest)
        } catch (refreshError) {
          processQueue(refreshError)
          store.dispatch(logout())
          window.location.href = '/login'
          return Promise.reject(refreshError)
        } finally {
          isRefreshing = false
        }
      }

      return Promise.reject(error)
    }
  )
}

export default axiosInstance
