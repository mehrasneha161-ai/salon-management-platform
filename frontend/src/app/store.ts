import { configureStore } from '@reduxjs/toolkit'
import authReducer from '../features/auth/authSlice'
import { authApi } from '../features/auth/authApi'
import { outletApi } from '../features/outlet/outletApi'
import { serviceApi } from '../features/service/serviceApi'
import { bookingApi } from '../features/booking/bookingApi'
import { staffApi } from '../features/staff/staffApi'
import { galleryApi } from '../features/gallery/galleryApi'
import { analyticsApi } from '../features/analytics/analyticsApi'
import { notificationApi } from '../features/notification/notificationApi'
import { paymentApi } from '../features/payment/paymentApi'
import { reviewApi } from '../features/review/reviewApi'
import { favouriteApi } from '../features/favourite/favouriteApi'

export const store = configureStore({
  reducer: {
    auth: authReducer,
    [authApi.reducerPath]: authApi.reducer,
    [outletApi.reducerPath]: outletApi.reducer,
    [serviceApi.reducerPath]: serviceApi.reducer,
    [bookingApi.reducerPath]: bookingApi.reducer,
    [staffApi.reducerPath]: staffApi.reducer,
    [galleryApi.reducerPath]: galleryApi.reducer,
    [analyticsApi.reducerPath]: analyticsApi.reducer,
    [notificationApi.reducerPath]: notificationApi.reducer,
    [paymentApi.reducerPath]: paymentApi.reducer,
    [reviewApi.reducerPath]: reviewApi.reducer,
    [favouriteApi.reducerPath]: favouriteApi.reducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().concat(
      authApi.middleware,
      outletApi.middleware,
      serviceApi.middleware,
      bookingApi.middleware,
      staffApi.middleware,
      galleryApi.middleware,
      analyticsApi.middleware,
      notificationApi.middleware,
      paymentApi.middleware,
      reviewApi.middleware,
      favouriteApi.middleware
    ),
})

export type RootState = ReturnType<typeof store.getState>
export type AppDispatch = typeof store.dispatch
