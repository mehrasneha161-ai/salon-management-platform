import { Typography, Card, Alert } from 'antd'

const { Title, Text } = Typography

export default function AdminNotificationsPage() {
  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Notifications</Title>
        <Text type="secondary">WhatsApp broadcasts and marketing campaigns.</Text>
      </div>
      <Card>
        <Alert
          type="info"
          showIcon
          message="Broadcast &amp; campaigns"
          description="Compose broadcasts and marketing campaigns (backed by the notification API) here. Scaffolded so the app builds and runs end-to-end."
        />
      </Card>
    </div>
  )
}
