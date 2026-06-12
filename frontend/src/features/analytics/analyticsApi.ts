import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, OutletRevenue, PopularService } from '../../types'

export const analyticsApi = createApi({
  reducerPath: 'analyticsApi',
  baseQuery: axiosBaseQuery(),
  endpoints: (builder) => ({
    getOutletPerformance: builder.query<ApiResponse<OutletRevenue[]>, void>({
      query: () => ({ url: API_ROUTES.ANALYTICS.OUTLET_PERFORMANCE, method: 'GET' }),
    }),
    getPopularServices: builder.query<ApiResponse<PopularService[]>, void>({
      query: () => ({ url: API_ROUTES.ANALYTICS.POPULAR_SERVICES, method: 'GET' }),
    }),
  }),
})

export const { useGetOutletPerformanceQuery, useGetPopularServicesQuery } = analyticsApi
