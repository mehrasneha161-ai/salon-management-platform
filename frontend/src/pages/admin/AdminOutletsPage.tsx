import { useState } from 'react'
import {
  Typography, Table, Button, Space, Tag, Spin, Modal, Form, Input, TimePicker, Switch, Popconfirm,
} from 'antd'
import dayjs from 'dayjs'
import {
  useGetAllOutletsQuery,
  useCreateOutletMutation,
  useUpdateOutletMutation,
  useDeleteOutletMutation,
} from '../../features/outlet/outletApi'
import { Outlet } from '../../types'
import toast from 'react-hot-toast'

const { Title, Text } = Typography
const TIME_FMT = 'HH:mm'

export default function AdminOutletsPage() {
  const { data, isLoading, refetch } = useGetAllOutletsQuery()
  const [createOutlet, { isLoading: creating }] = useCreateOutletMutation()
  const [updateOutlet, { isLoading: updating }] = useUpdateOutletMutation()
  const [deleteOutlet] = useDeleteOutletMutation()

  const [open, setOpen] = useState(false)
  const [editing, setEditing] = useState<Outlet | null>(null)
  const [form] = Form.useForm()

  const outlets = data?.data ?? []

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({
      isActive: true,
      openingTime: dayjs('09:00', TIME_FMT),
      closingTime: dayjs('20:00', TIME_FMT),
    })
    setOpen(true)
  }

  const openEdit = (o: Outlet) => {
    setEditing(o)
    form.setFieldsValue({
      name: o.name,
      address: o.address,
      city: o.city,
      phone: o.phone,
      isActive: o.isActive,
      openingTime: o.openingTime ? dayjs(o.openingTime, TIME_FMT) : dayjs('09:00', TIME_FMT),
      closingTime: o.closingTime ? dayjs(o.closingTime, TIME_FMT) : dayjs('20:00', TIME_FMT),
    })
    setOpen(true)
  }

  const submit = async () => {
    try {
      const v = await form.validateFields()
      const payload = {
        name: v.name,
        address: v.address,
        city: v.city,
        phone: v.phone,
        isActive: v.isActive,
        openingTime: v.openingTime ? v.openingTime.format(TIME_FMT) : undefined,
        closingTime: v.closingTime ? v.closingTime.format(TIME_FMT) : undefined,
      }
      if (editing) {
        await updateOutlet({ id: editing.id, data: payload }).unwrap()
        toast.success('Outlet updated')
      } else {
        await createOutlet(payload).unwrap()
        toast.success('Outlet created')
      }
      setOpen(false)
      refetch()
    } catch (e) {
      if ((e as { errorFields?: unknown }).errorFields) return
      toast.error('Could not save outlet')
    }
  }

  const remove = async (id: string) => {
    try { await deleteOutlet(id).unwrap(); toast.success('Outlet deleted'); refetch() }
    catch { toast.error('Could not delete outlet') }
  }

  const columns = [
    { title: 'Name', dataIndex: 'name', key: 'name' },
    { title: 'City', dataIndex: 'city', key: 'city', render: (v: string) => v || '-' },
    { title: 'Phone', dataIndex: 'phone', key: 'phone', render: (v: string) => v || '-' },
    {
      title: 'Business hours', key: 'hours',
      render: (_: unknown, r: Outlet) => `${r.openingTime ?? '09:00'} – ${r.closingTime ?? '20:00'}`,
    },
    {
      title: 'Active', dataIndex: 'isActive', key: 'isActive',
      render: (v: boolean) => <Tag color={v ? 'green' : 'red'}>{v ? 'Yes' : 'No'}</Tag>,
    },
    {
      title: 'Actions', key: 'actions',
      render: (_: unknown, r: Outlet) => (
        <Space>
          <Button size="small" onClick={() => openEdit(r)}>Edit</Button>
          <Popconfirm title="Delete this outlet?" onConfirm={() => remove(r.id)} okText="Delete">
            <Button danger size="small">Delete</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 8 }}>
        <Title level={3} style={{ margin: 0 }}>Outlets</Title>
        <Button type="primary" onClick={openCreate}>+ Add outlet</Button>
      </Space>
      <Text type="secondary" className="block mb-4">
        Business hours control which booking slots customers can pick.
      </Text>

      <Table dataSource={outlets} columns={columns} rowKey="id" scroll={{ x: 800 }} />

      <Modal
        title={editing ? 'Edit outlet' : 'Add outlet'}
        open={open}
        onOk={submit}
        confirmLoading={creating || updating}
        onCancel={() => setOpen(false)}
        okText="Save"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="name" label="Outlet name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="address" label="Address" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="city" label="City">
            <Input />
          </Form.Item>
          <Form.Item name="phone" label="Phone">
            <Input />
          </Form.Item>
          <Space>
            <Form.Item name="openingTime" label="Opening time">
              <TimePicker format={TIME_FMT} minuteStep={30} />
            </Form.Item>
            <Form.Item name="closingTime" label="Closing time">
              <TimePicker format={TIME_FMT} minuteStep={30} />
            </Form.Item>
          </Space>
          <Form.Item name="isActive" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
