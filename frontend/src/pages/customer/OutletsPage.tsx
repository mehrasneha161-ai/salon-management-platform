import { Row, Col, Card, Tag, Typography, Empty, Spin } from 'antd'
import { EnvironmentOutlined, PhoneOutlined } from '@ant-design/icons'
import { useGetOutletsQuery } from '../../features/outlet/outletApi'

const { Title, Text } = Typography

const OutletsPage: React.FC = () => {
  const { data, isLoading } = useGetOutletsQuery()
  const outlets = data?.data ?? []

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <Title level={3}>Our Outlets</Title>
      <Text type="secondary" className="mb-6 block">Find a branch near you and book your appointment.</Text>
      {outlets.length === 0 ? <Empty description="No outlets available" /> : (
        <Row gutter={[16, 16]}>
          {outlets.map((outlet) => (
            <Col key={outlet.id} xs={24} sm={12} lg={8}>
              <Card hoverable className="shadow-sm h-full">
                <div className="flex items-start justify-between mb-3">
                  <Title level={5} className="!mb-0">{outlet.name}</Title>
                  <Tag color={outlet.isActive ? 'green' : 'red'}>
                    {outlet.isActive ? 'Open' : 'Closed'}
                  </Tag>
                </div>
                <div className="flex items-center gap-2 text-gray-600 mb-2">
                  <EnvironmentOutlined />
                  <Text>{outlet.address}, {outlet.city}</Text>
                </div>
                {outlet.phone && (
                  <div className="flex items-center gap-2 text-gray-600">
                    <PhoneOutlined />
                    <Text>{outlet.phone}</Text>
                  </div>
                )}
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  )
}

export default OutletsPage
