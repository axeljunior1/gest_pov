# Matrice migration — React Web → Desktop JavaFX

**Légende statut :** `DONE` | `PARTIAL` | `TODO` | `NOT_APPLICABLE`

Référence routes : `frontend/src/App.jsx`

**Phase 5 (gabarit) :** Marques — [`phase-5.md`](phase-5.md).  
**Phase 6 :** Catégories — [`phase-6.md`](phase-6.md).  
**Phase 7 :** Produits — [`phase-7.md`](phase-7.md).  
**Phase 8 :** Fournisseurs, clients, stock lecture, unités, paramètres — [`phase-8.md`](phase-8.md).  
**Phase 9 :** POS cœur — [`phase-9.md`](phase-9.md).  
**Phase 10 :** Nav, raccourcis, packages USB — [`phase-10.md`](phase-10.md).

---

## POS (priorité métier, complexité élevée — pas le 1er gabarit)

| Écran React | Route | API principale | Écran Desktop | Statut |
|-------------|-------|----------------|---------------|--------|
| Préparation ventes | `/pos` | `/api/pos/*` | `PosView` | PARTIAL |
| Encaissement | `/pos/pending` | `/api/pos/sales`, validate | (même `PosView`) | PARTIAL |
| Historique ventes | `/pos/history` | `/api/pos/sales` | `PosHistoryView` | TODO |
| Rapports caisse | `/pos/reports` | `/api/pos/sessions` | `PosReportsView` | TODO |
| Retours POS | `/pos/returns` | `/api/pos/returns` | `PosReturnsView` | TODO |

---

## Authentification & licence

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Login | `/login` | `POST /api/auth/login` | `LoginView` (via `AppFlow`) | DONE |
| Activation licence | (gate) | `/api/license/*` | `LicenseView` | TODO |
| Discovery serveur | — | `GET /api/discovery` | `ServerDiscoveryView` (via `AppFlow`) | DONE |

---

## Dashboard & analytics

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Tableau de bord | `/dashboard` | `/api/dashboard/*` | — | TODO |
| Analytics | `/analytics` | `/api/analytics/*` | — | TODO |
| Ventes annulées | `/analytics/cancellations` | `/api/sales/cancellations` | — | TODO |

---

## Ventes back-office

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Liste ventes | `/sales` | `/api/sales` | — | TODO |
| Détail vente | `/sales/:id` | `/api/sales/{id}` | — | TODO |
| Retours | `/returns` | `/api/...` | — | TODO |

---

## Catalogue produits

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Produits | `/products` | `/api/products` | `ProductsView` | DONE |
| Détail produit | `/products/:id` | `/api/products/{id}` | `ProductFormView` | PARTIAL |
| Catégories | `/categories` | `/api/categories` | `CategoriesView` | DONE |
| Marques | `/brands` | `/api/brands` | `BrandsView` | DONE |
| Fournisseurs | `/suppliers` | `/api/suppliers` | `SuppliersView` | DONE |
| Unités | `/units` | `/api/units` | `UnitsView` | PARTIAL |
| Attributs | `/attributes` | `/api/attributes` | — | TODO |

**Marques DONE :** liste, recherche API, CRUD, validation nom, permissions `products.*`.  
**Catégories DONE :** arbre, sous-catégorie, rename, rattachement PUT `parentId`, recherche, suppression.  
**Produits DONE (cœur) :** liste filtrée, CRUD fiche générale (catégorie, marque, fournisseur, unité, prix, statut, cycle), stock lecture, images, PATCH prix + historique, bulk delete, permissions.  
**Unités PARTIAL :** nom + symbole, pas de conversions.  
**Stock PARTIAL :** liste `GET /api/stock/items` uniquement.

---

## Stock

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Stock | `/stock` | `/api/stock` | `StockView` | PARTIAL |
| Valorisation | `/stock/valuation` | `/api/stock/valuation` | — | TODO |
| Entrées | `/stock/entries` | `/api/stock/entries` | — | TODO |
| Sorties | `/stock/exits` | `/api/stock/exits` | — | TODO |
| Mouvements | `/stock/movements` | `/api/stock/movements` | — | TODO |
| Inventaires | `/stock/inventories` | `/api/stock/inventories` | — | TODO |
| Bons commande | `/purchase-orders` | `/api/purchase-orders` | — | TODO |

---

## Clients & admin

| Écran React | Route | API | Desktop | Statut |
|-------------|-------|-----|---------|--------|
| Clients | `/customers` | `/api/customers` | `CustomersView` | DONE |
| Utilisateurs | `/users` | `/api/users` | — | TODO |
| Rôles | `/roles` | `/api/roles` | — | TODO |
| Alertes | `/alerts` | `/api/alerts` | — | TODO |
| Import/Export | `/import-export` | `/api/import`, export | — | TODO |
| Paramètres | `/settings` | `/api/settings` | `SettingsView` | PARTIAL |
| Config client | `/configuration` | settings | — | TODO |

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
| Connexion REST | `ApiClient` + clients métier (brands, categories, products, suppliers, units, customers, stock, settings, pos) | DONE |
| Session JWT / permissions | `SessionContext` | DONE |
| Fenêtre principale | `MainWindow` | DONE |
| Health local serveur | `ServerHealthPanel` | TODO |

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
7. Package USB serveur + client — **DONE** dossiers ; `.exe` et VM LAN — Phase 10 restant

---

## Mise à jour

Mettre à jour ce fichier à chaque écran Desktop livré.
