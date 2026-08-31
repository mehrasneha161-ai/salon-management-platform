import { Routes, Route, Navigate } from 'react-router-dom'
import { useSelector } from 'react-redux'
import type { RootState } from './app/store'
import { APP_ROUTES } from './constants'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import CustomerLayout from './components/layout/CustomerLayout'
import StaffLayout from './components/layout/StaffLayout'
import AdminLayout from './components/layout/AdminLayout'
import CustomerDashboard from './pages/customer/CustomerDashboard'
import OutletsPage from './pages/customer/OutletsPage'
import BookingPage from './pages/customer/BookingPage'
import BookingHistoryPage from './pages/customer/BookingHistoryPage'
import GalleryPage from './pages/customer/GalleryPage'
import StaffDashboard from './pages/staff/StaffDashboard'
import StaffAttendancePage from './pages/staff/StaffAttendancePage'
import AdminDashboard from './pages/admin/AdminDashboard'
import AdminBookingsPage from './pages/admin/AdminBookingsPage'
import AdminOutletsPage from './pages/admin/AdminOutletsPage'
import AdminStaffPage from './pages/admin/AdminStaffPage'
import AdminServicesPage from './pages/admin/AdminServicesPage'
import AdminAnalyticsPage from './pages/admin/AdminAnalyticsPage'
import AdminNotificationsPage from './pages/admin/AdminNotificationsPage'
import AdminGalleryPage from './pages/admin/AdminGalleryPage'
import ProtectedRoute from './components/layout/ProtectedRoute'

function App() {
  const { isAuthenticated, role } = useSelector((state: RootState) => state.auth)

  return (
    <Routes>
      {/* Public */}
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />

      {/* Root redirect */}
      <Route
        path="/"
        element={
          isAuthenticated
            ? role === 'ADMIN'
              ? <Navigate to={APP_ROUTES.ADMIN.DASHBOARD} replace />
              : role === 'STAFF'
              ? <Navigate to={APP_ROUTES.STAFF.DASHBOARD} replace />
              : <Navigate to={APP_ROUTES.CUSTOMER.DASHBOARD} replace />
            : <Navigate to="/login" replace />
        }
      />

      {/* Customer Portal */}
      <Route element={<ProtectedRoute allowedRoles={['CUSTOMER']} />}>
        <Route element={<CustomerLayout />}>
          <Route path={APP_ROUTES.CUSTOMER.DASHBOARD} element={<CustomerDashboard />} />
          <Route path={APP_ROUTES.CUSTOMER.OUTLETS} element={<OutletsPage />} />
          <Route path={APP_ROUTES.CUSTOMER.BOOK} element={<BookingPage />} />
          <Route path={APP_ROUTES.CUSTOMER.HISTORY} element={<BookingHistoryPage />} />
          <Route path={APP_ROUTES.CUSTOMER.GALLERY} element={<GalleryPage />} />
        </Route>
      </Route>

      {/* Staff Portal */}
      <Route element={<ProtectedRoute allowedRoles={['STAFF']} />}>
        <Route element={<StaffLayout />}>
          <Route path={APP_ROUTES.STAFF.DASHBOARD} element={<StaffDashboard />} />
          <Route path={APP_ROUTES.STAFF.ATTENDANCE} element={<StaffAttendancePage />} />
        </Route>
      </Route>

      {/* Admin Portal */}
      <Route element={<ProtectedRoute allowedRoles={['ADMIN']} />}>
        <Route element={<AdminLayout />}>
          <Route path={APP_ROUTES.ADMIN.DASHBOARD} element={<AdminDashboard />} />
          <Route path={APP_ROUTES.ADMIN.BOOKINGS} element={<AdminBookingsPage />} />
          <Route path={APP_ROUTES.ADMIN.STAFF} element={<AdminStaffPage />} />
          <Route path={APP_ROUTES.ADMIN.SERVICES} element={<AdminServicesPage />} />
          <Route path={APP_ROUTES.ADMIN.OUTLETS} element={<AdminOutletsPage />} />
          <Route path={APP_ROUTES.ADMIN.ANALYTICS} element={<AdminAnalyticsPage />} />
          <Route path={APP_ROUTES.ADMIN.NOTIFICATIONS} element={<AdminNotificationsPage />} />
          <Route path={APP_ROUTES.ADMIN.GALLERY} element={<AdminGalleryPage />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  )
}

export default App
