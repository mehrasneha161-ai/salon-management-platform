import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, SalonService, ServicePackage } from '../../types'

export const serviceApi = createApi({
  reducerPath: 'serviceApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Service', 'Package'],
  endpoints: (builder) => ({
    getServices: builder.query<ApiResponse<SalonService[]>, { categoryId?: string; outletId?: string }>({
      query: ({ categoryId, outletId } = {}) => {
        const params = new URLSearchParams()
        if (categoryId) params.append('categoryId', categoryId)
        if (outletId) params.append('outletId', outletId)
        return { url: `${API_ROUTES.SERVICES}?${params}`, method: 'GET' }
      },
      providesTags: ['Service'],
    }),
    createService: builder.mutation<ApiResponse<SalonService>, object>({
      query: (body) => ({ url: API_ROUTES.SERVICES, method: 'POST', data: body }),
      invalidatesTags: ['Service'],
    }),
    updateService: builder.mutation<ApiResponse<SalonService>, { id: string; data: object }>({
      query: ({ id, data }) => ({ url: `${API_ROUTES.SERVICES}/${id}`, method: 'PUT', data }),
      invalidatesTags: ['Service'],
    }),
    deleteService: builder.mutation<ApiResponse<void>, string>({
      query: (id) => ({ url: `${API_ROUTES.SERVICES}/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Service'],
    }),
    getPackages: builder.query<ApiResponse<ServicePackage[]>, { outletId?: string }>({
      query: ({ outletId } = {}) => ({
        url: outletId ? `${API_ROUTES.PACKAGES}?outletId=${outletId}` : API_ROUTES.PACKAGES,
        method: 'GET',
      }),
      providesTags: ['Package'],
    }),
    createPackage: builder.mutation<ApiResponse<ServicePackage>, object>({
      query: (body) => ({ url: API_ROUTES.PACKAGES, method: 'POST', data: body }),
      invalidatesTags: ['Package'],
    }),
  }),
})

export const {
  useGetServicesQuery,
  useCreateServiceMutation,
  useUpdateServiceMutation,
  useDeleteServiceMutation,
  useGetPackagesQuery,
  useCreatePackageMutation,
} = serviceApi
