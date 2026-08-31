import { Typography, Card, Alert } from 'antd'

const { Title, Text } = Typography

export default function AdminServicesPage() {
  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Services &amp; Packages</Title>
        <Text type="secondary">Manage service categories, services and packages.</Text>
      </div>
      <Card>
        <Alert
          type="info"
          showIcon
          message="Service catalog"
          description="CRUD for categories, services and packages (backed by the service API) will render here. Scaffolded so the app builds and runs end-to-end."
        />
      </Card>
    </div>
  )
}
