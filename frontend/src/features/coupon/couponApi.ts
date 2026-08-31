import { createApi } from '@reduxjs/toolkit/query/react'
import { API_ROUTES } from '../../constants'
import type {
  ApiResponse,
  Coupon,
  CouponRequest,
  CouponValidationRequest,
  CouponValidationResponse,
} from '../../types'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'

export const couponApi = createApi({
  reducerPath: 'couponApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Coupon'],
  endpoints: (builder) => ({
    validateCoupon: builder.mutation<ApiResponse<CouponValidationResponse>, CouponValidationRequest>({
      query: (body) => ({
        url: `${API_ROUTES.COUPONS}/validate`,
        method: 'POST',
        data: body,
      }),
    }),
    getCoupons: builder.query<ApiResponse<Coupon[]>, void>({
      query: () => ({ url: API_ROUTES.COUPONS, method: 'GET' }),
      providesTags: ['Coupon'],
    }),
    getCoupon: builder.query<ApiResponse<Coupon>, string>({
      query: (id) => ({ url: `${API_ROUTES.COUPONS}/${id}`, method: 'GET' }),
      providesTags: ['Coupon'],
    }),
    createCoupon: builder.mutation<ApiResponse<Coupon>, CouponRequest>({
      query: (body) => ({ url: API_ROUTES.COUPONS, method: 'POST', data: body }),
      invalidatesTags: ['Coupon'],
    }),
    updateCoupon: builder.mutation<ApiResponse<Coupon>, { id: string; data: CouponRequest }>({
      query: ({ id, data }) => ({
        url: `${API_ROUTES.COUPONS}/${id}`,
        method: 'PUT',
        data,
      }),
      invalidatesTags: ['Coupon'],
    }),
    toggleCoupon: builder.mutation<ApiResponse<Coupon>, string>({
      query: (id) => ({
        url: `${API_ROUTES.COUPONS}/${id}/toggle`,
        method: 'PATCH',
      }),
      invalidatesTags: ['Coupon'],
    }),
  }),
})

export const {
  useValidateCouponMutation,
  useGetCouponsQuery,
  useGetCouponQuery,
  useCreateCouponMutation,
  useUpdateCouponMutation,
  useToggleCouponMutation,
} = couponApi
