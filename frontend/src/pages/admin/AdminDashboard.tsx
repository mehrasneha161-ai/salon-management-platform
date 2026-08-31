import { Typography, Card, Row, Col, Statistic, Alert } from 'antd'
import { CalendarOutlined, TeamOutlined, ShopOutlined, BarChartOutlined } from '@ant-design/icons'

const { Title, Text } = Typography

export default function AdminDashboard() {
  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Admin Overview</Title>
        <Text type="secondary">Your salon business at a glance.</Text>
      </div>

      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Bookings" value="—" prefix={<CalendarOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Staff" value="—" prefix={<TeamOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Outlets" value="—" prefix={<ShopOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Revenue" value="—" prefix={<BarChartOutlined />} />
          </Card>
        </Col>
      </Row>

      <Alert
        type="info"
        showIcon
        message="Dashboard"
        description="Live KPIs (revenue, bookings, popular services) will render here from the analytics API. This screen is scaffolded so the app builds and runs end-to-end."
      />
    </div>
  )
}
