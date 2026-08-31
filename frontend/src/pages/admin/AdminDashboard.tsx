import { Typography, Card, Row, Col, Statistic, Spin } from 'antd'
import { CalendarOutlined, ShopOutlined, BarChartOutlined } from '@ant-design/icons'
import { useGetOutletPerformanceQuery } from '../../features/analytics/analyticsApi'
import { formatCurrency } from '../../utils/formatters'

const { Title, Text } = Typography

export default function AdminDashboard() {
  const { data, isLoading } = useGetOutletPerformanceQuery()
  const rows = data?.data ?? []
  const totalRevenue = rows.reduce((s, r) => s + (r.totalRevenue ?? 0), 0)
  const totalBookings = rows.reduce((s, r) => s + (r.totalBookings ?? 0), 0)

  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Admin Overview</Title>
        <Text type="secondary">Business at a glance (from completed bookings).</Text>
      </div>
      {isLoading ? <Spin size="large" /> : (
        <Row gutter={[16, 16]}>
          <Col xs={24} sm={8}>
            <Card className="text-center shadow-sm">
              <Statistic title="Total Revenue" value={formatCurrency(totalRevenue)} prefix={<BarChartOutlined />} />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card className="text-center shadow-sm">
              <Statistic title="Completed Bookings" value={totalBookings} prefix={<CalendarOutlined />} />
            </Card>
          </Col>
          <Col xs={24} sm={8}>
            <Card className="text-center shadow-sm">
              <Statistic title="Active Outlets" value={rows.length} prefix={<ShopOutlined />} />
            </Card>
          </Col>
        </Row>
      )}
    </div>
  )
}
