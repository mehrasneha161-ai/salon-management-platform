import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, User } from '../../types'

export const authApi = createApi({
  reducerPath: 'authApi',
  baseQuery: axiosBaseQuery(),
  endpoints: (builder) => ({
    register: builder.mutation<ApiResponse<User>, { fullName: string; phoneNumber: string; password: string }>({
      query: (body) => ({ url: API_ROUTES.AUTH.REGISTER, method: 'POST', data: body }),
    }),
    login: builder.mutation<ApiResponse<User>, { phoneNumber: string; password: string }>({
      query: (body) => ({ url: API_ROUTES.AUTH.LOGIN, method: 'POST', data: body }),
    }),
    logout: builder.mutation<ApiResponse<void>, string>({
      query: (token) => ({ url: `${API_ROUTES.AUTH.LOGOUT}?token=${token}`, method: 'POST' }),
    }),
  }),
})

export const { useRegisterMutation, useLoginMutation, useLogoutMutation } = authApi
