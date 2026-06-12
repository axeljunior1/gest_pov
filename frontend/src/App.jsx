import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import ProtectedRoute from './components/ProtectedRoute'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import ProductsPage from './pages/ProductsPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CategoriesPage from './pages/CategoriesPage'
import SuppliersPage from './pages/SuppliersPage'
import UnitsPage from './pages/UnitsPage'
import AttributesPage from './pages/AttributesPage'
import StockPage from './pages/StockPage'
import StockEntriesPage from './pages/StockEntriesPage'
import AlertsPage from './pages/AlertsPage'
import DocumentationPage from './pages/DocumentationPage'

export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<Layout />}>
                <Route index element={<ProductsPage />} />
                <Route path="products/:id" element={<ProductDetailPage />} />
                <Route path="categories" element={<CategoriesPage />} />
                <Route path="suppliers" element={<SuppliersPage />} />
                <Route path="units" element={<UnitsPage />} />
                <Route path="attributes" element={<AttributesPage />} />
                <Route path="stock" element={<StockPage />} />
                <Route path="stock/entries" element={<StockEntriesPage />} />
                <Route path="alerts" element={<AlertsPage />} />
                <Route path="documentation" element={<DocumentationPage />} />
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  )
}
