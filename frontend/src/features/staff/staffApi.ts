import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, StaffMember } from '../../types'

export const staffApi = createApi({
  reducerPath: 'staffApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Staff'],
  endpoints: (builder) => ({
    getStaff: builder.query<ApiResponse<StaffMember[]>, { outletId?: string; status?: string }>({
      query: ({ outletId, status } = {}) => {
        const params = new URLSearchParams()
        if (outletId) params.append('outletId', outletId)
        if (status) params.append('status', status)
        return { url: `${API_ROUTES.STAFF}?${params}`, method: 'GET' }
      },
      providesTags: ['Staff'],
    }),
    registerStaff: builder.mutation<ApiResponse<StaffMember>, object>({
      query: (body) => ({ url: API_ROUTES.STAFF, method: 'POST', data: body }),
      invalidatesTags: ['Staff'],
    }),
    updateStaffStatus: builder.mutation<ApiResponse<StaffMember>, { id: string; status: string }>({
      query: ({ id, status }) => ({
        url: `${API_ROUTES.STAFF}/${id}/status?status=${status}`,
        method: 'PUT',
      }),
      invalidatesTags: ['Staff'],
    }),
    checkIn: builder.mutation<ApiResponse<object>, void>({
      query: () => ({ url: `${API_ROUTES.STAFF}/attendance/check-in`, method: 'POST' }),
    }),
    checkOut: builder.mutation<ApiResponse<object>, void>({
      query: () => ({ url: `${API_ROUTES.STAFF}/attendance/check-out`, method: 'POST' }),
    }),
    getAttendance: builder.query<ApiResponse<object[]>, { id: string; year?: number; month?: number }>({
      query: ({ id, year, month }) => {
        const params = new URLSearchParams()
        if (year) params.append('year', String(year))
        if (month) params.append('month', String(month))
        return { url: `${API_ROUTES.STAFF}/${id}/attendance?${params}`, method: 'GET' }
      },
    }),
  }),
})

export const {
  useGetStaffQuery,
  useRegisterStaffMutation,
  useUpdateStaffStatusMutation,
  useCheckInMutation,
  useCheckOutMutation,
  useGetAttendanceQuery,
} = staffApi
