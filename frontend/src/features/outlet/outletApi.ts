import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, Outlet } from '../../types'

export const outletApi = createApi({
  reducerPath: 'outletApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Outlet'],
  endpoints: (builder) => ({
    getOutlets: builder.query<ApiResponse<Outlet[]>, void>({
      query: () => ({ url: API_ROUTES.OUTLETS, method: 'GET' }),
      providesTags: ['Outlet'],
    }),
    getAllOutlets: builder.query<ApiResponse<Outlet[]>, void>({
      query: () => ({ url: `${API_ROUTES.OUTLETS}/all`, method: 'GET' }),
      providesTags: ['Outlet'],
    }),
    createOutlet: builder.mutation<ApiResponse<Outlet>, Partial<Outlet>>({
      query: (body) => ({ url: API_ROUTES.OUTLETS, method: 'POST', data: body }),
      invalidatesTags: ['Outlet'],
    }),
    updateOutlet: builder.mutation<ApiResponse<Outlet>, { id: string; data: Partial<Outlet> }>({
      query: ({ id, data }) => ({ url: `${API_ROUTES.OUTLETS}/${id}`, method: 'PUT', data }),
      invalidatesTags: ['Outlet'],
    }),
    deleteOutlet: builder.mutation<ApiResponse<void>, string>({
      query: (id) => ({ url: `${API_ROUTES.OUTLETS}/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Outlet'],
    }),
  }),
})

export const {
  useGetOutletsQuery,
  useGetAllOutletsQuery,
  useCreateOutletMutation,
  useUpdateOutletMutation,
  useDeleteOutletMutation,
} = outletApi
