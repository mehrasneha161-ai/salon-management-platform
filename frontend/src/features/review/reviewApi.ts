import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse } from '../../types'

export interface Review {
  id: string
  bookingId: string
  bookingRef: string
  customerId: string
  customerName: string
  staffId?: string
  staffName?: string
  rating: number
  comment?: string
  createdAt: string
}

export const reviewApi = createApi({
  reducerPath: 'reviewApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Review'],
  endpoints: (builder) => ({
    createReview: builder.mutation<ApiResponse<Review>, { bookingId: string; rating: number; comment?: string }>({
      query: (body) => ({ url: API_ROUTES.REVIEWS, method: 'POST', data: body }),
      invalidatesTags: ['Review'],
    }),
    getStaffReviews: builder.query<ApiResponse<Review[]>, string>({
      query: (staffId) => ({ url: `${API_ROUTES.REVIEWS}/staff/${staffId}`, method: 'GET' }),
      providesTags: ['Review'],
    }),
    getMyReviews: builder.query<ApiResponse<Review[]>, void>({
      query: () => ({ url: `${API_ROUTES.REVIEWS}/my`, method: 'GET' }),
      providesTags: ['Review'],
    }),
  }),
})

export const {
  useCreateReviewMutation,
  useGetStaffReviewsQuery,
  useGetMyReviewsQuery,
} = reviewApi
