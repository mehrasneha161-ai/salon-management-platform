import { useState, useEffect } from 'react'
import { Form, Select, DatePicker, Button, Card, Typography, Tag, Spin, Alert, Steps, Radio } from 'antd'
import { useNavigate } from 'react-router-dom'
import toast from 'react-hot-toast'
import dayjs from 'dayjs'
import { useGetOutletsQuery } from '../../features/outlet/outletApi'
import { useGetServicesQuery } from '../../features/service/serviceApi'
import { useGetStaffQuery } from '../../features/staff/staffApi'
import { useGetAvailableSlotsQuery, useCreateBookingMutation } from '../../features/booking/bookingApi'
import { socketService } from '../../services/socketService'
import { APP_ROUTES } from '../../constants'

const { Title, Text } = Typography

const BookingPage: React.FC = () => {
  const navigate = useNavigate()
  const [form] = Form.useForm()
  const [currentStep, setCurrentStep] = useState(0)
  const [selectedOutlet, setSelectedOutlet] = useState<string | undefined>()
  const [selectedStaff, setSelectedStaff] = useState<string | undefined>()
  const [selectedDate, setSelectedDate] = useState<string | undefined>()
  const [selectedSlot, setSelectedSlot] = useState<string | undefined>()
  const [selectedService, setSelectedService] = useState<string | undefined>()
  const [availableSlots, setAvailableSlots] = useState<string[]>([])

  const { data: outletsData } = useGetOutletsQuery()
  const { data: servicesData } = useGetServicesQuery({ outletId: selectedOutlet })
  const { data: staffData } = useGetStaffQuery({ outletId: selectedOutlet, status: 'AVAILABLE' })
  const { data: slotsData, refetch: refetchSlots } = useGetAvailableSlotsQuery(
    { outletId: selectedOutlet!, staffId: selectedStaff!, date: selectedDate! },
    { skip: !selectedOutlet || !selectedStaff || !selectedDate }
  )
  const [createBooking, { isLoading: isBooking }] = useCreateBookingMutation()

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

  const handleBooking = async () => {
    if (!selectedOutlet || !selectedStaff || !selectedDate || !selectedSlot || !selectedService) {
      toast.error('Please complete all selections')
      return
    }
    try {
      const result = await createBooking({
        outletId: selectedOutlet,
        staffId: selectedStaff,
        serviceId: selectedService,
        scheduledDate: selectedDate,
        scheduledTime: selectedSlot,
        sessionId: crypto.randomUUID(),
      }).unwrap()
      if (result.success) {
        toast.success(`Booking confirmed! Ref: ${result.data.bookingRef}`)
        navigate(APP_ROUTES.CUSTOMER.HISTORY)
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } }
      toast.error(error?.data?.message ?? 'Booking failed. Slot may have been taken.')
    }
  }

  const outlets = outletsData?.data ?? []
  const services = servicesData?.data ?? []
  const staff = staffData?.data ?? []

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
                onChange={(val) => { setSelectedOutlet(val); setSelectedStaff(undefined); setSelectedSlot(undefined) }}
                options={outlets.map((o) => ({ value: o.id, label: `${o.name} - ${o.city}` }))}
              />
            </div>
            <div>
              <Text strong>Select Service</Text>
              <Select
                className="w-full mt-2"
                placeholder="Choose a service"
                disabled={!selectedOutlet}
                onChange={setSelectedService}
                options={services.map((s) => ({ value: s.id, label: `${s.name} - ₹${s.price} (${s.durationMinutes}min)` }))}
              />
            </div>
            <Button type="primary" block disabled={!selectedOutlet || !selectedService}
              onClick={() => setCurrentStep(1)}>Next</Button>
          </div>
        )}
        {currentStep === 1 && (
          <div className="space-y-4">
            <div>
              <Text strong>Select Stylist</Text>
              <div className="grid grid-cols-2 gap-3 mt-2">
                {staff.map((s) => (
                  <Card
                    key={s.id}
                    hoverable
                    size="small"
                    className={`cursor-pointer ${selectedStaff === s.id ? 'border-amber-500 border-2' : ''}`}
                    onClick={() => setSelectedStaff(s.id)}
                  >
                    <div className="font-medium">{s.fullName}</div>
                    <div className="text-xs text-gray-500">{s.specialization}</div>
                    <Tag color={s.status === 'AVAILABLE' ? 'green' : 'orange'} className="mt-1">{s.status}</Tag>
                  </Card>
                ))}
              </div>
            </div>
            <div>
              <Text strong>Select Date</Text>
              <DatePicker
                className="w-full mt-2"
                disabledDate={(d) => d.isBefore(dayjs(), 'day')}
                onChange={(_, dateStr) => setSelectedDate(dateStr as string)}
              />
            </div>
            <div className="flex gap-2">
              <Button block onClick={() => setCurrentStep(0)}>Back</Button>
              <Button type="primary" block disabled={!selectedStaff || !selectedDate}
                onClick={() => setCurrentStep(2)}>Next</Button>
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
                onChange={(e) => setSelectedSlot(e.target.value)}
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
              <Button type="primary" block disabled={!selectedSlot}
                onClick={() => setCurrentStep(3)}>Next</Button>
            </div>
          </div>
        )}
        {currentStep === 3 && (
          <div className="space-y-4">
            <Title level={5}>Booking Summary</Title>
            <div className="bg-amber-50 p-4 rounded-lg space-y-2">
              <div><Text strong>Date:</Text> <Text>{selectedDate}</Text></div>
              <div><Text strong>Time:</Text> <Text>{selectedSlot}</Text></div>
              <div><Text strong>Service:</Text> <Text>{services.find(s => s.id === selectedService)?.name}</Text></div>
              <div><Text strong>Amount:</Text> <Text className="text-green-600 font-semibold">
                ₹{services.find(s => s.id === selectedService)?.price}
              </Text></div>
            </div>
            <Alert message="Your slot will be locked for 10 minutes after booking. Please complete payment promptly." type="info" showIcon />
            <div className="flex gap-2">
              <Button block onClick={() => setCurrentStep(2)}>Back</Button>
              <Button type="primary" block loading={isBooking} onClick={handleBooking}>
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
