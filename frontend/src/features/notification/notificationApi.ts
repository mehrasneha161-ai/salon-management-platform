import { createApi } from '@reduxjs/toolkit/query/react'
import { axiosBaseQuery } from '../../utils/axiosBaseQuery'
import { API_ROUTES } from '../../constants'
import { ApiResponse } from '../../types'

export const notificationApi = createApi({
  reducerPath: 'notificationApi',
  baseQuery: axiosBaseQuery(),
  endpoints: (builder) => ({
    sendBroadcast: builder.mutation<ApiResponse<void>, { message: string; phoneNumbers: string[] }>({
      query: (body) => ({ url: API_ROUTES.NOTIFICATIONS.BROADCAST, method: 'POST', data: body }),
    }),
    sendCampaign: builder.mutation<ApiResponse<void>, string>({
      query: (message) => ({
        url: `${API_ROUTES.NOTIFICATIONS.CAMPAIGN}?message=${encodeURIComponent(message)}`,
        method: 'POST',
      }),
    }),
  }),
})

export const { useSendBroadcastMutation, useSendCampaignMutation } = notificationApi
