import { Typography, Card, Alert } from 'antd'

const { Title, Text } = Typography

export default function AdminAnalyticsPage() {
  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Analytics</Title>
        <Text type="secondary">Outlet revenue and popular services.</Text>
      </div>
      <Card>
        <Alert
          type="info"
          showIcon
          message="Business analytics"
          description="Revenue-by-outlet and popular-services charts (backed by the analytics API) will render here. Scaffolded so the app builds and runs end-to-end."
        />
      </Card>
    </div>
  )
}
