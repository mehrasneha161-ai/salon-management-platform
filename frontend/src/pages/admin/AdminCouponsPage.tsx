import { useState } from 'react'
import {
  Button,
  DatePicker,
  Form,
  Input,
  InputNumber,
  Modal,
  Select,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
  Typography,
} from 'antd'
import dayjs, { type Dayjs } from 'dayjs'
import toast from 'react-hot-toast'
import {
  useCreateCouponMutation,
  useGetCouponsQuery,
  useToggleCouponMutation,
  useUpdateCouponMutation,
} from '../../features/coupon/couponApi'
import { useGetAllOutletsQuery } from '../../features/outlet/outletApi'
import { useGetPackagesQuery, useGetServicesQuery } from '../../features/service/serviceApi'
import type { Coupon, CouponDiscountType, CouponRequest } from '../../types'
import { formatCurrency, formatDateTime } from '../../utils/formatters'

const { Title, Text } = Typography
const { RangePicker } = DatePicker

type CouponFormValues = Omit<CouponRequest, 'validFrom' | 'validUntil'> & {
  validity: [Dayjs, Dayjs]
}

const getApiErrorMessage = (error: unknown, fallback: string) => {
  const apiError = error as { data?: { message?: string } }
  return apiError.data?.message ?? fallback
}

const optionalNumber = (value: number | null | undefined) => value ?? undefined

const getCouponStatus = (coupon: Coupon) => {
  if (!coupon.isActive) return { label: 'Inactive', color: 'default' }
  const now = dayjs()
  if (now.isBefore(dayjs(coupon.validFrom))) return { label: 'Scheduled', color: 'blue' }
  if (!now.isBefore(dayjs(coupon.validUntil))) return { label: 'Expired', color: 'orange' }
  return { label: 'Active', color: 'green' }
}

export default function AdminCouponsPage() {
  const { data, isLoading } = useGetCouponsQuery()
  const { data: outletsData } = useGetAllOutletsQuery()
  const { data: servicesData } = useGetServicesQuery({})
  const { data: packagesData } = useGetPackagesQuery({})
  const [createCoupon, { isLoading: isCreating }] = useCreateCouponMutation()
  const [updateCoupon, { isLoading: isUpdating }] = useUpdateCouponMutation()
  const [toggleCoupon, { isLoading: isToggling }] = useToggleCouponMutation()
  const [open, setOpen] = useState(false)
  const [editingCoupon, setEditingCoupon] = useState<Coupon>()
  const [form] = Form.useForm<CouponFormValues>()

  const selectedOutletId = Form.useWatch('outletId', form)
  const selectedServiceId = Form.useWatch('serviceId', form)
  const selectedPackageId = Form.useWatch('packageId', form)
  const coupons = data?.data ?? []
  const outlets = outletsData?.data ?? []
  const services = servicesData?.data ?? []
  const packages = packagesData?.data ?? []
  const availableServices = services.filter(
    (service) => !selectedOutletId || !service.outletId || service.outletId === selectedOutletId
  )
  const availablePackages = packages.filter(
    (servicePackage) => !selectedOutletId || !servicePackage.outletId || servicePackage.outletId === selectedOutletId
  )

  const closeModal = () => {
    setOpen(false)
    setEditingCoupon(undefined)
    form.resetFields()
  }

  const openCreate = () => {
    setEditingCoupon(undefined)
    form.resetFields()
    form.setFieldsValue({
      discountType: 'PERCENTAGE',
      minimumSpend: 0,
      isActive: true,
      validity: [dayjs(), dayjs().add(30, 'day')],
    })
    setOpen(true)
  }

  const openEdit = (coupon: Coupon) => {
    setEditingCoupon(coupon)
    form.resetFields()
    form.setFieldsValue({
      code: coupon.code,
      name: coupon.name,
      description: coupon.description ?? undefined,
      discountType: coupon.discountType,
      discountValue: coupon.discountValue,
      minimumSpend: coupon.minimumSpend,
      maximumDiscount: coupon.maximumDiscount ?? undefined,
      validity: [dayjs(coupon.validFrom), dayjs(coupon.validUntil)],
      usageLimit: coupon.usageLimit ?? undefined,
      perCustomerLimit: coupon.perCustomerLimit ?? undefined,
      outletId: coupon.outletId ?? undefined,
      serviceId: coupon.serviceId ?? undefined,
      packageId: coupon.packageId ?? undefined,
      isActive: coupon.isActive,
    })
    setOpen(true)
  }

  const submit = async () => {
    try {
      const values = await form.validateFields()
      const payload: CouponRequest = {
        code: values.code.trim().toUpperCase(),
        name: values.name.trim(),
        description: values.description?.trim() || undefined,
        discountType: values.discountType,
        discountValue: values.discountValue,
        minimumSpend: optionalNumber(values.minimumSpend),
        maximumDiscount: optionalNumber(values.maximumDiscount),
        validFrom: values.validity[0].toISOString(),
        validUntil: values.validity[1].toISOString(),
        usageLimit: optionalNumber(values.usageLimit),
        perCustomerLimit: optionalNumber(values.perCustomerLimit),
        outletId: values.outletId || undefined,
        serviceId: values.serviceId || undefined,
        packageId: values.packageId || undefined,
        isActive: values.isActive,
      }

      if (editingCoupon) {
        await updateCoupon({ id: editingCoupon.id, data: payload }).unwrap()
        toast.success('Coupon updated')
      } else {
        await createCoupon(payload).unwrap()
        toast.success('Coupon created')
      }
      closeModal()
    } catch (error: unknown) {
      if ((error as { errorFields?: unknown }).errorFields) return
      toast.error(getApiErrorMessage(error, 'Could not save coupon'))
    }
  }

  const handleToggle = async (coupon: Coupon) => {
    try {
      await toggleCoupon(coupon.id).unwrap()
      toast.success(`Coupon ${coupon.isActive ? 'deactivated' : 'activated'}`)
    } catch (error: unknown) {
      toast.error(getApiErrorMessage(error, 'Could not update coupon status'))
    }
  }

  const columns = [
    {
      title: 'Coupon',
      key: 'coupon',
      fixed: 'left' as const,
      render: (_: unknown, coupon: Coupon) => (
        <div>
          <Text strong copyable>{coupon.code}</Text>
          <div><Text type="secondary">{coupon.name}</Text></div>
        </div>
      ),
    },
    {
      title: 'Discount',
      key: 'discount',
      render: (_: unknown, coupon: Coupon) => (
        <div>
          <Tag color={coupon.discountType === 'PERCENTAGE' ? 'purple' : 'cyan'}>
            {coupon.discountType === 'PERCENTAGE' ? 'Percentage' : 'Fixed'}
          </Tag>
          <div>
            {coupon.discountType === 'PERCENTAGE'
              ? `${coupon.discountValue}%`
              : formatCurrency(coupon.discountValue)}
          </div>
        </div>
      ),
    },
    {
      title: 'Min spend',
      dataIndex: 'minimumSpend',
      key: 'minimumSpend',
      render: (value: number) => formatCurrency(value ?? 0),
    },
    {
      title: 'Validity',
      key: 'validity',
      render: (_: unknown, coupon: Coupon) => (
        <div className="whitespace-nowrap">
          <div>{formatDateTime(coupon.validFrom)}</div>
          <Text type="secondary">to {formatDateTime(coupon.validUntil)}</Text>
        </div>
      ),
    },
    {
      title: 'Usage',
      key: 'usage',
      render: (_: unknown, coupon: Coupon) => {
        const used = coupon.reservedCount + coupon.redeemedCount
        return (
          <div className="whitespace-nowrap">
            <div>{used} / {coupon.usageLimit ?? 'Unlimited'}</div>
            <Text type="secondary">
              {coupon.reservedCount} reserved, {coupon.redeemedCount} redeemed
            </Text>
          </div>
        )
      },
    },
    {
      title: 'Status',
      key: 'status',
      render: (_: unknown, coupon: Coupon) => {
        const status = getCouponStatus(coupon)
        return <Tag color={status.color}>{status.label}</Tag>
      },
    },
    {
      title: 'Active',
      key: 'active',
      render: (_: unknown, coupon: Coupon) => (
        <Switch
          checked={coupon.isActive}
          loading={isToggling}
          onChange={() => handleToggle(coupon)}
          aria-label={`Toggle ${coupon.code}`}
        />
      ),
    },
    {
      title: 'Actions',
      key: 'actions',
      fixed: 'right' as const,
      render: (_: unknown, coupon: Coupon) => (
        <Button size="small" onClick={() => openEdit(coupon)}>Edit</Button>
      ),
    },
  ]

  if (isLoading) {
    return <div className="flex justify-center p-20"><Spin size="large" /></div>
  }

  return (
    <div>
      <Space style={{ width: '100%', justifyContent: 'space-between', marginBottom: 8 }}>
        <Title level={3} style={{ margin: 0 }}>Coupons</Title>
        <Button type="primary" onClick={openCreate}>+ Add coupon</Button>
      </Space>
      <Text type="secondary" className="block mb-4">
        Create discounts, control redemption limits, and optionally scope offers to an outlet or item.
      </Text>

      <Table
        dataSource={coupons}
        columns={columns}
        rowKey="id"
        scroll={{ x: 1250 }}
      />

      <Modal
        title={editingCoupon ? 'Edit coupon' : 'Add coupon'}
        open={open}
        width={760}
        onOk={submit}
        confirmLoading={isCreating || isUpdating}
        onCancel={closeModal}
        okText={editingCoupon ? 'Save' : 'Create'}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-4">
            <Form.Item
              name="code"
              label="Coupon code"
              rules={[
                { required: true, whitespace: true, message: 'Coupon code is required' },
                { max: 50 },
              ]}
            >
              <Input placeholder="WELCOME20" maxLength={50} />
            </Form.Item>
            <Form.Item
              name="name"
              label="Name"
              rules={[
                { required: true, whitespace: true, message: 'Coupon name is required' },
                { max: 150 },
              ]}
            >
              <Input maxLength={150} />
            </Form.Item>
          </div>

          <Form.Item name="description" label="Description">
            <Input.TextArea rows={2} />
          </Form.Item>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-4">
            <Form.Item name="discountType" label="Discount type" rules={[{ required: true }]}>
              <Select
                options={[
                  { value: 'PERCENTAGE' satisfies CouponDiscountType, label: 'Percentage' },
                  { value: 'FIXED' satisfies CouponDiscountType, label: 'Fixed amount' },
                ]}
              />
            </Form.Item>
            <Form.Item
              noStyle
              shouldUpdate={(previous, current) => previous.discountType !== current.discountType}
            >
              {({ getFieldValue }) => (
                <Form.Item
                  name="discountValue"
                  label={getFieldValue('discountType') === 'PERCENTAGE' ? 'Discount (%)' : 'Discount (₹)'}
                  rules={[
                    { required: true, message: 'Discount value is required' },
                    {
                      validator: (_, value?: number) => {
                        if (value === undefined || value === null) return Promise.resolve()
                        if (value <= 0) return Promise.reject(new Error('Discount value must be positive'))
                        if (getFieldValue('discountType') === 'PERCENTAGE' && value > 100) {
                          return Promise.reject(new Error('Percentage discount cannot exceed 100'))
                        }
                        return Promise.resolve()
                      },
                    },
                  ]}
                >
                  <InputNumber
                    min={0.01}
                    max={getFieldValue('discountType') === 'PERCENTAGE' ? 100 : undefined}
                    precision={2}
                    style={{ width: '100%' }}
                  />
                </Form.Item>
              )}
            </Form.Item>
            <Form.Item name="minimumSpend" label="Minimum spend (₹)">
              <InputNumber min={0} precision={2} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="maximumDiscount" label="Maximum discount (₹, optional)">
              <InputNumber min={0.01} precision={2} style={{ width: '100%' }} />
            </Form.Item>
          </div>

          <Form.Item
            name="validity"
            label="Validity"
            rules={[
              { required: true, message: 'Select a validity range' },
              {
                validator: (_, value?: [Dayjs, Dayjs]) =>
                  !value || value[1].isAfter(value[0])
                    ? Promise.resolve()
                    : Promise.reject(new Error('Valid until must be after valid from')),
              },
            ]}
          >
            <RangePicker showTime style={{ width: '100%' }} />
          </Form.Item>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-4">
            <Form.Item name="usageLimit" label="Global usage limit (optional)">
              <InputNumber min={1} precision={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="perCustomerLimit" label="Per-customer limit (optional)">
              <InputNumber min={1} precision={0} style={{ width: '100%' }} />
            </Form.Item>
          </div>

          <Form.Item name="outletId" label="Outlet scope (optional)">
            <Select
              allowClear
              placeholder="All outlets"
              options={outlets.map((outlet) => ({ value: outlet.id, label: outlet.name }))}
              onChange={() => {
                form.setFieldValue('serviceId', undefined)
                form.setFieldValue('packageId', undefined)
              }}
            />
          </Form.Item>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-x-4">
            <Form.Item name="serviceId" label="Service scope (optional)">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                disabled={Boolean(selectedPackageId)}
                placeholder="All services"
                options={availableServices.map((service) => ({ value: service.id, label: service.name }))}
                onChange={(value) => {
                  if (value) form.setFieldValue('packageId', undefined)
                }}
              />
            </Form.Item>
            <Form.Item name="packageId" label="Package scope (optional)">
              <Select
                allowClear
                showSearch
                optionFilterProp="label"
                disabled={Boolean(selectedServiceId)}
                placeholder="All packages"
                options={availablePackages.map((servicePackage) => ({
                  value: servicePackage.id,
                  label: servicePackage.name,
                }))}
                onChange={(value) => {
                  if (value) form.setFieldValue('serviceId', undefined)
                }}
              />
            </Form.Item>
          </div>

          <Form.Item name="isActive" label="Active" valuePropName="checked">
            <Switch />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
