import { useState } from 'react'
import { Table, Tag, Button, Typography, Empty, Spin, Modal, DatePicker, TimePicker, Space } from 'antd'
import dayjs, { Dayjs } from 'dayjs'
import {
  useGetMyBookingsQuery,
  useCancelBookingMutation,
  useRescheduleBookingMutation,
} from '../../features/booking/bookingApi'
import { Booking } from '../../types'
import { formatDate, formatCurrency, getStatusColor } from '../../utils/formatters'
import toast from 'react-hot-toast'

const { Title } = Typography

const RESCHEDULABLE = ['PENDING', 'SLOT_LOCKED', 'CONFIRMED']

const BookingHistoryPage: React.FC = () => {
  const { data, isLoading, refetch } = useGetMyBookingsQuery({ page: 0, size: 20 })
  const [cancelBooking, { isLoading: isCancelling }] = useCancelBookingMutation()
  const [rescheduleBooking, { isLoading: isRescheduling }] = useRescheduleBookingMutation()
  const bookings = data?.data?.content ?? []

  // Reschedule modal state
  const [target, setTarget] = useState<Booking | null>(null)
  const [newDate, setNewDate] = useState<Dayjs | null>(null)
  const [newTime, setNewTime] = useState<Dayjs | null>(null)

  const handleCancel = async (id: string) => {
    try {
      await cancelBooking(id).unwrap()
      toast.success('Booking cancelled')
      refetch()
    } catch {
      toast.error('Failed to cancel booking')
    }
  }

  const openReschedule = (booking: Booking) => {
    setTarget(booking)
    setNewDate(null)
    setNewTime(null)
  }

  const submitReschedule = async () => {
    if (!target || !newDate || !newTime) {
      toast.error('Please pick a new date and time')
      return
    }
    try {
      await rescheduleBooking({
        id: target.id,
        scheduledDate: newDate.format('YYYY-MM-DD'),
        scheduledTime: newTime.format('HH:mm'),
      }).unwrap()
      toast.success('Booking rescheduled — awaiting confirmation')
      setTarget(null)
      refetch()
    } catch {
      toast.error('Could not reschedule (slot may be taken). Try another time.')
    }
  }

  const columns = [
    { title: 'Ref', dataIndex: 'bookingRef', key: 'bookingRef', render: (v: string) => <code>{v}</code> },
    { title: 'Service', dataIndex: 'serviceName', key: 'serviceName', render: (v: string, r: Booking) => v ?? r.packageName ?? '-' },
    { title: 'Outlet', dataIndex: 'outletName', key: 'outletName' },
    { title: 'Stylist', dataIndex: 'staffName', key: 'staffName', render: (v: string) => v ?? 'TBD' },
    { title: 'Date', dataIndex: 'scheduledDate', key: 'scheduledDate', render: (v: string) => formatDate(v) },
    { title: 'Time', dataIndex: 'scheduledTime', key: 'scheduledTime' },
    { title: 'Amount', dataIndex: 'totalAmount', key: 'totalAmount', render: (v: number) => formatCurrency(v) },
    {
      title: 'Status', dataIndex: 'status', key: 'status',
      render: (v: string) => <Tag color={getStatusColor(v)}>{v}</Tag>
    },
    {
      title: 'Action', key: 'action',
      render: (_: unknown, record: Booking) =>
        RESCHEDULABLE.includes(record.status) ? (
          <Space>
            <Button size="small" onClick={() => openReschedule(record)}>Reschedule</Button>
            <Button danger size="small" loading={isCancelling} onClick={() => handleCancel(record.id)}>
              Cancel
            </Button>
          </Space>
        ) : null
    },
  ]

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <Title level={3}>My Bookings</Title>
      {bookings.length === 0 ? (
        <Empty description="No bookings yet. Book your first appointment!" />
      ) : (
        <Table
          dataSource={bookings}
          columns={columns}
          rowKey="id"
          scroll={{ x: 900 }}
          pagination={{ pageSize: 10 }}
        />
      )}

      <Modal
        title={target ? `Reschedule ${target.bookingRef}` : 'Reschedule'}
        open={!!target}
        onOk={submitReschedule}
        confirmLoading={isRescheduling}
        onCancel={() => setTarget(null)}
        okText="Reschedule"
      >
        <p className="mb-2 text-gray-500">Pick a new date and time. It will be sent for confirmation.</p>
        <Space direction="vertical" size="middle" style={{ width: '100%' }}>
          <DatePicker
            style={{ width: '100%' }}
            value={newDate}
            onChange={(d) => setNewDate(d)}
            disabledDate={(current) => current && current <= dayjs().endOf('day')}
          />
          <TimePicker
            style={{ width: '100%' }}
            value={newTime}
            onChange={(t) => setNewTime(t)}
            format="HH:mm"
            minuteStep={30}
          />
        </Space>
      </Modal>
    </div>
  )
}

export default BookingHistoryPage
