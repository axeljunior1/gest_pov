# Phase 7 — Produits (Desktop métier)

**Statut :** module Produits livré (liste + fiche cœur). Phase 7 **arrêtée ici** — pas de Phase 8 (stock/fournisseurs/clients comme modules autonomes).

---

## Module

**Produits** (React `/products` + fiche `/products/:id` / `/products/new`).

Liste filtrée, création / édition de la fiche générale, prix + historique, stock **lecture**, images (si présentes), suppression unitaire et multiple, permissions. SKU unique, cycle de vie serveur, stock réel **uniquement dans Spring Boot**.

**Pagination :** le Web n’en a pas (`GET /api/products` renvoie la liste filtrée). Desktop identique — pas de paramètres de page inventés.

---

## Architecture JavaFX

```text
MainWindow
    → ProductWorkspace
        → ProductsView      liste + filtres
        → ProductFormView   fiche (Général / Prix / Images)
    → CategoryClient, BrandClient, SupplierClient, UnitClient  (listes de référence)
    → ProductClient         GET/POST/PUT/DELETE/PATCH + multipart images
```

HTTP uniquement via `ApiClient` (ajout `patch` + `postMultipart`).

---

## Endpoints (existants — aucun ajout backend)

| Méthode | Chemin | Permission |
|---------|--------|------------|
| GET | `/api/products` | `products.read` |
| GET | `/api/products/{id}` | `products.read` |
| POST | `/api/products` | `products.create` |
| PUT | `/api/products/{id}` | `products.update` |
| DELETE | `/api/products/{id}` | `products.delete` |
| POST | `/api/products/bulk-delete` | `products.delete` |
| PATCH | `/api/products/{id}/price` | `products.update` |
| GET | `/api/products/{id}/price-history` | `products.read` |
| POST | `/api/products/{id}/images` | `products.create` |
| DELETE | `/api/products/{id}/images/{imageId}` | `products.delete` |
| GET | `/api/categories`, `/api/brands`, `/api/suppliers`, `/api/units` | `products.read` |

---

## Permissions UI

| Action | Permission |
|--------|------------|
| Liste / fiche | `products.read` |
| Nouveau + enregistrer (création) | `products.create` |
| Enregistrer / appliquer un prix | `products.update` |
| Suppr. / bulk / supprimer image | `products.delete` |
| Upload image | `products.create` (comme le Web) |

Masquage UI ≠ sécurité. `@PreAuthorize` reste la source de vérité.

---

## UI

Nav **Catalogue → Produits | Catégories | Marques** (défaut : Produits).

**Liste :** recherche, catégorie, fournisseur, marque, cycle de vie, stock faible, rupture, tableau (nom, SKU, catégorie, prix vente, stock, statut, cycle), clic = fiche, suppression + sélection multiple.

**Fiche :** nom, SKU, code-barres + génération EAN, marque, catégorie, fournisseur, unité de base, prix achat/vente, statut, cycle, description, badge stock. Onglets Prix (PATCH + historique) et Images (aperçu URL `/uploads/…`, ajout fichier, suppression).

---

## Tests

`mvn -f desktop/client test` : **58** OK.

Couverture Phase 7 : search + filtres, create/update/delete, bulk-delete, PATCH prix + historique, upload/delete image, nom vide, 401, mapping DTO, validator prix.

Backend : **229** OK. Aucun Java Spring modifié.

---

## Différences Web / Desktop (volontaires)

| Sujet | Web | Desktop |
|-------|-----|---------|
| Pagination | non | non |
| Variantes (création + onglet) | oui | **non** (fiche simple) |
| Conditionnements, documents, attributs, audit | oui | **non** |
| Fournisseurs liés au produit | onglet | fournisseur principal seulement |
| Workflow submit/approve/reject | oui | cycle de vie via PUT fiche |
| Galerie codes-barres | oui | code-barres champ texte |
| Recherche | `EntitySearchField` | champ texte + `GET /api/products?query=` |
| Boutons d’écriture | souvent toujours visibles | selon permissions |

---

## Limites / PARTIAL vs fiche Web complète

Cœur catalogue (liste + fiche générale + prix + stock lu + images) : **DONE**.

Non fait : variantes, conditionnements, documents, attributs custom, audit, workflow validation, POS.

---

## Prochain module (ne pas lancer sans validation)

**Phase 8 — modules métier secondaires** (stocks, fournisseurs, clients, paramètres, autres référentiels).
