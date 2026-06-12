import { Form, Input, Button, Card, Typography, Divider } from 'antd'
import { useNavigate, Link } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import toast from 'react-hot-toast'
import { useRegisterMutation } from '../features/auth/authApi'
import { setCredentials } from '../features/auth/authSlice'
import { APP_ROUTES } from '../constants'

const { Title, Text } = Typography

const RegisterPage: React.FC = () => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const [register, { isLoading }] = useRegisterMutation()

  const onFinish = async (values: { fullName: string; phoneNumber: string; password: string }) => {
    try {
      const result = await register(values).unwrap()
      if (result.success) {
        dispatch(setCredentials(result.data))
        toast.success('Account created successfully!')
        navigate(APP_ROUTES.CUSTOMER.DASHBOARD)
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } }
      toast.error(error?.data?.message ?? 'Registration failed.')
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-amber-50 to-orange-100 flex items-center justify-center p-4">
      <Card className="w-full max-w-md shadow-xl rounded-2xl">
        <div className="text-center mb-6">
          <div className="text-5xl mb-3">💈</div>
          <Title level={2} className="!mb-1">Create Account</Title>
          <Text type="secondary">Join us today</Text>
        </div>
        <Form layout="vertical" onFinish={onFinish} size="large">
          <Form.Item name="fullName" label="Full Name"
            rules={[{ required: true }, { min: 2, message: 'At least 2 characters' }]}>
            <Input placeholder="Your full name" />
          </Form.Item>
          <Form.Item name="phoneNumber" label="Phone Number"
            rules={[{ required: true }, { pattern: /^[6-9]\d{9}$/, message: 'Valid Indian number required' }]}>
            <Input placeholder="9876543210" />
          </Form.Item>
          <Form.Item name="email" label="Email (optional)">
            <Input type="email" placeholder="you@example.com" />
          </Form.Item>
          <Form.Item name="password" label="Password"
            rules={[{ required: true }, { min: 8, message: 'Minimum 8 characters' }]}>
            <Input.Password placeholder="Create a strong password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={isLoading} className="h-12 text-base">
            Create Account
          </Button>
        </Form>
        <Divider />
        <div className="text-center">
          <Text type="secondary">Already have an account? </Text>
          <Link to="/login" className="text-amber-600 font-medium">Sign in</Link>
        </div>
      </Card>
    </div>
  )
}

export default RegisterPage
