import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse, StaffMember } from '../../types'

export const favouriteApi = createApi({
  reducerPath: 'favouriteApi',
  baseQuery: axiosBaseQuery(),
  tagTypes: ['Favourite'],
  endpoints: (builder) => ({
    getFavourites: builder.query<ApiResponse<StaffMember[]>, void>({
      query: () => ({ url: API_ROUTES.FAVOURITES, method: 'GET' }),
      providesTags: ['Favourite'],
    }),
    addFavourite: builder.mutation<ApiResponse<void>, string>({
      query: (staffId) => ({ url: `${API_ROUTES.FAVOURITES}/${staffId}`, method: 'POST' }),
      invalidatesTags: ['Favourite'],
    }),
    removeFavourite: builder.mutation<ApiResponse<void>, string>({
      query: (staffId) => ({ url: `${API_ROUTES.FAVOURITES}/${staffId}`, method: 'DELETE' }),
      invalidatesTags: ['Favourite'],
    }),
  }),
})

export const {
  useGetFavouritesQuery,
  useAddFavouriteMutation,
  useRemoveFavouriteMutation,
} = favouriteApi
