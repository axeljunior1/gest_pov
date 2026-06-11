import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { NotificationProvider } from './context/NotificationContext'
import Layout from './components/Layout'
import ProductsPage from './pages/ProductsPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CategoriesPage from './pages/CategoriesPage'
import SuppliersPage from './pages/SuppliersPage'
import UnitsPage from './pages/UnitsPage'
import AttributesPage from './pages/AttributesPage'

export default function App() {
  return (
    <NotificationProvider>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />}>
            <Route index element={<ProductsPage />} />
            <Route path="products/:id" element={<ProductDetailPage />} />
            <Route path="categories" element={<CategoriesPage />} />
            <Route path="suppliers" element={<SuppliersPage />} />
            <Route path="units" element={<UnitsPage />} />
            <Route path="attributes" element={<AttributesPage />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </NotificationProvider>
  )
}
