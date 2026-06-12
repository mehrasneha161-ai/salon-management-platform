import React from 'react'
import ReactDOM from 'react-dom/client'
import { Provider } from 'react-redux'
import { BrowserRouter } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import { Toaster } from 'react-hot-toast'
import App from './App'
import { store } from './app/store'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <Provider store={store}>
      <BrowserRouter>
        <ConfigProvider
          theme={{
            token: {
              colorPrimary: '#d4a373',
              borderRadius: 8,
              fontFamily: "'Inter', sans-serif",
            },
          }}
        >
          <App />
          <Toaster position="top-right" toastOptions={{ duration: 4000 }} />
        </ConfigProvider>
      </BrowserRouter>
    </Provider>
  </React.StrictMode>
)
