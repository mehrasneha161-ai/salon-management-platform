import { Table, Tag, Button, Typography, Empty, Spin } from 'antd'
import { useGetMyBookingsQuery, useCancelBookingMutation } from '../../features/booking/bookingApi'
import { Booking } from '../../types'
import { formatDate, formatCurrency, getStatusColor } from '../../utils/formatters'
import toast from 'react-hot-toast'

const { Title } = Typography

const BookingHistoryPage: React.FC = () => {
  const { data, isLoading, refetch } = useGetMyBookingsQuery({ page: 0, size: 20 })
  const [cancelBooking, { isLoading: isCancelling }] = useCancelBookingMutation()
  const bookings = data?.data?.content ?? []

  const handleCancel = async (id: string) => {
    try {
      await cancelBooking(id).unwrap()
      toast.success('Booking cancelled')
      refetch()
    } catch {
      toast.error('Failed to cancel booking')
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
        ['PENDING', 'SLOT_LOCKED', 'CONFIRMED'].includes(record.status) ? (
          <Button danger size="small" loading={isCancelling} onClick={() => handleCancel(record.id)}>
            Cancel
          </Button>
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
    </div>
  )
}

export default BookingHistoryPage
