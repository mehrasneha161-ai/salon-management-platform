export const API_BASE_URL = '/api/v1'

export const API_ROUTES = {
  AUTH: {
    REGISTER: `${API_BASE_URL}/auth/register`,
    LOGIN: `${API_BASE_URL}/auth/login`,
    REFRESH: `${API_BASE_URL}/auth/refresh-token`,
    LOGOUT: `${API_BASE_URL}/auth/logout`,
  },
  OUTLETS: `${API_BASE_URL}/outlets`,
  SERVICES: `${API_BASE_URL}/services`,
  PACKAGES: `${API_BASE_URL}/packages`,
  BOOKINGS: `${API_BASE_URL}/bookings`,
  SLOTS: `${API_BASE_URL}/slots/available`,
  STAFF: `${API_BASE_URL}/staff`,
  GALLERY: `${API_BASE_URL}/gallery`,
  ANALYTICS: {
    OUTLET_PERFORMANCE: `${API_BASE_URL}/analytics/outlet-performance`,
    POPULAR_SERVICES: `${API_BASE_URL}/analytics/popular-services`,
  },
  NOTIFICATIONS: {
    BROADCAST: `${API_BASE_URL}/notifications/broadcast`,
    CAMPAIGN: `${API_BASE_URL}/notifications/campaign`,
  },
}

export const APP_ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  REGISTER: '/register',
  CUSTOMER: {
    DASHBOARD: '/customer/dashboard',
    OUTLETS: '/customer/outlets',
    BOOK: '/customer/book',
    HISTORY: '/customer/history',
    GALLERY: '/customer/gallery',
  },
  STAFF: {
    DASHBOARD: '/staff/dashboard',
    ATTENDANCE: '/staff/attendance',
  },
  ADMIN: {
    DASHBOARD: '/admin/dashboard',
    BOOKINGS: '/admin/bookings',
    STAFF: '/admin/staff',
    SERVICES: '/admin/services',
    OUTLETS: '/admin/outlets',
    GALLERY: '/admin/gallery',
    ANALYTICS: '/admin/analytics',
    NOTIFICATIONS: '/admin/notifications',
  },
}

export const ROLES = {
  CUSTOMER: 'CUSTOMER',
  STAFF: 'STAFF',
  ADMIN: 'ADMIN',
} as const
