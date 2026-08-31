import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse } from '../../types'

export interface PaymentInfo {
  id: string
  bookingId: string
  bookingRef: string
  amount: number
  currency: string
  gateway: string
  status: string
  orderRef?: string
  keyId?: string
  paidAt?: string
}

export const paymentApi = createApi({
  reducerPath: 'paymentApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Payment'],
  endpoints: (builder) => ({
    initiatePayment: builder.mutation<ApiResponse<PaymentInfo>, { bookingId: string }>({
      query: (body) => ({ url: `${API_ROUTES.PAYMENTS}/initiate`, method: 'POST', data: body }),
      invalidatesTags: ['Payment'],
    }),
    verifyPayment: builder.mutation<
      ApiResponse<PaymentInfo>,
      { paymentId: string; gatewayPaymentId?: string; gatewaySignature?: string }
    >({
      query: (body) => ({ url: `${API_ROUTES.PAYMENTS}/verify`, method: 'POST', data: body }),
      invalidatesTags: ['Payment'],
    }),
    getPaymentByBooking: builder.query<ApiResponse<PaymentInfo>, string>({
      query: (bookingId) => ({ url: `${API_ROUTES.PAYMENTS}/booking/${bookingId}`, method: 'GET' }),
      providesTags: ['Payment'],
    }),
  }),
})

export const {
  useInitiatePaymentMutation,
  useVerifyPaymentMutation,
  useGetPaymentByBookingQuery,
} = paymentApi
