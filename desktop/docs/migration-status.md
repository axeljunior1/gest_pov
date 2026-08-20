# Matrice migration — React Web → Desktop JavaFX

**Légende statut :** `DONE` | `PARTIAL` | `TODO` | `NOT_APPLICABLE`

Référence routes : `frontend/src/App.jsx`

**Phase 5 (gabarit) :** Marques — [`phase-5.md`](phase-5.md).  
**Phase 6 :** Catégories — [`phase-6.md`](phase-6.md).  
**Phase 7 :** Produits — [`phase-7.md`](phase-7.md).  
**Phase 8 :** Fournisseurs, clients, stock lecture, unités, paramètres — [`phase-8.md`](phase-8.md).  
**Phase 9 :** POS cœur — [`phase-9.md`](phase-9.md).  
**Phase 10 :** Nav, raccourcis, packages USB — [`phase-10.md`](phase-10.md).  
**Phase H (ops) :** Installateurs `.exe` / VM LAN / backlog MFA — [`phase-h.md`](phase-h.md) (**documenté**, items TODO).

---

## POS (priorité métier, complexité élevée — pas le 1er gabarit)

| Écran React | Route | API principale | Écran Desktop | Statut |
|-------------|-------|----------------|---------------|--------|
| Préparation ventes | `/pos` | `/api/pos/*` | `PosView` | DONE |
| Encaissement | `/pos/pending` | `/api/pos/sales`, validate | (même `PosView`) | DONE |
| Historique ventes | `/pos/history` | `/api/pos/sales/completed` | `PosHistoryView` | DONE |
| Rapports caisse | `/pos/reports` | `/api/pos/sessions` | `PosReportsView` | DONE |
| Retours POS | `/pos/returns` | `/api/pos/returns` | `PosReturnsView` | DONE |

---

## Authentification & licence

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Login | `/login` | `POST /api/auth/login` | `LoginView` (via `AppFlow`) | DONE |
| Activation licence | (gate) | `/api/license/*` | `LicenseView` | DONE |
| Discovery serveur | — | `GET /api/discovery` | `ServerDiscoveryView` (via `AppFlow`) | DONE |

---

## Dashboard & analytics

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Tableau de bord | `/dashboard` | `/api/dashboard/*` | `DashboardView` | DONE |
| Analytics | `/analytics` | `/api/analytics/*` | `AnalyticsView` | DONE |
| Ventes annulées | `/analytics/cancellations` | `/api/sales/cancellations` | (onglet `AnalyticsView`) | DONE |

---

## Ventes back-office

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Liste ventes | `/sales` | `/api/sales/browse` | `SalesListView` | DONE |
| Détail vente | `/sales/:id` | `/api/sales/{id}` | `SaleDetailView` | DONE |
| Retours | `/returns` | `/api/...` | `PosReturnsView` | PARTIAL |

---

## Catalogue produits

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Produits | `/products` | `/api/products` | `ProductsView` | DONE |
| Détail produit | `/products/:id` | `/api/products/{id}` | `ProductFormView` | DONE |
| Catégories | `/categories` | `/api/categories` | `CategoriesView` | DONE |
| Marques | `/brands` | `/api/brands` | `BrandsView` | DONE |
| Fournisseurs | `/suppliers` | `/api/suppliers` | `SuppliersView` | DONE |
| Unités | `/units` | `/api/units` | `UnitsView` | DONE |
| Attributs | `/attributes` | `/api/attributes` | `AttributesView` | DONE |

**Produits :** liste + fiche (variantes, conditionnements, images, workflow, audit).  
**Unités :** CRUD symbole + conversions SI.  
**Stock :** items + mouvements + entrepôts + docs + inventaires + valorisation + PO.

---

## Stock

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Stock | `/stock` | `/api/stock` | `StockView` | DONE |
| Valorisation | `/stock/valuation` | `/api/stock/valuation` | `StockValuationView` | DONE |
| Entrées | `/stock/entries` | `/api/stock/entries` | `StockEntriesExitsView` | DONE |
| Sorties | `/stock/exits` | `/api/stock/exits` | `StockEntriesExitsView` | DONE |
| Mouvements | `/stock/movements` | `/api/stock/movements` | (onglet Stock) | DONE |
| Inventaires | `/stock/inventories` | `/api/stock/inventories` | `InventoriesView` | DONE |
| Bons commande | `/purchase-orders` | `/api/purchase-orders` | `PurchaseOrdersView` | DONE |
| Entrepôts | `/warehouses` | `/api/warehouses` | `WarehousesView` | DONE |
| Transferts | — | `/api/stock/transfers` | `StockTransfersView` | DONE |

---

## Clients & admin

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Clients | `/customers` | `/api/customers` | `CustomersView` | DONE |
| Utilisateurs | `/users` | `/api/users` | `UsersView` | DONE |
| Rôles | `/roles` | `/api/roles` | `RolesView` | DONE |
| Alertes | `/alerts` | `/api/alerts` | `AlertsView` | DONE |
| Import/Export | `/import-export` | `/api/import`, export | `ImportExportView` | DONE |
| Paramètres | `/settings` | `/api/settings` | `SettingsView` | PARTIAL |
| Config client | `/configuration` | settings | `ClientConfigurationView` | PARTIAL |

---

## Outils dev (Web only)

| Écran React | Route | Desktop | Statut |
|-------------|-------|---------|--------|
| Dev tools | `/dev-tools` | — | NOT_APPLICABLE |
| Demo data | `/demo-data` | — | NOT_APPLICABLE |
| Documentation | `/documentation` | — | NOT_APPLICABLE |

---

## Infrastructure Desktop (nouveau)

| Fonction | Desktop | Statut |
|----------|---------|--------|
| Config client | `ClientConfigStore` | DONE |
| Discovery LAN | `DiscoveryService` + UDP | DONE |
| Connexion REST | `ApiClient` + clients métier (+ users, roles, alerts, import/export, license, dashboard, analytics, sales) | DONE |
| Session JWT / permissions | `SessionContext` | DONE |
| Fenêtre principale | `MainWindow` (ADMIN / Dashboard / Analytics / health) | DONE |
| Health local serveur | Indicateur top-bar (`/api/health` + fallback discovery) | DONE |
| Package dossier serveur/client USB | `build-offline-package.ps1` / `build-client-package.ps1` | DONE |
| Installateurs `.exe` (jpackage / WiX) | stub `build-exe-stub.ps1` + [phase-h.md](phase-h.md) | TODO |
| Validation VM LAN complète | [phase-4-validation.md](phase-4-validation.md) | TODO (NOT_EXECUTED) |
| MFA / reset password e-mail / ESC-POS native | backlog [phase-h.md](phase-h.md) | TODO |

---

## Audit complexité (aide au choix)

| Module | UI | Écriture | Métier dans l’UI | Difficulté |
|--------|----|----------|------------------|------------|
| Marques | liste + 1 champ | CRUD | unicité / liens produits → API | faible — **gabarit Phase 5** |
| Catégories | arbre | CRUD + parent | cycle / enfants → API | moyenne |
| Fournisseurs | liste | CRUD | proche marques | faible |
| Unités | groupes + conversions | surtout lecture | conversions SI | moyenne |
| Produits | fiche lourde | beaucoup | cycle de vie, variantes | très élevée |
| POS | workspace | élevé | stock, caisse, tickets | très élevée |
| Stock / inventaires | workflows | élevé | validation documents | élevée |
| Users / rôles | admin | élevé | matrice permissions | élevée |

---

## Priorisation recommandée

1. Login + discovery (**DONE**) + licence UI
2. **Marques** (**DONE** — gabarit Phase 5)
3. **Catégories** (**DONE** — Phase 6)
4. Produits (Phase 7) — **DONE** (cœur liste + fiche ; variantes/conditionnements PARTIAL)
5. Modules secondaires — **DONE** (Phase 8 : fournisseurs, clients, stock lecture, unités, paramètres)
6. POS cœur — **PARTIAL** (Phase 9)
7. Package USB serveur + client — **DONE** dossiers ; `.exe` / VM LAN / backlog ops — **Phase H** ([phase-h.md](phase-h.md)) documenté, TODO
8. Admin + dashboard + ventes BO — **DONE** (Phases E/F/G MVP)

---

## Mise à jour

Mettre à jour ce fichier à chaque écran Desktop livré.

**2026-08-20 — Phases E/F/G MVP :** Users/Roles/Alerts/ImportExport, Licence + health top-bar, Dashboard/Analytics/Sales BO branchés dans `MainWindow`.
