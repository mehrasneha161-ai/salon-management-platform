import { Layout, Menu, Avatar, Dropdown, Tag } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import { HomeOutlined, ClockCircleOutlined, UserOutlined, LogoutOutlined } from '@ant-design/icons'
import { logout } from '../../features/auth/authSlice'
import { RootState } from '../../app/store'
import { APP_ROUTES } from '../../constants'

const { Header, Sider, Content } = Layout

const StaffLayout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useDispatch()
  const { fullName } = useSelector((state: RootState) => state.auth)

  const menuItems = [
    { key: APP_ROUTES.STAFF.DASHBOARD, icon: <HomeOutlined />, label: 'Dashboard' },
    { key: APP_ROUTES.STAFF.ATTENDANCE, icon: <ClockCircleOutlined />, label: 'Attendance' },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth="0" className="salon-sidebar">
        <div className="p-4 text-white text-xl font-bold text-center border-b border-gray-600 mb-2">
          💈 Staff
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
          style={{ background: 'transparent' }}
        />
      </Sider>
      <Layout>
        <Header className="bg-white flex items-center justify-between px-6 shadow-sm">
          <div className="flex items-center gap-2">
            <h2 className="text-lg font-semibold text-gray-700">Staff Portal</h2>
            <Tag color="blue">STAFF</Tag>
          </div>
          <Dropdown
            menu={{
              items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Logout',
                onClick: () => { dispatch(logout()); navigate('/login') } }],
            }}
          >
            <div className="flex items-center gap-2 cursor-pointer">
              <Avatar icon={<UserOutlined />} />
              <span>{fullName}</span>
            </div>
          </Dropdown>
        </Header>
        <Content className="m-6"><Outlet /></Content>
      </Layout>
    </Layout>
  )
}

export default StaffLayout
