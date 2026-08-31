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
  openingTime?: string
  closingTime?: string
}

export interface ServiceCategory {
  id: string
  name: string
  iconUrl?: string
  sortOrder: number
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
  subtotalAmount?: number
  discountAmount?: number
  totalAmount: number
  couponCode?: string
  notes?: string
  createdAt: string
}

export type CreateBookingRequest = {
  outletId: string
  staffId: string
  serviceId?: string
  packageId?: string
  scheduledDate: string
  scheduledTime: string
  notes?: string
  sessionId?: string
} & (
  | {
      couponCode: string
      expectedCouponId: string
      expectedSubtotalAmount: number
      expectedDiscountAmount: number
      expectedTotalAmount: number
    }
  | {
      couponCode?: never
      expectedCouponId?: never
      expectedSubtotalAmount?: never
      expectedDiscountAmount?: never
      expectedTotalAmount?: never
    }
)

export type CouponDiscountType = 'PERCENTAGE' | 'FIXED'

export interface Coupon {
  id: string
  code: string
  name: string
  description?: string | null
  discountType: CouponDiscountType
  discountValue: number
  minimumSpend: number
  maximumDiscount?: number | null
  validFrom: string
  validUntil: string
  usageLimit?: number | null
  perCustomerLimit?: number | null
  reservedCount: number
  redeemedCount: number
  outletId?: string | null
  serviceId?: string | null
  packageId?: string | null
  isActive: boolean
  createdAt: string
  updatedAt: string
}

export interface CouponValidationResponse {
  couponId: string
  code: string
  discountType: CouponDiscountType
  discountValue: number
  maximumDiscount?: number | null
  subtotalAmount: number
  discountAmount: number
  totalAmount: number
  validUntil: string
}

export interface CouponRequest {
  code: string
  name: string
  description?: string
  discountType: CouponDiscountType
  discountValue: number
  minimumSpend?: number
  maximumDiscount?: number
  validFrom: string
  validUntil: string
  usageLimit?: number
  perCustomerLimit?: number
  outletId?: string
  serviceId?: string
  packageId?: string
  isActive: boolean
}

export interface CouponValidationRequest {
  code: string
  outletId: string
  serviceId?: string
  packageId?: string
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
