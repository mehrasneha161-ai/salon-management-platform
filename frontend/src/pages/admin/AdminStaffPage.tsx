import { Typography, Card, Alert } from 'antd'

const { Title, Text } = Typography

export default function AdminStaffPage() {
  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Staff</Title>
        <Text type="secondary">Register staff, manage profiles and availability.</Text>
      </div>
      <Card>
        <Alert
          type="info"
          showIcon
          message="Staff management"
          description="Staff list, registration form and status controls (backed by the staff API) will render here. Scaffolded so the app builds and runs end-to-end."
        />
      </Card>
    </div>
  )
}
