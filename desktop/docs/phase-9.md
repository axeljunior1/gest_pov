# Phase 9 — POS (Desktop)

**Statut :** caisse cœur livrée (session, recherche, panier, paiement, ticket JSON). Pas de retours / reports / double poste préparateur-caissier.

---

## Module

**Point de vente** (cœur de React `/pos`).

- Ouverture de session (`POST /api/pos/sessions/open`)
- Recherche catalogue (`GET /api/pos/catalog/search`)
- Création vente, lignes, quantité, remise ligne
- Encaissement CASH / CARD / MOBILE_MONEY (`POST /api/pos/sales/{id}/validate`)
- Ticket JSON (`GET /api/pos/sales/{id}/ticket`)
- Message stock insuffisant si `hasStockIssues`

Prix, stock, totaux, validation **uniquement dans Spring Boot**.

---

## Permissions UI

| Action | Permission |
|--------|------------|
| Voir la caisse | `pos.sale.read` |
| Ouvrir la session | `pos.session.open` (ou droits préparateur existants côté API) |
| Ajouter des lignes | `pos.sale.create` |
| Remise | `pos.sale.discount` |
| Encaisser | `pos.payment.collect` / `pos.sale.validate` / `pos.payment.validate` |
| Ticket | `pos.ticket.print` |

---

## Raccourcis

| Touche | Action |
|--------|--------|
| F4 | Ouvrir la caisse |
| F2 | Focus recherche produit (écran POS) |

---

## Limites / PARTIAL vs Web

Non fait (volontaire) : file d’attente caissier, ventes en attente / hold, retours, rapports de caisse, fidélité, impression ticket physique, variantes / conditionnements, scan avancé.

Concurrence : deux caisses peuvent vendre le même stock ; le backend refuse ou signale selon les règles serveur. Reload après chaque mutation.

---

## Tests

`PosClientTest` : session, search, add line, qty, remise, validate, ticket. FakeHttpServer, pas de PostgreSQL.
