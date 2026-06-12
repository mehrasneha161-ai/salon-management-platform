import { Row, Col, Card, Select, Image, Typography, Empty, Spin, Tag } from 'antd'
import { useState } from 'react'
import { useGetGalleryQuery } from '../../features/gallery/galleryApi'

const { Title, Text } = Typography

const CATEGORIES = [
  { value: '', label: 'All Categories' },
  { value: 'hair', label: 'Hair' },
  { value: 'skin', label: 'Skin' },
  { value: 'nails', label: 'Nails' },
  { value: 'makeup', label: 'Makeup' },
]

const GalleryPage: React.FC = () => {
  const [categoryId, setCategoryId] = useState<string | undefined>()
  const { data, isLoading } = useGetGalleryQuery({ categoryId })
  const items = data?.data ?? []

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <Title level={3}>Before & After Gallery</Title>
        <Select
          placeholder="Filter by category"
          allowClear
          style={{ width: 200 }}
          onChange={setCategoryId}
          options={CATEGORIES}
        />
      </div>
      {items.length === 0 ? <Empty description="No gallery items yet" /> : (
        <Row gutter={[16, 16]}>
          {items.map((item) => (
            <Col key={item.id} xs={24} sm={12} lg={8}>
              <Card
                className="shadow-sm"
                title={item.title ?? 'Transformation'}
                extra={<Tag color="purple">{item.categoryName}</Tag>}
              >
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <Text type="secondary" className="text-xs block mb-1">BEFORE</Text>
                    <Image src={item.beforeUrl} alt="Before" className="rounded-lg w-full object-cover" height={150} />
                  </div>
                  <div>
                    <Text type="secondary" className="text-xs block mb-1">AFTER</Text>
                    <Image src={item.afterUrl} alt="After" className="rounded-lg w-full object-cover" height={150} />
                  </div>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  )
}

export default GalleryPage
