import { Form, Input, Button, Card, Typography, Divider } from 'antd'
import { UserOutlined, LockOutlined, PhoneOutlined } from '@ant-design/icons'
import { useNavigate, Link } from 'react-router-dom'
import { useDispatch } from 'react-redux'
import toast from 'react-hot-toast'
import { useLoginMutation } from '../features/auth/authApi'
import { setCredentials } from '../features/auth/authSlice'
import { APP_ROUTES } from '../constants'

const { Title, Text } = Typography

const LoginPage: React.FC = () => {
  const navigate = useNavigate()
  const dispatch = useDispatch()
  const [login, { isLoading }] = useLoginMutation()

  const onFinish = async (values: { phoneNumber: string; password: string }) => {
    try {
      const result = await login(values).unwrap()
      if (result.success) {
        dispatch(setCredentials(result.data))
        toast.success(`Welcome back, ${result.data.fullName}!`)
        const role = result.data.role
        if (role === 'ADMIN') navigate(APP_ROUTES.ADMIN.DASHBOARD)
        else if (role === 'STAFF') navigate(APP_ROUTES.STAFF.DASHBOARD)
        else navigate(APP_ROUTES.CUSTOMER.DASHBOARD)
      }
    } catch (err: unknown) {
      const error = err as { data?: { message?: string } }
      toast.error(error?.data?.message ?? 'Login failed. Please try again.')
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-amber-50 to-orange-100 flex items-center justify-center p-4">
      <Card className="w-full max-w-md shadow-xl rounded-2xl">
        <div className="text-center mb-6">
          <div className="text-5xl mb-3">💈</div>
          <Title level={2} className="!mb-1">Salon Platform</Title>
          <Text type="secondary">Sign in to your account</Text>
        </div>
        <Form layout="vertical" onFinish={onFinish} size="large">
          <Form.Item
            name="phoneNumber"
            label="Phone Number"
            rules={[{ required: true, message: 'Phone number is required' },
                    { pattern: /^[6-9]\d{9}$/, message: 'Enter valid Indian phone number' }]}
          >
            <Input prefix={<PhoneOutlined />} placeholder="9876543210" />
          </Form.Item>
          <Form.Item
            name="password"
            label="Password"
            rules={[{ required: true, message: 'Password is required' }]}
          >
            <Input.Password prefix={<LockOutlined />} placeholder="Your password" />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={isLoading} className="h-12 text-base">
            Sign In
          </Button>
        </Form>
        <Divider />
        <div className="text-center">
          <Text type="secondary">Don't have an account? </Text>
          <Link to="/register" className="text-amber-600 font-medium">Register here</Link>
        </div>
      </Card>
    </div>
  )
}

export default LoginPage
