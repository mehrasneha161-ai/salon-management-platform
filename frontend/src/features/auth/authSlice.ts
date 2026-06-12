import { createSlice, PayloadAction } from '@reduxjs/toolkit'

interface AuthState {
  accessToken: string | null
  refreshToken: string | null
  role: string | null
  fullName: string | null
  phoneNumber: string | null
  isAuthenticated: boolean
}

const initialState: AuthState = {
  accessToken: localStorage.getItem('accessToken'),
  refreshToken: localStorage.getItem('refreshToken'),
  role: localStorage.getItem('role'),
  fullName: localStorage.getItem('fullName'),
  phoneNumber: localStorage.getItem('phoneNumber'),
  isAuthenticated: !!localStorage.getItem('accessToken'),
}

const authSlice = createSlice({
  name: 'auth',
  initialState,
  reducers: {
    setCredentials: (state, action: PayloadAction<{
      accessToken: string
      refreshToken: string
      role: string
      fullName: string
      phoneNumber: string
    }>) => {
      const { accessToken, refreshToken, role, fullName, phoneNumber } = action.payload
      state.accessToken = accessToken
      state.refreshToken = refreshToken
      state.role = role
      state.fullName = fullName
      state.phoneNumber = phoneNumber
      state.isAuthenticated = true
      localStorage.setItem('accessToken', accessToken)
      localStorage.setItem('refreshToken', refreshToken)
      localStorage.setItem('role', role)
      localStorage.setItem('fullName', fullName)
      localStorage.setItem('phoneNumber', phoneNumber)
    },
    setTokens: (state, action: PayloadAction<{ accessToken: string; refreshToken: string }>) => {
      state.accessToken = action.payload.accessToken
      state.refreshToken = action.payload.refreshToken
      localStorage.setItem('accessToken', action.payload.accessToken)
      localStorage.setItem('refreshToken', action.payload.refreshToken)
    },
    logout: (state) => {
      state.accessToken = null
      state.refreshToken = null
      state.role = null
      state.fullName = null
      state.phoneNumber = null
      state.isAuthenticated = false
      localStorage.clear()
    },
  },
})

export const { setCredentials, setTokens, logout } = authSlice.actions
export default authSlice.reducer
