import { Typography, Card, Table, Spin, Row, Col } from 'antd'
import {
  useGetOutletPerformanceQuery,
  useGetPopularServicesQuery,
} from '../../features/analytics/analyticsApi'
import { OutletRevenue, PopularService } from '../../types'
import { formatCurrency } from '../../utils/formatters'

const { Title } = Typography

export default function AdminAnalyticsPage() {
  const { data: revData, isLoading: loadingRev } = useGetOutletPerformanceQuery()
  const { data: popData, isLoading: loadingPop } = useGetPopularServicesQuery()

  const revenue = revData?.data ?? []
  const popular = popData?.data ?? []

  const revColumns = [
    { title: 'Outlet', dataIndex: 'outletName', key: 'outletName' },
    { title: 'Bookings', dataIndex: 'totalBookings', key: 'totalBookings' },
    { title: 'Revenue', dataIndex: 'totalRevenue', key: 'totalRevenue', render: (v: number) => formatCurrency(v) },
  ]
  const popColumns = [
    { title: 'Service', dataIndex: 'serviceName', key: 'serviceName' },
    { title: 'Category', dataIndex: 'categoryName', key: 'categoryName' },
    { title: 'Bookings', dataIndex: 'bookingCount', key: 'bookingCount' },
  ]

  return (
    <div>
      <Title level={3}>Analytics</Title>
      <Row gutter={[16, 16]}>
        <Col xs={24} lg={12}>
          <Card title="Revenue by outlet (completed bookings)">
            {loadingRev ? <Spin /> : (
              <Table<OutletRevenue> dataSource={revenue} columns={revColumns} rowKey="outletId" pagination={false} size="small" />
            )}
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="Popular services">
            {loadingPop ? <Spin /> : (
              <Table<PopularService> dataSource={popular} columns={popColumns} rowKey="serviceId" pagination={false} size="small" />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  )
}
