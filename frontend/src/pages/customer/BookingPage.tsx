import { useEffect, useRef, useState } from 'react'
import {
  Alert,
  Button,
  Card,
  DatePicker,
  Input,
  Radio,
  Select,
  Space,
  Steps,
  Tag,
  Typography,
} from 'antd'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import dayjs from 'dayjs'
import { useGetOutletsQuery } from '../../features/outlet/outletApi'
import { useGetServicesQuery } from '../../features/service/serviceApi'
import { useGetStaffQuery } from '../../features/staff/staffApi'
import { useGetAvailableSlotsQuery, useCreateBookingMutation } from '../../features/booking/bookingApi'
import { useValidateCouponMutation } from '../../features/coupon/couponApi'
import type { CouponValidationResponse, CreateBookingRequest } from '../../types'
import { socketService } from '../../services/socketService'
import { APP_ROUTES } from '../../constants'
import { formatCurrency } from '../../utils/formatters'

const { Title, Text } = Typography

const getApiErrorMessage = (error: unknown, fallback: string) => {
  const apiError = error as { data?: { message?: string } }
  return apiError.data?.message ?? fallback
}

const BookingPage: React.FC = () => {
  const navigate = useNavigate()
  const [currentStep, setCurrentStep] = useState(0)
  const [selectedOutlet, setSelectedOutlet] = useState<string>()
  const [selectedStaff, setSelectedStaff] = useState<string>()
  const [selectedDate, setSelectedDate] = useState<string>()
  const [selectedSlot, setSelectedSlot] = useState<string>()
  const [selectedService, setSelectedService] = useState<string>()
  const [availableSlots, setAvailableSlots] = useState<string[]>([])
  const [couponCode, setCouponCode] = useState('')
  const [appliedCouponCode, setAppliedCouponCode] = useState<string>()
  const [couponQuote, setCouponQuote] = useState<CouponValidationResponse>()
  const [couponError, setCouponError] = useState<string>()
  const couponValidationVersion = useRef(0)

  const { data: outletsData } = useGetOutletsQuery()
  const { data: servicesData } = useGetServicesQuery({ outletId: selectedOutlet })
  const { data: staffData } = useGetStaffQuery({ outletId: selectedOutlet, status: 'AVAILABLE' })
  const { data: slotsData, refetch: refetchSlots } = useGetAvailableSlotsQuery(
    { outletId: selectedOutlet!, staffId: selectedStaff!, date: selectedDate! },
    { skip: !selectedOutlet || !selectedStaff || !selectedDate }
  )
  const [createBooking, { isLoading: isBooking }] = useCreateBookingMutation()
  const [validateCoupon, { isLoading: isValidatingCoupon }] = useValidateCouponMutation()

  useEffect(() => {
    if (slotsData?.data) setAvailableSlots(slotsData.data)
  }, [slotsData])

  useEffect(() => {
    if (selectedOutlet && selectedDate) {
      socketService.connect(() => {
        socketService.subscribeToSlots(selectedOutlet, selectedDate, () => {
          refetchSlots()
          toast('Slot availability updated!', { icon: 'ℹ️' })
        })
      })
    }
    return () => {
      if (selectedOutlet && selectedDate) {
        socketService.unsubscribe(selectedOutlet, selectedDate)
      }
    }
  }, [selectedOutlet, selectedDate])

  const clearAppliedCoupon = (clearInput = false) => {
    couponValidationVersion.current += 1
    setAppliedCouponCode(undefined)
    setCouponQuote(undefined)
    setCouponError(undefined)
    if (clearInput) setCouponCode('')
  }

  const handleOutletChange = (outletId: string) => {
    setSelectedOutlet(outletId)
    setSelectedService(undefined)
    setSelectedStaff(undefined)
    setSelectedDate(undefined)
    setSelectedSlot(undefined)
    setAvailableSlots([])
    clearAppliedCoupon(true)
  }

  const handleServiceChange = (serviceId: string) => {
    setSelectedService(serviceId)
    clearAppliedCoupon(true)
  }

  const handleCouponInputChange = (value: string) => {
    setCouponCode(value)
    if (appliedCouponCode || couponQuote) clearAppliedCoupon()
    else setCouponError(undefined)
  }

  const handleApplyCoupon = async () => {
    if (!selectedOutlet || !selectedService) return

    const normalizedCode = couponCode.trim().toUpperCase()
    if (!normalizedCode) {
      setCouponError('Enter a coupon code')
      return
    }

    setCouponError(undefined)
    const validationVersion = couponValidationVersion.current + 1
    couponValidationVersion.current = validationVersion
    try {
      const result = await validateCoupon({
        code: normalizedCode,
        outletId: selectedOutlet,
        serviceId: selectedService,
      }).unwrap()
      if (validationVersion !== couponValidationVersion.current) return
      setCouponCode(result.data.code)
      setAppliedCouponCode(result.data.code)
      setCouponQuote(result.data)
      toast.success(`${result.data.code} applied`)
    } catch (error: unknown) {
      if (validationVersion !== couponValidationVersion.current) return
      const message = getApiErrorMessage(error, 'Coupon could not be applied')
      clearAppliedCoupon()
      setCouponError(message)
      toast.error(message)
    }
  }

  const handleBooking = async () => {
    if (isValidatingCoupon) return
    if (!selectedOutlet || !selectedStaff || !selectedDate || !selectedSlot || !selectedService) {
      toast.error('Please complete all selections')
      return
    }
    try {
      const bookingRequest: CreateBookingRequest = couponQuote
        ? {
            outletId: selectedOutlet,
            staffId: selectedStaff,
            serviceId: selectedService,
            scheduledDate: selectedDate,
            scheduledTime: selectedSlot,
            sessionId: crypto.randomUUID(),
            couponCode: couponQuote.code,
            expectedCouponId: couponQuote.couponId,
            expectedSubtotalAmount: couponQuote.subtotalAmount,
            expectedDiscountAmount: couponQuote.discountAmount,
            expectedTotalAmount: couponQuote.totalAmount,
          }
        : {
            outletId: selectedOutlet,
            staffId: selectedStaff,
            serviceId: selectedService,
            scheduledDate: selectedDate,
            scheduledTime: selectedSlot,
            sessionId: crypto.randomUUID(),
          }
      const result = await createBooking(bookingRequest).unwrap()
      if (result.success) {
        const booking = result.data
        if (Number(booking.totalAmount) === 0 || booking.status === 'CONFIRMED') {
          toast.success(`Booking confirmed! Ref: ${booking.bookingRef}`)
        } else {
          toast.success(`Slot reserved! Ref: ${booking.bookingRef}. Continue to payment.`)
        }
        navigate(APP_ROUTES.CUSTOMER.HISTORY)
      }
    } catch (error: unknown) {
      toast.error(getApiErrorMessage(error, 'Booking failed. Slot may have been taken.'))
    }
  }

  const outlets = outletsData?.data ?? []
  const services = servicesData?.data ?? []
  const staff = staffData?.data ?? []
  const selectedServiceDetails = services.find((service) => service.id === selectedService)
  const subtotal = couponQuote?.subtotalAmount ?? selectedServiceDetails?.price ?? 0
  const finalTotal = couponQuote?.totalAmount ?? subtotal

  return (
    <div className="max-w-2xl mx-auto">
      <Title level={3}>Book an Appointment</Title>
      <Steps
        current={currentStep}
        className="mb-6"
        items={[
          { title: 'Outlet & Service' },
          { title: 'Staff & Date' },
          { title: 'Time Slot' },
          { title: 'Confirm' },
        ]}
      />
      <Card className="shadow-sm">
        {currentStep === 0 && (
          <div className="space-y-4">
            <div>
              <Text strong>Select Outlet</Text>
              <Select
                className="w-full mt-2"
                placeholder="Choose a branch"
                value={selectedOutlet}
                onChange={handleOutletChange}
                options={outlets.map((outlet) => ({
                  value: outlet.id,
                  label: `${outlet.name} - ${outlet.city}`,
                }))}
              />
            </div>
            <div>
              <Text strong>Select Service</Text>
              <Select
                className="w-full mt-2"
                placeholder="Choose a service"
                disabled={!selectedOutlet}
                value={selectedService}
                onChange={handleServiceChange}
                options={services.map((service) => ({
                  value: service.id,
                  label: `${service.name} - ₹${service.price} (${service.durationMinutes}min)`,
                }))}
              />
            </div>
            <Button
              type="primary"
              block
              disabled={!selectedOutlet || !selectedService}
              onClick={() => setCurrentStep(1)}
            >
              Next
            </Button>
          </div>
        )}
        {currentStep === 1 && (
          <div className="space-y-4">
            <div>
              <Text strong>Select Stylist</Text>
              <div className="grid grid-cols-2 gap-3 mt-2">
                {staff.map((member) => (
                  <Card
                    key={member.id}
                    hoverable
                    size="small"
                    className={`cursor-pointer ${selectedStaff === member.id ? 'border-amber-500 border-2' : ''}`}
                    onClick={() => setSelectedStaff(member.id)}
                  >
                    <div className="font-medium">{member.fullName}</div>
                    <div className="text-xs text-gray-500">{member.specialization}</div>
                    <Tag color={member.status === 'AVAILABLE' ? 'green' : 'orange'} className="mt-1">
                      {member.status}
                    </Tag>
                  </Card>
                ))}
              </div>
            </div>
            <div>
              <Text strong>Select Date</Text>
              <DatePicker
                className="w-full mt-2"
                value={selectedDate ? dayjs(selectedDate) : null}
                disabledDate={(date) => date.isBefore(dayjs(), 'day')}
                onChange={(_, dateString) => {
                  setSelectedDate(dateString as string)
                  setSelectedSlot(undefined)
                }}
              />
            </div>
            <div className="flex gap-2">
              <Button block onClick={() => setCurrentStep(0)}>Back</Button>
              <Button
                type="primary"
                block
                disabled={!selectedStaff || !selectedDate}
                onClick={() => setCurrentStep(2)}
              >
                Next
              </Button>
            </div>
          </div>
        )}
        {currentStep === 2 && (
          <div className="space-y-4">
            <Text strong>Available Time Slots</Text>
            {availableSlots.length === 0 ? (
              <Alert message="No slots available for selected date and stylist" type="warning" />
            ) : (
              <Radio.Group
                className="w-full"
                onChange={(event) => setSelectedSlot(event.target.value)}
                value={selectedSlot}
              >
                <div className="grid grid-cols-3 gap-2 mt-2">
                  {availableSlots.map((slot) => (
                    <Radio.Button key={slot} value={slot} className="text-center">
                      {slot}
                    </Radio.Button>
                  ))}
                </div>
              </Radio.Group>
            )}
            <div className="flex gap-2">
              <Button block onClick={() => setCurrentStep(1)}>Back</Button>
              <Button type="primary" block disabled={!selectedSlot} onClick={() => setCurrentStep(3)}>
                Next
              </Button>
            </div>
          </div>
        )}
        {currentStep === 3 && (
          <div className="space-y-4">
            <Title level={5}>Booking Summary</Title>
            <div className="bg-amber-50 p-4 rounded-lg space-y-2">
              <div><Text strong>Date:</Text> <Text>{selectedDate}</Text></div>
              <div><Text strong>Time:</Text> <Text>{selectedSlot}</Text></div>
              <div><Text strong>Service:</Text> <Text>{selectedServiceDetails?.name}</Text></div>
              <div className="flex justify-between">
                <Text strong>Subtotal:</Text>
                <Text>{formatCurrency(subtotal)}</Text>
              </div>
              {couponQuote && (
                <div className="flex justify-between">
                  <Text strong>Discount ({appliedCouponCode}):</Text>
                  <Text type="success">-{formatCurrency(couponQuote.discountAmount)}</Text>
                </div>
              )}
              <div className="flex justify-between">
                <Text strong>Total:</Text>
                <Text className="text-green-600 font-semibold">{formatCurrency(finalTotal)}</Text>
              </div>
            </div>

            {selectedOutlet && selectedService && (
              <div>
                <Text strong>Coupon code</Text>
                <Space.Compact className="w-full mt-2">
                  <Input
                    value={couponCode}
                    maxLength={50}
                    disabled={isValidatingCoupon}
                    placeholder="Enter coupon code"
                    onChange={(event) => handleCouponInputChange(event.target.value)}
                    onPressEnter={handleApplyCoupon}
                  />
                  {couponQuote ? (
                    <Button onClick={() => clearAppliedCoupon(true)}>Remove</Button>
                  ) : (
                    <Button
                      type="primary"
                      loading={isValidatingCoupon}
                      disabled={!couponCode.trim()}
                      onClick={handleApplyCoupon}
                    >
                      Apply
                    </Button>
                  )}
                </Space.Compact>
                {couponQuote && (
                  <Alert
                    className="mt-2"
                    type="success"
                    showIcon
                    message={`${appliedCouponCode} applied — you save ${formatCurrency(couponQuote.discountAmount)}`}
                  />
                )}
                {couponError && (
                  <Alert className="mt-2" type="error" showIcon message={couponError} />
                )}
              </div>
            )}

            <Alert
              message="Your slot will be locked for 10 minutes after booking. Please complete payment promptly."
              type="info"
              showIcon
            />
            <div className="flex gap-2">
              <Button block onClick={() => setCurrentStep(2)}>Back</Button>
              <Button
                type="primary"
                block
                loading={isBooking}
                disabled={isValidatingCoupon}
                onClick={handleBooking}
              >
                Confirm Booking
              </Button>
            </div>
          </div>
        )}
      </Card>
    </div>
  )
}

export default BookingPage
