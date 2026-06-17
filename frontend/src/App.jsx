import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { LicenseGate } from './pages/ActivationPage'
import ProtectedRoute from './components/ProtectedRoute'
import PermissionRoute from './components/PermissionRoute'
import BackOfficeRoute from './components/BackOfficeRoute'
import Layout from './components/Layout'
import POSLayout from './components/POSLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import AnalyticsPage from './pages/AnalyticsPage'
import CancelledSalesPage from './pages/CancelledSalesPage'
import SalesPage from './pages/SalesPage'
import SaleDetailPage from './pages/SaleDetailPage'
import ReturnsPage from './pages/ReturnsPage'
import ReturnDetailPage from './pages/ReturnDetailPage'
import HomeRedirect from './components/HomeRedirect'
import ProductDetailPage from './pages/ProductDetailPage'
import CategoriesPage from './pages/CategoriesPage'
import BrandsPage from './pages/BrandsPage'
import SuppliersPage from './pages/SuppliersPage'
import UnitsPage from './pages/UnitsPage'
import AttributesPage from './pages/AttributesPage'
import PurchaseOrdersPage from './pages/PurchaseOrdersPage'
import StockPage from './pages/StockPage'
import StockEntriesPage from './pages/StockEntriesPage'
import StockExitsPage from './pages/StockExitsPage'
import StockMovementsPage from './pages/StockMovementsPage'
import InventoryCountsPage from './pages/InventoryCountsPage'
import StockValuationPage from './pages/StockValuationPage'
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
import PosSessionReportsPage from './pages/PosSessionReportsPage'
import PosReturnsPage from './pages/PosReturnsPage'

export default function App() {
  return (
    <LicenseGate>
      <AuthProvider>
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
                  <Route element={<PermissionRoute permission="pos.report.read" />}>
                    <Route path="pos/reports" element={<PosSessionReportsPage />} />
                  </Route>
                  <Route element={<PermissionRoute anyOf={['pos.return.create', 'pos.sale.refund', 'pos.return.read']} />}>
                    <Route path="pos/returns" element={<PosReturnsPage />} />
                  </Route>
                </Route>
              </Route>
              <Route element={<BackOfficeRoute />}>
                <Route element={<Layout />}>
                  <Route element={<PermissionRoute permission="dashboard.read" />}>
                    <Route path="dashboard" element={<DashboardPage />} />
                  </Route>
                  <Route element={<PermissionRoute anyOf={['analytics.read', 'analytics.sales.read', 'sales.cancellations.read']} />}>
                    <Route path="analytics" element={<AnalyticsPage />} />
                    <Route path="analytics/cancellations" element={<CancelledSalesPage />} />
                  </Route>
                  <Route element={<PermissionRoute anyOf={['pos.sale.read', 'pos.sale.read_own', 'analytics.sales.read', 'pos.report.read']} />}>
                    <Route path="sales" element={<SalesPage />} />
                    <Route path="sales/:id" element={<SaleDetailPage />} />
                  </Route>
                  <Route element={<PermissionRoute anyOf={['pos.return.read', 'analytics.sales.read', 'pos.report.read']} />}>
                    <Route path="returns" element={<ReturnsPage />} />
                    <Route path="returns/:id" element={<ReturnDetailPage />} />
                  </Route>
                  <Route element={<PermissionRoute permission="products.read" />}>
                    <Route index element={<HomeRedirect />} />
                    <Route path="products/:id" element={<ProductDetailPage />} />
                    <Route path="categories" element={<CategoriesPage />} />
                    <Route path="brands" element={<BrandsPage />} />
                    <Route path="suppliers" element={<SuppliersPage />} />
                    <Route path="units" element={<UnitsPage />} />
                    <Route path="attributes" element={<AttributesPage />} />
                  </Route>
                  <Route element={<PermissionRoute permission="stock.read" />}>
                    <Route path="stock" element={<StockPage />} />
                    <Route path="stock/valuation" element={<StockValuationPage />} />
                  </Route>
                  <Route element={<PermissionRoute permission="stock_entry.read" />}>
                    <Route path="stock/entries" element={<StockEntriesPage />} />
                    <Route path="purchase-orders" element={<PurchaseOrdersPage />} />
                  </Route>
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
      </AuthProvider>
    </LicenseGate>
  )
}
