import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, PagedResponse, Booking } from '../../types'

export const bookingApi = createApi({
  reducerPath: 'bookingApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Booking'],
  endpoints: (builder) => ({
    createBooking: builder.mutation<ApiResponse<Booking>, object>({
      query: (body) => ({ url: API_ROUTES.BOOKINGS, method: 'POST', data: body }),
      invalidatesTags: ['Booking'],
    }),
    getMyBookings: builder.query<ApiResponse<PagedResponse<Booking>>, { page?: number; size?: number }>({
      query: ({ page = 0, size = 10 } = {}) => ({
        url: `${API_ROUTES.BOOKINGS}/my?page=${page}&size=${size}`,
        method: 'GET',
      }),
      providesTags: ['Booking'],
    }),
    getBookings: builder.query<ApiResponse<PagedResponse<Booking>>, { outletId?: string; date?: string; status?: string; page?: number }>({
      query: ({ outletId, date, status, page = 0 } = {}) => {
        const params = new URLSearchParams()
        if (outletId) params.append('outletId', outletId)
        if (date) params.append('date', date)
        if (status) params.append('status', status)
        params.append('page', String(page))
        return { url: `${API_ROUTES.BOOKINGS}?${params}`, method: 'GET' }
      },
      providesTags: ['Booking'],
    }),
    approveBooking: builder.mutation<ApiResponse<Booking>, { id: string; staffId: string }>({
      query: ({ id, staffId }) => ({
        url: `${API_ROUTES.BOOKINGS}/${id}/approve`,
        method: 'PUT',
        data: { staffId },
      }),
      invalidatesTags: ['Booking'],
    }),
    rejectBooking: builder.mutation<ApiResponse<Booking>, { id: string; reason?: string }>({
      query: ({ id, reason }) => ({
        url: `${API_ROUTES.BOOKINGS}/${id}/reject?reason=${reason ?? ''}`,
        method: 'PUT',
      }),
      invalidatesTags: ['Booking'],
    }),
    completeBooking: builder.mutation<ApiResponse<Booking>, string>({
      query: (id) => ({ url: `${API_ROUTES.BOOKINGS}/${id}/complete`, method: 'PUT' }),
      invalidatesTags: ['Booking'],
    }),
    cancelBooking: builder.mutation<ApiResponse<Booking>, string>({
      query: (id) => ({ url: `${API_ROUTES.BOOKINGS}/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Booking'],
    }),
    getAvailableSlots: builder.query<ApiResponse<string[]>, { outletId: string; staffId: string; date: string; durationMinutes?: number }>({
      query: ({ outletId, staffId, date, durationMinutes = 30 }) => ({
        url: `${API_ROUTES.SLOTS}?outletId=${outletId}&staffId=${staffId}&date=${date}&durationMinutes=${durationMinutes}`,
        method: 'GET',
      }),
    }),
  }),
})

export const {
  useCreateBookingMutation,
  useGetMyBookingsQuery,
  useGetBookingsQuery,
  useApproveBookingMutation,
  useRejectBookingMutation,
  useCompleteBookingMutation,
  useCancelBookingMutation,
  useGetAvailableSlotsQuery,
} = bookingApi
