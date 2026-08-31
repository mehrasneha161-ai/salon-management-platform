import { Layout, Menu, Avatar, Dropdown, Button } from 'antd'
import { Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useDispatch, useSelector } from 'react-redux'
import {
  HomeOutlined, CalendarOutlined, HistoryOutlined,
  PictureOutlined, ShopOutlined, UserOutlined, LogoutOutlined
} from '@ant-design/icons'
import { logout } from '../../features/auth/authSlice'
import type { RootState } from '../../app/store'
import { APP_ROUTES } from '../../constants'

const { Header, Sider, Content } = Layout

const CustomerLayout: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const dispatch = useDispatch()
  const { fullName } = useSelector((state: RootState) => state.auth)

  const menuItems = [
    { key: APP_ROUTES.CUSTOMER.DASHBOARD, icon: <HomeOutlined />, label: 'Dashboard' },
    { key: APP_ROUTES.CUSTOMER.OUTLETS, icon: <ShopOutlined />, label: 'Outlets' },
    { key: APP_ROUTES.CUSTOMER.BOOK, icon: <CalendarOutlined />, label: 'Book Appointment' },
    { key: APP_ROUTES.CUSTOMER.HISTORY, icon: <HistoryOutlined />, label: 'My Bookings' },
    { key: APP_ROUTES.CUSTOMER.GALLERY, icon: <PictureOutlined />, label: 'Gallery' },
  ]

  const handleLogout = () => {
    dispatch(logout())
    navigate('/login')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider breakpoint="lg" collapsedWidth="0" className="salon-sidebar">
        <div className="p-4 text-white text-xl font-bold text-center border-b border-gray-600 mb-2">
          💈 Salon
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
          <h2 className="text-lg font-semibold text-gray-700">Customer Portal</h2>
          <Dropdown
            menu={{
              items: [{ key: 'logout', icon: <LogoutOutlined />, label: 'Logout', onClick: handleLogout }],
            }}
          >
            <div className="flex items-center gap-2 cursor-pointer">
              <Avatar icon={<UserOutlined />} />
              <span className="text-gray-700">{fullName}</span>
            </div>
          </Dropdown>
        </Header>
        <Content className="m-6">
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}

export default CustomerLayout
