import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { NotificationProvider } from './context/NotificationContext'
import ProtectedRoute from './components/ProtectedRoute'
import PermissionRoute from './components/PermissionRoute'
import BackOfficeRoute from './components/BackOfficeRoute'
import Layout from './components/Layout'
import POSLayout from './components/POSLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import AnalyticsPage from './pages/AnalyticsPage'
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
import DevToolsPage from './pages/DevToolsPage'
import SuperAdminRoute from './components/SuperAdminRoute'
import DocumentationPage from './pages/DocumentationPage'
import CustomersPage from './pages/CustomersPage'
import POSPage from './pages/POSPage'
import PosPendingPaymentsPage from './pages/PosPendingPaymentsPage'
import PosSalesHistoryPage from './pages/PosSalesHistoryPage'

export default function App() {
  return (
    <AuthProvider>
      <NotificationProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<LoginPage />} />
            <Route element={<ProtectedRoute />}>
              <Route element={<PermissionRoute permission="pos.sale.read" />}>
                <Route element={<POSLayout />}>
                  <Route element={<PermissionRoute anyOf={['pos.sale.send_to_payment', 'pos.sale.create', 'pos.sale.prepare']} />}>
                    <Route path="pos" element={<POSPage />} />
                  </Route>
                  <Route element={<PermissionRoute anyOf={['pos.payment.collect', 'pos.payment.validate']} />}>
                    <Route path="pos/pending" element={<PosPendingPaymentsPage />} />
                  </Route>
                  <Route element={<PermissionRoute anyOf={['pos.ticket.print', 'pos.ticket.reprint', 'pos.report.read']} />}>
                    <Route path="pos/history" element={<PosSalesHistoryPage />} />
                  </Route>
                </Route>
              </Route>
              <Route element={<BackOfficeRoute />}>
                <Route element={<Layout />}>
                  <Route element={<PermissionRoute permission="dashboard.read" />}>
                    <Route path="dashboard" element={<DashboardPage />} />
                  </Route>
                  <Route element={<PermissionRoute anyOf={['analytics.read', 'analytics.sales.read']} />}>
                    <Route path="analytics" element={<AnalyticsPage />} />
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
                  <Route element={<SuperAdminRoute />}>
                    <Route path="dev-tools" element={<DevToolsPage />} />
                  </Route>
                  <Route element={<PermissionRoute permission="customer.read" />}>
                    <Route path="customers" element={<CustomersPage />} />
                    <Route path="customers/:id" element={<CustomersPage />} />
                  </Route>
                  <Route path="documentation" element={<DocumentationPage />} />
                </Route>
              </Route>
            </Route>
          </Routes>
        </BrowserRouter>
      </NotificationProvider>
    </AuthProvider>
  )
}
