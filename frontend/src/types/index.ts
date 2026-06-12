export interface User {
  userId?: string
  fullName: string
  phoneNumber: string
  role: 'CUSTOMER' | 'STAFF' | 'ADMIN'
  accessToken: string
  refreshToken: string
}

export interface Outlet {
  id: string
  name: string
  address: string
  city: string
  phone: string
  latitude: number
  longitude: number
  isActive: boolean
}

export interface SalonService {
  id: string
  name: string
  description: string
  categoryId: string
  categoryName: string
  outletId?: string
  durationMinutes: number
  price: number
  isActive: boolean
}

export interface ServicePackage {
  id: string
  name: string
  description: string
  outletId?: string
  price: number
  discountPct: number
  services: SalonService[]
  isActive: boolean
}

export interface StaffMember {
  id: string
  userId: string
  fullName: string
  phoneNumber: string
  specialization: string
  bio: string
  profilePicUrl?: string
  status: 'AVAILABLE' | 'BUSY' | 'OFF_DUTY'
  outletId: string
  outletName: string
  totalPresentDays: number
}

export interface Booking {
  id: string
  bookingRef: string
  customerName: string
  customerPhone: string
  outletId: string
  outletName: string
  staffId?: string
  staffName?: string
  serviceName?: string
  packageName?: string
  scheduledDate: string
  scheduledTime: string
  durationMinutes: number
  status: BookingStatus
  totalAmount: number
  notes?: string
  createdAt: string
}

export type BookingStatus =
  | 'PENDING'
  | 'SLOT_LOCKED'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'
  | 'REJECTED'

export interface GalleryItem {
  id: string
  title: string
  beforeUrl: string
  afterUrl: string
  categoryId: string
  categoryName: string
  createdAt: string
}

export interface OutletRevenue {
  outletId: string
  outletName: string
  totalRevenue: number
  totalBookings: number
}

export interface PopularService {
  serviceId: string
  serviceName: string
  categoryName: string
  bookingCount: number
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
  errorCode?: string
  timestamp: string
}

export interface PagedResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  last: boolean
}
