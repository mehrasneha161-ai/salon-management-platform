import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, GalleryItem } from '../../types'

export const galleryApi = createApi({
  reducerPath: 'galleryApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Gallery'],
  endpoints: (builder) => ({
    getGallery: builder.query<ApiResponse<GalleryItem[]>, { categoryId?: string }>({
      query: ({ categoryId } = {}) => ({
        url: categoryId ? `${API_ROUTES.GALLERY}?categoryId=${categoryId}` : API_ROUTES.GALLERY,
        method: 'GET',
      }),
      providesTags: ['Gallery'],
    }),
    deleteGalleryItem: builder.mutation<ApiResponse<void>, string>({
      query: (id) => ({ url: `${API_ROUTES.GALLERY}/${id}`, method: 'DELETE' }),
      invalidatesTags: ['Gallery'],
    }),
  }),
})

export const { useGetGalleryQuery, useDeleteGalleryItemMutation } = galleryApi
