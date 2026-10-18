import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import type { ReactElement } from 'react'
import Layout from './components/Layout'
import Home from './pages/Home'
import Discover from './pages/Discover'
import Publish from './pages/Publish'
import PostDetail from './pages/PostDetail'
import EditPost from './pages/EditPost'
import Profile from './pages/Profile'
import Albums from './pages/Albums'
import History from './pages/History'
import Messages from './pages/Messages'
import Login from './pages/Login'
import Forgot from './pages/Forgot'
import Admin from './pages/Admin'
import Settings from './pages/Settings'
import Privacy from './pages/Privacy'
import Terms from './pages/Terms'
import { useAuth } from './store/auth'

function RequireAuth({ children }: { children: ReactElement }) {
  const token = useAuth((s) => s.token)
  if (!token) {
    return <Navigate to="/login" replace />
  }
  return children
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route path="/forgot" element={<Forgot />} />
        <Route path="/admin" element={<Admin />} />
        <Route path="/privacy" element={<Privacy />} />
        <Route path="/terms" element={<Terms />} />
        <Route element={<Layout />}>
          <Route path="/" element={<Home />} />
          <Route path="/discover" element={<Discover />} />
          <Route
            path="/publish"
            element={
              <RequireAuth>
                <Publish />
              </RequireAuth>
            }
          />
          <Route path="/post/:id" element={<PostDetail />} />
          <Route
            path="/post/:id/edit"
            element={
              <RequireAuth>
                <EditPost />
              </RequireAuth>
            }
          />
          <Route path="/profile/:id" element={<Profile />} />
          <Route
            path="/albums"
            element={
              <RequireAuth>
                <Albums />
              </RequireAuth>
            }
          />
          <Route
            path="/history"
            element={
              <RequireAuth>
                <History />
              </RequireAuth>
            }
          />
          <Route
            path="/messages"
            element={
              <RequireAuth>
                <Messages />
              </RequireAuth>
            }
          />
          <Route
            path="/settings"
            element={
              <RequireAuth>
                <Settings />
              </RequireAuth>
            }
          />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  )
}
