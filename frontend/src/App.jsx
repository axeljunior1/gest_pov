import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import ProtectedRoute from './components/ProtectedRoute'
import PermissionRoute from './components/PermissionRoute'
import Layout from './components/Layout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import ProductsPage from './pages/ProductsPage'
import ProductDetailPage from './pages/ProductDetailPage'
import CategoriesPage from './pages/CategoriesPage'
import SuppliersPage from './pages/SuppliersPage'
import UnitsPage from './pages/UnitsPage'
import AttributesPage from './pages/AttributesPage'
import StockPage from './pages/StockPage'
import StockEntriesPage from './pages/StockEntriesPage'
import StockExitsPage from './pages/StockExitsPage'
import StockMovementsPage from './pages/StockMovementsPage'
import InventoryCountsPage from './pages/InventoryCountsPage'
import AlertsPage from './pages/AlertsPage'
import UsersPage from './pages/UsersPage'
import RolesPage from './pages/RolesPage'
import ImportExportPage from './pages/ImportExportPage'
import SettingsPage from './pages/SettingsPage'
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
                <Route element={<PermissionRoute permission="dashboard.read" />}>
                  <Route path="dashboard" element={<DashboardPage />} />
                </Route>
                <Route index element={<ProductsPage />} />
                <Route path="products/:id" element={<ProductDetailPage />} />
                <Route path="categories" element={<CategoriesPage />} />
                <Route path="suppliers" element={<SuppliersPage />} />
                <Route path="units" element={<UnitsPage />} />
                <Route path="attributes" element={<AttributesPage />} />
                <Route path="stock" element={<StockPage />} />
                <Route path="stock/entries" element={<StockEntriesPage />} />
                <Route element={<PermissionRoute permission="inventory.read" />}>
                  <Route path="stock/inventories" element={<InventoryCountsPage />} />
                </Route>
                <Route element={<PermissionRoute permission="stock_movement.read" />}>
                  <Route path="stock/movements" element={<StockMovementsPage />} />
                </Route>
                <Route element={<PermissionRoute permission="stock_exit.read" />}>
                  <Route path="stock/exits" element={<StockExitsPage />} />
                </Route>
                <Route element={<PermissionRoute permission="alerts.read" />}>
                  <Route path="alerts" element={<AlertsPage />} />
                </Route>
                <Route element={<PermissionRoute permission="users.read" />}>
                  <Route path="users" element={<UsersPage />} />
                </Route>
                <Route element={<PermissionRoute permission="roles.read" />}>
                  <Route path="roles" element={<RolesPage />} />
                </Route>
                <Route element={<PermissionRoute permission="import.read" />}>
                  <Route path="import-export" element={<ImportExportPage />} />
                </Route>
                <Route element={<PermissionRoute permission="settings.read" />}>
                  <Route path="settings" element={<SettingsPage />} />
                </Route>
                <Route path="documentation" element={<DocumentationPage />} />
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  )
}
