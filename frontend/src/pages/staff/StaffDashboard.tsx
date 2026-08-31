import { Typography, Card, Row, Col, Statistic, Button, Space, Select, Table, Tag, Spin, Empty } from 'antd'
import { ClockCircleOutlined, LoginOutlined, LogoutOutlined, CalendarOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  useGetMyStaffProfileQuery,
  useCheckInMutation,
  useCheckOutMutation,
  useUpdateStaffStatusMutation,
} from '../../features/staff/staffApi'
import {
  useGetAssignedBookingsQuery,
  useCompleteBookingMutation,
} from '../../features/booking/bookingApi'
import { Booking } from '../../types'
import { formatDate, formatCurrency, getStatusColor } from '../../utils/formatters'
import toast from 'react-hot-toast'

const { Title, Text } = Typography

const STATUS_OPTIONS = ['AVAILABLE', 'BUSY', 'OFF_DUTY']

export default function StaffDashboard() {
  const { data: profileData, isLoading: loadingProfile, refetch: refetchProfile } = useGetMyStaffProfileQuery()
  const today = dayjs().format('YYYY-MM-DD')
  const { data: bookingsData, isLoading: loadingBookings, refetch: refetchBookings } =
    useGetAssignedBookingsQuery({ date: today })

  const [checkIn, { isLoading: checkingIn }] = useCheckInMutation()
  const [checkOut, { isLoading: checkingOut }] = useCheckOutMutation()
  const [updateStatus] = useUpdateStaffStatusMutation()
  const [complete, { isLoading: completing }] = useCompleteBookingMutation()

  const profile = profileData?.data
  const bookings = bookingsData?.data ?? []

  const doCheckIn = async () => {
    try { await checkIn().unwrap(); toast.success('Checked in'); refetchProfile() }
    catch { toast.error('Could not check in (already checked in today?)') }
  }
  const doCheckOut = async () => {
    try { await checkOut().unwrap(); toast.success('Checked out'); refetchProfile() }
    catch { toast.error('Could not check out (no check-in today?)') }
  }
  const changeStatus = async (status: string) => {
    if (!profile) return
    try { await updateStatus({ id: profile.id, status }).unwrap(); toast.success('Status updated'); refetchProfile() }
    catch { toast.error('Could not update status') }
  }
  const doComplete = async (b: Booking) => {
    try { await complete(b.id).unwrap(); toast.success('Booking completed'); refetchBookings() }
    catch { toast.error('Could not complete booking') }
  }

  const columns = [
    { title: 'Ref', dataIndex: 'bookingRef', key: 'bookingRef', render: (v: string) => <code>{v}</code> },
    { title: 'Customer', dataIndex: 'customerName', key: 'customerName' },
    { title: 'Phone', dataIndex: 'customerPhone', key: 'customerPhone' },
    { title: 'Service', dataIndex: 'serviceName', key: 'serviceName', render: (v: string, r: Booking) => v ?? r.packageName ?? '-' },
    { title: 'Time', dataIndex: 'scheduledTime', key: 'scheduledTime' },
    { title: 'Amount', dataIndex: 'totalAmount', key: 'totalAmount', render: (v: number) => formatCurrency(v) },
    { title: 'Status', dataIndex: 'status', key: 'status', render: (v: string) => <Tag color={getStatusColor(v)}>{v}</Tag> },
    {
      title: 'Action', key: 'action',
      render: (_: unknown, r: Booking) =>
        ['CONFIRMED', 'IN_PROGRESS'].includes(r.status) ? (
          <Button size="small" type="primary" loading={completing} onClick={() => doComplete(r)}>Complete</Button>
        ) : null,
    },
  ]

  if (loadingProfile) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Welcome, {profile?.fullName ?? 'Staff'}! 👋</Title>
        <Text type="secondary">
          {profile?.outletName ? `${profile.outletName} · ` : ''}
          {profile?.specialization || 'Stylist'}
        </Text>
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Today" value={dayjs().format('DD MMM YYYY')} prefix={<CalendarOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Present days" value={profile?.totalPresentDays ?? 0} prefix={<ClockCircleOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Today's bookings" value={bookings.length} prefix={<CalendarOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="shadow-sm">
            <Text type="secondary" className="block mb-2">My status</Text>
            <Select
              value={profile?.status}
              style={{ width: '100%' }}
              options={STATUS_OPTIONS.map((s) => ({ value: s, label: s }))}
              onChange={changeStatus}
            />
          </Card>
        </Col>
      </Row>

      <Card title="Attendance" className="mb-6">
        <Space>
          <Button type="primary" icon={<LoginOutlined />} loading={checkingIn} onClick={doCheckIn}>
            Check in
          </Button>
          <Button icon={<LogoutOutlined />} loading={checkingOut} onClick={doCheckOut}>
            Check out
          </Button>
        </Space>
        <div className="mt-2">
          <Text type="secondary" className="text-xs">
            Check in once at the start of your shift and check out when you finish.
          </Text>
        </div>
      </Card>

      <Card title={`Today's assigned bookings (${formatDate(today)})`}>
        {loadingBookings ? <Spin /> : bookings.length === 0 ? (
          <Empty description="No bookings assigned for today." />
        ) : (
          <Table dataSource={bookings} columns={columns} rowKey="id" scroll={{ x: 900 }} pagination={false} />
        )}
      </Card>
    </div>
  )
}
