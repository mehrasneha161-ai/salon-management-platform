import { useState } from 'react'
import {
  Typography, Table, Button, Space, Tag, Spin, Modal, Form, Input,
  Select, InputNumber, Popconfirm, Tabs,
} from 'antd'
import {
  useGetServicesQuery,
  useGetPackagesQuery,
  useGetCategoriesQuery,
  useCreateServiceMutation,
  useDeleteServiceMutation,
} from '../../features/service/serviceApi'
import { useGetOutletsQuery } from '../../features/outlet/outletApi'
import { SalonService, ServicePackage } from '../../types'
import { formatCurrency } from '../../utils/formatters'
import toast from 'react-hot-toast'

const { Title } = Typography

export default function AdminServicesPage() {
  const { data: servicesData, isLoading, refetch } = useGetServicesQuery({})
  const { data: packagesData } = useGetPackagesQuery({})
  const { data: categoriesData } = useGetCategoriesQuery()
  const { data: outletsData } = useGetOutletsQuery()
  const [createService, { isLoading: creating }] = useCreateServiceMutation()
  const [deleteService] = useDeleteServiceMutation()

  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const services = servicesData?.data ?? []
  const packages = packagesData?.data ?? []
  const categories = categoriesData?.data ?? []
  const outlets = outletsData?.data ?? []

  const submit = async () => {
    try {
      const values = await form.validateFields()
      await createService(values).unwrap()
      toast.success('Service created')
      setOpen(false)
      form.resetFields()
      refetch()
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return
      toast.error('Could not create service')
    }
  }

  const remove = async (id: string) => {
    try { await deleteService(id).unwrap(); toast.success('Service deleted'); refetch() }
    catch { toast.error('Could not delete service') }
  }

  const serviceColumns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Category', dataIndex: 'categoryName', key: 'categoryName' },
    { title: 'Duration', dataIndex: 'durationMinutes', key: 'durationMinutes', render: (v: number) => `${v} min` },
    { title: 'Price', dataIndex: 'price', key: 'price', render: (v: number) => formatCurrency(v) },
    { title: 'Active', dataIndex: 'isActive', key: 'isActive', render: (v: boolean) => <Tag color={v ? 'green' : 'red'}>{v ? 'Yes' : 'No'}</Tag> },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, r: SalonService) => (
        <Popconfirm title="Delete this service?" onConfirm={() => remove(r.id)} okText="Delete">
          <Button danger size="small">Delete</Button>
        </Popconfirm>
      ),
    },
  ]

  const packageColumns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'Price', dataIndex: 'price', key: 'price', render: (v: number) => formatCurrency(v) },
    { title: 'Discount %', dataIndex: 'discountPct', key: 'discountPct' },
    {
      title: 'Services', key: 'services',
      render: (_: unknown, r: ServicePackage) => (r.services ?? []).map((s) => s.name).join(', ') || '-',
    },
  ]

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>Services &amp; Packages</Title>
        <Button type="primary" onClick={() => setOpen(true)}>+ Add service</Button>
      </Space>

      <Tabs
        items={[
          {
            key: 'services',
            label: `Services (${services.length})`,
            children: <Table dataSource={services} columns={serviceColumns} rowKey="id" scroll={{ x: 800 }} />,
          },
          {
            key: 'packages',
            label: `Packages (${packages.length})`,
            children: <Table dataSource={packages} columns={packageColumns} rowKey="id" scroll={{ x: 700 }} />,
          },
        ]}
      />

      <Modal
        title="Add service"
        open={open}
        onOk={submit}
        confirmLoading={creating}
        onCancel={() => setOpen(false)}
        okText="Create"
      >
        <Form form={form} layout="vertical" initialValues={{ durationMinutes: 30, isActive: true }}>
          <Form.Item name="name" label="Service name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="categoryId" label="Category" rules={[{ required: true }]}>
            <Select options={categories.map((c) => ({ value: c.id, label: c.name }))} />
          </Form.Item>
          <Form.Item name="outletId" label="Outlet (optional — all outlets if empty)">
            <Select allowClear options={outlets.map((o) => ({ value: o.id, label: o.name }))} />
          </Form.Item>
          <Form.Item name="durationMinutes" label="Duration (minutes)" rules={[{ required: true }]}>
            <InputNumber min={1} step={15} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="price" label="Price (₹)" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
