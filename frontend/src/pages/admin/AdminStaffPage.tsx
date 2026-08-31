import { useState } from 'react'
import { Table, Button, Typography, Space, Spin, Modal, Form, Input, Select } from 'antd'
import {
  useGetStaffQuery,
  useRegisterStaffMutation,
  useUpdateStaffStatusMutation,
} from '../../features/staff/staffApi'
import { useGetOutletsQuery } from '../../features/outlet/outletApi'
import { StaffMember } from '../../types'
import toast from 'react-hot-toast'

const { Title } = Typography

const STATUS_OPTIONS = ['AVAILABLE', 'BUSY', 'OFF_DUTY']

export default function AdminStaffPage() {
  const { data, isLoading, refetch } = useGetStaffQuery({})
  const { data: outletsData } = useGetOutletsQuery()
  const [registerStaff, { isLoading: registering }] = useRegisterStaffMutation()
  const [updateStatus] = useUpdateStaffStatusMutation()
  const [open, setOpen] = useState(false)
  const [form] = Form.useForm()

  const staff = data?.data ?? []
  const outlets = outletsData?.data ?? []

  const submit = async () => {
    try {
      const values = await form.validateFields()
      await registerStaff(values).unwrap()
      toast.success('Staff registered')
      setOpen(false)
      form.resetFields()
      refetch()
    } catch (e) {
      // validation errors are shown inline; API errors below
      if ((e as { errorFields?: unknown }).errorFields) return
      toast.error('Could not register staff')
    }
  }

  const changeStatus = async (id: string, status: string) => {
    try { await updateStatus({ id, status }).unwrap(); toast.success('Status updated'); refetch() }
    catch { toast.error('Could not update status') }
  }

  const columns = [
    { title: 'Name', dataIndex: 'fullName', key: 'fullName' },
    { title: 'Phone', dataIndex: 'phoneNumber', key: 'phoneNumber' },
    { title: 'Specialization', dataIndex: 'specialization', key: 'specialization', render: (v: string) => v || '-' },
    { title: 'Outlet', dataIndex: 'outletName', key: 'outletName' },
    { title: 'Present days', dataIndex: 'totalPresentDays', key: 'totalPresentDays' },
    {
      title: 'Status', key: 'status',
      render: (_: unknown, r: StaffMember) => (
        <Select
          size="small"
          value={r.status}
          style={{ width: 130 }}
          options={STATUS_OPTIONS.map((s) => ({ value: s, label: s }))}
          onChange={(v) => changeStatus(r.id, v)}
        />
      ),
    },
  ]

  if (isLoading) return <div className="flex justify-center p-20"><Spin size="large" /></div>

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 16 }}>
        <Title level={3} style={{ margin: 0 }}>Staff</Title>
        <Button type="primary" onClick={() => setOpen(true)}>+ Register staff</Button>
      </Space>

      <Table dataSource={staff} columns={columns} rowKey="id" scroll={{ x: 800 }} />

      <Modal
        title="Register staff"
        open={open}
        onOk={submit}
        confirmLoading={registering}
        onCancel={() => setOpen(false)}
        okText="Register"
      >
        <Form form={form} layout="vertical">
          <Form.Item name="fullName" label="Full name" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="phoneNumber" label="Phone" rules={[{ required: true, pattern: /^[6-9]\d{9}$/, message: 'Valid 10-digit mobile' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label="Password" rules={[{ required: true, min: 8 }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="outletId" label="Outlet" rules={[{ required: true }]}>
            <Select options={outlets.map((o) => ({ value: o.id, label: o.name }))} />
          </Form.Item>
          <Form.Item name="specialization" label="Specialization">
            <Input />
          </Form.Item>
          <Form.Item name="bio" label="Bio">
            <Input.TextArea rows={2} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
