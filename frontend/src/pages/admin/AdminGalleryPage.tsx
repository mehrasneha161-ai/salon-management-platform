import { Typography, Card, Row, Col, Button, Empty, Spin, Image, Popconfirm } from 'antd'
import { DeleteOutlined } from '@ant-design/icons'
import {
  useGetGalleryQuery,
  useDeleteGalleryItemMutation,
} from '../../features/gallery/galleryApi'
import { formatDate } from '../../utils/formatters'
import toast from 'react-hot-toast'

const { Title, Text } = Typography

export default function AdminGalleryPage() {
  const { data, isLoading, refetch } = useGetGalleryQuery({})
  const [deleteItem, { isLoading: deleting }] = useDeleteGalleryItemMutation()

  const items = data?.data ?? []

  const remove = async (id: string) => {
    try {
      await deleteItem(id).unwrap()
      toast.success('Gallery item deleted')
      refetch()
    } catch {
      toast.error('Could not delete item')
    }
  }

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <div className="mb-6">
        <Title level={3}>Gallery</Title>
        <Text type="secondary">Before / after transformations shown to customers.</Text>
      </div>

      {items.length === 0 ? (
        <Empty description="No gallery items yet." />
      ) : (
        <Row gutter={[16, 16]}>
          {items.map((item) => (
            <Col xs={24} sm={12} lg={8} key={item.id}>
              <Card
                title={item.title || item.categoryName}
                extra={
                  <Popconfirm title="Delete this item?" onConfirm={() => remove(item.id)} okText="Delete">
                    <Button danger size="small" icon={<DeleteOutlined />} loading={deleting} />
                  </Popconfirm>
                }
              >
                <Row gutter={8}>
                  <Col span={12}>
                    <Text type="secondary" className="text-xs">Before</Text>
                    <Image src={item.beforeUrl} alt="before" style={{ width: '100%' }} />
                  </Col>
                  <Col span={12}>
                    <Text type="secondary" className="text-xs">After</Text>
                    <Image src={item.afterUrl} alt="after" style={{ width: '100%' }} />
                  </Col>
                </Row>
                <div className="mt-2">
                  <Text type="secondary" className="text-xs">
                    {item.categoryName} · {formatDate(item.createdAt)}
                  </Text>
                </div>
              </Card>
            </Col>
          ))}
        </Row>
      )}
    </div>
  )
}
