import { Typography, Card, Alert } from 'antd'

const { Title, Text } = Typography

export default function AdminBookingsPage() {
  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Bookings</Title>
        <Text type="secondary">Review, approve or reject customer bookings.</Text>
      </div>
      <Card>
        <Alert
          type="info"
          showIcon
          message="Booking management"
          description="A filterable table of bookings with approve / reject / complete actions (backed by the bookings API) will render here. Scaffolded so the app builds and runs end-to-end."
        />
      </Card>
    </div>
  )
}
