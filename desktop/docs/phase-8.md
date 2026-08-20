# Phase 8 — Modules métier secondaires (Desktop)

**Statut :** Fournisseurs, clients, stock (lecture), unités, paramètres livrés. POS = Phase 9.

---

## Modules

| Écran | Route Web | API | Permission |
|-------|-----------|-----|------------|
| Fournisseurs | `/suppliers` | `/api/suppliers` | `products.read` / create / update / delete |
| Unités | `/units` | `/api/units` | `products.read` / create / delete |
| Clients | `/customers` | `/api/customers` | `customer.read` / create / update / delete |
| Stock | `/stock` | `GET /api/stock/items` | `stock.read` |
| Paramètres | `/settings` | `/api/settings` | `settings.read` / `settings.update` |

Règles métier (unicité, liaisons produits, quantités réelles) **uniquement dans Spring Boot**.

---

## Architecture

```text
MainWindow
    → SuppliersView / UnitsView / CustomersView / StockView / SettingsView
    → SupplierClient, UnitClient, CustomerClient, StockClient, SettingsClient
```

HTTP uniquement via `ApiClient`. Aucun nouvel endpoint backend.

---

## UI

Nav après login :

- **CATALOGUE** — Produits, Catégories, Marques, Fournisseurs, Unités
- **STOCK** — Stock (liste filtrable, pas d’entrées/sorties)
- **VENTES** — Caisse POS (Phase 9), Clients
- **PARAMÈTRES** — clé / valeur

---

## Limites / PARTIAL

| Sujet | État |
|-------|------|
| Fournisseurs / clients CRUD | DONE |
| Unités (nom + symbole) | DONE — pas de conversions / groupes SI |
| Stock | lecture `items` seulement — pas d’entrées, sorties, inventaires, valorisation |
| Paramètres | liste + édition d’une valeur — pas de logo, numérotation, fidélité dédiée |

---

## Tests

`mvn -f desktop/client test` : **71** OK (FakeHttpServer). Couverture : CRUD fournisseurs/clients/unités, liste stock, GET/PUT paramètres.

Aucun Java Spring modifié.
