import { Typography, Card, Table, Button, Space, Spin, Empty, Tag } from 'antd'
import { LoginOutlined, LogoutOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import {
  useGetMyStaffProfileQuery,
  useGetAttendanceQuery,
  useCheckInMutation,
  useCheckOutMutation,
} from '../../features/staff/staffApi'
import { formatDate } from '../../utils/formatters'
import toast from 'react-hot-toast'

const { Title, Text } = Typography

interface AttendanceRow {
  id: string
  date: string
  checkInAt?: string
  checkOutAt?: string
  status: string
}

export default function StaffAttendancePage() {
  const { data: profileData, isLoading: loadingProfile } = useGetMyStaffProfileQuery()
  const profile = profileData?.data

  const { data: attendanceData, isLoading: loadingAttendance, refetch } = useGetAttendanceQuery(
    { id: profile?.id ?? '' },
    { skip: !profile?.id }
  )

  const [checkIn, { isLoading: checkingIn }] = useCheckInMutation()
  const [checkOut, { isLoading: checkingOut }] = useCheckOutMutation()

  const rows = (attendanceData?.data ?? []) as unknown as AttendanceRow[]

  const doCheckIn = async () => {
    try { await checkIn().unwrap(); toast.success('Checked in'); refetch() }
    catch { toast.error('Could not check in (already checked in today?)') }
  }
  const doCheckOut = async () => {
    try { await checkOut().unwrap(); toast.success('Checked out'); refetch() }
    catch { toast.error('Could not check out (no check-in today?)') }
  }

  const columns = [
    { title: 'Date', dataIndex: 'date', key: 'date', render: (v: string) => formatDate(v) },
    {
      title: 'Check in', dataIndex: 'checkInAt', key: 'checkInAt',
      render: (v?: string) => (v ? dayjs(v).format('hh:mm A') : '-'),
    },
    {
      title: 'Check out', dataIndex: 'checkOutAt', key: 'checkOutAt',
      render: (v?: string) => (v ? dayjs(v).format('hh:mm A') : '-'),
    },
    {
      title: 'Status', dataIndex: 'status', key: 'status',
      render: (v: string) => <Tag color={v === 'PRESENT' ? 'green' : 'default'}>{v}</Tag>,
    },
  ]

  if (loadingProfile) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <div className="mb-6">
        <Title level={3}>My Attendance</Title>
        <Text type="secondary">Mark today's attendance and review your history.</Text>
      </div>

      <Card className="mb-6">
        <Space>
          <Button type="primary" icon={<LoginOutlined />} loading={checkingIn} onClick={doCheckIn}>
            Check in
          </Button>
          <Button icon={<LogoutOutlined />} loading={checkingOut} onClick={doCheckOut}>
            Check out
          </Button>
        </Space>
      </Card>

      <Card title={`History (${rows.length} day${rows.length === 1 ? '' : 's'})`}>
        {loadingAttendance ? <Spin /> : rows.length === 0 ? (
          <Empty description="No attendance records yet." />
        ) : (
          <Table dataSource={rows} columns={columns} rowKey="id" pagination={{ pageSize: 15 }} />
        )}
      </Card>
    </div>
  )
}
