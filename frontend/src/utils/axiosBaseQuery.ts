import axiosInstance from '../services/axiosInstance'
import type { AxiosRequestConfig } from 'axios'

export const axiosBaseQuery =
  () =>
  async ({ url, method, data, params }: AxiosRequestConfig) => {
    try {
      const result = await axiosInstance({ url, method, data, params })
      return { data: result.data }
    } catch (axiosError: unknown) {
      const err = axiosError as { response?: { status: number; data: unknown } }
      return {
        error: {
          status: err.response?.status,
          data: err.response?.data,
        },
      }
    }
  }
