# Phase 5 — Choix du premier module

**Module choisi :** Catalogue — **Marques** (`/brands` → `BrandsView`)

---

## Pourquoi celui-ci

Il valide tout le contrat architectural demandé, sans emporter la complexité POS ni la fiche produit :

| Critère | Couverture Marques |
|---------|--------------------|
| Lecture | `GET /api/brands` |
| Écriture | `POST` création, `PUT` modification |
| Suppression | `DELETE` (existant Web/API) |
| Validation locale | nom obligatoire (trim) |
| Validation métier | unicité du nom, suppression si produits liés — **backend** |
| Permissions | `products.read` / `create` / `update` / `delete` |
| Erreurs | 401, 403, 400 métier, réseau |
| Rafraîchissement | reload après mutation + bouton Actualiser |

Un seul champ métier (`nom`). Parité Web réelle (liste, recherche, CRUD, messages). Idéal comme **modèle** pour Catégories, Fournisseurs, etc.

---

## Endpoints

| Méthode | Chemin | Permission |
|---------|--------|------------|
| GET | `/api/brands` | `products.read` |
| GET | `/api/brands/search?nom=` | `products.read` |
| GET | `/api/brands/{id}` | `products.read` |
| POST | `/api/brands` | `products.create` |
| PUT | `/api/brands/{id}` | `products.update` |
| DELETE | `/api/brands/{id}` | `products.delete` |

Aucun endpoint ajouté.

---

## Complexité

**Faible / maîtrisée.** Pas d’arbre, pas de variantes, pas de pagination API (liste complète comme le Web), pas de fichier.

Risques : concurrence deux postes (dernier `PUT` gagne ; unicité renvoyée en 400) ; suppression refusée si produits liés.

---

## Pourquoi pas les autres en premier

| Module | Raison de report |
|--------|------------------|
| POS | Plus complexe de Gest POV (sessions, stock, paiements, tickets). Priorité métier plus tard, pas comme gabarit. |
| Produits | Variantes, cycle de vie, images, conditionnements. |
| Catégories | CRUD + **arbre parent/enfant** — bon 2e module, pas le 1er. |
| Unités | Catalogue de référence + conversions SI, peu représentatif d’un CRUD libre. |
| Clients / Users / Stock | Champs nombreux, règles métier lourdes. |
| Dashboard / Analytics | Surtout lecture, peu d’écriture. |

**Prochain module recommandé :** Catégories (même famille catalogue, arbre en plus).
