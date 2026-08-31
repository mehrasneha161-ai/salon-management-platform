import { Typography, Card, Alert, Row, Col, Statistic } from 'antd'
import { ClockCircleOutlined, CalendarOutlined } from '@ant-design/icons'
import { useSelector } from 'react-redux'
import { RootState } from '../../app/store'

const { Title, Text } = Typography

export default function StaffDashboard() {
  const { fullName } = useSelector((state: RootState) => state.auth)

  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Welcome, {fullName}! 👋</Title>
        <Text type="secondary">Your attendance and assigned bookings at a glance.</Text>
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Today" value={new Date().toLocaleDateString()} prefix={<CalendarOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Status" value="Available" prefix={<ClockCircleOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
      </Row>

      <Alert
        type="info"
        showIcon
        message="Attendance & assigned bookings"
        description="Check-in / check-out and your assigned bookings will appear here. This screen is scaffolded so the app builds and runs end-to-end; full functionality can be wired to the existing staff & booking APIs."
      />
    </div>
  )
}
