import { Table, Tag, Button, Typography, Space, Spin } from 'antd'
import { useState } from 'react'
import {
  useGetBookingsQuery,
  useApproveBookingMutation,
  useRejectBookingMutation,
  useCompleteBookingMutation,
} from '../../features/booking/bookingApi'
import { Booking } from '../../types'
import { formatDate, formatCurrency, getStatusColor } from '../../utils/formatters'
import toast from 'react-hot-toast'

const { Title } = Typography

export default function AdminBookingsPage() {
  const [page, setPage] = useState(0)
  const { data, isLoading, refetch } = useGetBookingsQuery({ page })
  const [approve, { isLoading: approving }] = useApproveBookingMutation()
  const [reject, { isLoading: rejecting }] = useRejectBookingMutation()
  const [complete, { isLoading: completing }] = useCompleteBookingMutation()

  const bookings = data?.data?.content ?? []
  const total = data?.data?.totalElements ?? 0

  const doApprove = async (r: Booking) => {
    if (!r.staffId) { toast.error('No stylist assigned to approve'); return }
    try { await approve({ id: r.id, staffId: r.staffId }).unwrap(); toast.success('Approved'); refetch() }
    catch { toast.error('Could not approve') }
  }
  const doReject = async (r: Booking) => {
    try { await reject({ id: r.id, reason: 'Rejected by admin' }).unwrap(); toast.success('Rejected'); refetch() }
    catch { toast.error('Could not reject') }
  }
  const doComplete = async (r: Booking) => {
    try { await complete(r.id).unwrap(); toast.success('Marked complete'); refetch() }
    catch { toast.error('Could not complete') }
  }

  const columns = [
    { title: 'Ref', dataIndex: 'bookingRef', key: 'bookingRef', render: (v: string) => <code>{v}</code> },
    { title: 'Customer', dataIndex: 'customerName', key: 'customerName' },
    { title: 'Stylist', dataIndex: 'staffName', key: 'staffName', render: (v: string) => v ?? 'TBD' },
    { title: 'Service', dataIndex: 'serviceName', key: 'serviceName', render: (v: string, r: Booking) => v ?? r.packageName ?? '-' },
    { title: 'Date', dataIndex: 'scheduledDate', key: 'scheduledDate', render: (v: string) => formatDate(v) },
    { title: 'Time', dataIndex: 'scheduledTime', key: 'scheduledTime' },
    { title: 'Amount', dataIndex: 'totalAmount', key: 'totalAmount', render: (v: number) => formatCurrency(v) },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color={getStatusColor(v)}>{v}</Tag> },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, r: Booking) => (
        <Space>
          {['PENDING', 'SLOT_LOCKED'].includes(r.status) && (
            <>
              <Button type="primary" size="small" loading={approving} onClick={() => doApprove(r)}>Approve</Button>
              <Button danger size="small" loading={rejecting} onClick={() => doReject(r)}>Reject</Button>
            </>
          )}
          {['CONFIRMED', 'IN_PROGRESS'].includes(r.status) && (
            <Button size="small" loading={completing} onClick={() => doComplete(r)}>Complete</Button>
          )}
        </Space>
      ),
    },
  ]

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <Title level={3}>Bookings</Title>
      <Table
        dataSource={bookings}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1000 }}
        pagination={{
          current: page + 1,
          total,
          pageSize: 20,
          onChange: (p) => setPage(p - 1),
        }}
      />
    </div>
  )
}
