import { Row, Col, Card, Statistic, Typography, Button } from 'antd'
import { CalendarOutlined, HistoryOutlined, PictureOutlined, ShopOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import { RootState } from '../../app/store'
import { useGetMyBookingsQuery } from '../../features/booking/bookingApi'
import { APP_ROUTES } from '../../constants'

const { Title, Text } = Typography

const CustomerDashboard: React.FC = () => {
  const navigate = useNavigate()
  const { fullName } = useSelector((state: RootState) => state.auth)
  const { data: bookingsData } = useGetMyBookingsQuery({ page: 0, size: 5 })

  const totalBookings = bookingsData?.data?.totalElements ?? 0
  const upcomingBookings = bookingsData?.data?.content?.filter(
    (b) => b.status === 'CONFIRMED' || b.status === 'PENDING'
  ).length ?? 0

  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Welcome back, {fullName}! 👋</Title>
        <Text type="secondary">Manage your salon appointments and explore our services.</Text>
      </div>
      <Row gutter={[16, 16]} className="mb-6">
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Total Bookings" value={totalBookings} prefix={<HistoryOutlined />} />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card className="text-center shadow-sm">
            <Statistic title="Upcoming" value={upcomingBookings} prefix={<CalendarOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
      </Row>
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable className="text-center booking-card" onClick={() => navigate(APP_ROUTES.CUSTOMER.BOOK)}>
            <CalendarOutlined className="text-4xl text-amber-500 mb-3" />
            <div className="font-semibold text-lg">Book Appointment</div>
            <Text type="secondary">Schedule your next visit</Text>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable className="text-center booking-card" onClick={() => navigate(APP_ROUTES.CUSTOMER.OUTLETS)}>
            <ShopOutlined className="text-4xl text-blue-500 mb-3" />
            <div className="font-semibold text-lg">Our Outlets</div>
            <Text type="secondary">Find a branch near you</Text>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable className="text-center booking-card" onClick={() => navigate(APP_ROUTES.CUSTOMER.GALLERY)}>
            <PictureOutlined className="text-4xl text-purple-500 mb-3" />
            <div className="font-semibold text-lg">Gallery</div>
            <Text type="secondary">Before & After transformations</Text>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card hoverable className="text-center booking-card" onClick={() => navigate(APP_ROUTES.CUSTOMER.HISTORY)}>
            <HistoryOutlined className="text-4xl text-green-500 mb-3" />
            <div className="font-semibold text-lg">My Bookings</div>
            <Text type="secondary">View your history</Text>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default CustomerDashboard
