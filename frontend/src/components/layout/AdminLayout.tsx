import { Layout, Menu, Avatar, Dropdown, Badge } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  DashboardOutlined, CalendarOutlined, TeamOutlined, ScissorOutlined,
  ShopOutlined, PictureOutlined, BarChartOutlined, BellOutlined,
  UserOutlined, LogoutOutlined
} from '@ant-design/icons'
import { logout } from '../../features/auth/authSlice'
import type { RootState } from '../../app/store'
import { APP_ROUTES } from '../../constants'

const { Header, Sider, Content } = Layout

const AdminLayout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useDispatch()
  const { fullName } = useSelector((state: RootState) => state.auth)

  const menuItems = [
    { key: APP_ROUTES.ADMIN.DASHBOARD, icon: <DashboardOutlined />, label: 'Dashboard' },
    { key: APP_ROUTES.ADMIN.BOOKINGS, icon: <CalendarOutlined />, label: 'Bookings' },
    { key: APP_ROUTES.ADMIN.STAFF, icon: <TeamOutlined />, label: 'Staff' },
    { key: APP_ROUTES.ADMIN.SERVICES, icon: <ScissorOutlined />, label: 'Services' },
    { key: APP_ROUTES.ADMIN.OUTLETS, icon: <ShopOutlined />, label: 'Outlets' },
    { key: APP_ROUTES.ADMIN.GALLERY, icon: <PictureOutlined />, label: 'Gallery' },
    { key: APP_ROUTES.ADMIN.ANALYTICS, icon: <BarChartOutlined />, label: 'Analytics' },
    { key: APP_ROUTES.ADMIN.NOTIFICATIONS, icon: <BellOutlined />, label: 'Notifications' },
  ]

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={220} breakpoint="lg" collapsedWidth="0" className="salon-sidebar">
        <div className="p-4 text-white text-xl font-bold text-center border-b border-gray-600 mb-2">
          💈 Admin
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
          <h2 className="text-lg font-semibold text-gray-700">Admin Dashboard</h2>
          <Dropdown
            menu={{
              items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Logout',
                onClick: () => { dispatch(logout()); navigate('/login') } }],
            }}
          >
            <div className="flex items-center gap-2 cursor-pointer">
              <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#d4a373' }} />
              <span className="font-medium">{fullName}</span>
            </div>
          </Dropdown>
        </Header>
        <Content className="m-6"><Outlet /></Content>
      </Layout>
    </Layout>
  )
}

export default AdminLayout
