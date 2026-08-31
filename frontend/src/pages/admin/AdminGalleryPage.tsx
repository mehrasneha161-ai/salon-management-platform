import { Typography, Card, Alert } from 'antd'

const { Title, Text } = Typography

export default function AdminGalleryPage() {
  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Gallery</Title>
        <Text type="secondary">Manage before/after gallery items.</Text>
      </div>
      <Card>
        <Alert
          type="info"
          showIcon
          message="Gallery management"
          description="Upload and publish before/after images (backed by the gallery API + S3) here. Scaffolded so the app builds and runs end-to-end."
        />
      </Card>
    </div>
  )
}
