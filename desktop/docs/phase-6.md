# Phase 6 — Catégories (Desktop métier)

**Statut :** module Catégories livré. Phase 6 **arrêtée ici** — pas de Produits (Phase 7), POS, ni installateur.

> Les anciens docs (`installation-client.md`, `installer/README.md`) parlaient encore d’une « Phase 6 installateur `.exe` ». Ce n’est **pas** cette phase. L’installateur / upgrade offline / VM Phase 4.5 est reporté à la **Phase 10**.

---

## Module

**Catégories** (React `/categories`).

Arbre parent/enfant, création racine, sous-catégorie, renommage, suppression, recherche, validation locale du nom, erreurs API, refresh. Enfants restants, produits rattachés et parent invalide **uniquement dans Spring Boot**.

**Rattachement :** le Web n’a pas d’UI de déplacement. L’API `PUT /api/categories/{id}` accepte `parentId` (y compris `null` = racine). Desktop expose **Rattacher** via ce PUT existant — pas de nouvel endpoint.

---

## Architecture JavaFX

```text
GestPovDesktopApp
    → AppFlow          discovery / login / logout (Phase 3 conservée)
    → MainWindow       nav Catalogue : Marques | Catégories
    → BrandsView       Phase 5
    → CategoriesView   Phase 6
    → SessionContext   JWT, user, rôles, permissions, serverId / version
    → ApiClient        HTTP unique
        ├── AuthClient
        ├── BrandClient
        └── CategoryClient
    → CategoryValidator   nom obligatoire (UX seulement)
```

Packages ajoutés : `ui/categories/`, `model/Category`, `net/CategoryClient`.

Aucun `HttpClient` par écran. Le code ne distingue pas `127.0.0.1` et une IP LAN.

---

## Endpoints (existants — aucun ajout backend)

| Méthode | Chemin | Permission |
|---------|--------|------------|
| GET | `/api/categories` | `products.read` |
| GET | `/api/categories/search?nom=` | `products.read` |
| POST | `/api/categories` | `products.create` |
| PUT | `/api/categories/{id}` | `products.update` |
| DELETE | `/api/categories/{id}` | `products.delete` |

Phase 3 inchangée. Phase 5 Marques inchangée.

`MoveCategoryRequest` backend n’est **pas** un déplacement d’arbre (mouvement de stock). Non utilisé.

---

## Permissions UI

| Action | Permission | UI |
|--------|------------|----|
| Voir le module | `products.read` | entrée de nav + arbre |
| Créer racine / sous-cat. | `products.create` | champ + Créer + « + Sous-cat. » |
| Modifier / rattacher | `products.update` | Modifier, Rattacher |
| Supprimer | `products.delete` | Suppr. |

Le masquage n’est **pas** une sécurité : le backend `@PreAuthorize` reste la source de vérité.

**Écart Web :** React `/categories` affiche Modifier / Sous-cat. / Suppr. même sans droit (échec 403). Desktop les masque. Même résultat métier.

---

## Composants

Réutilise Phase 5 : `ErrorBanner`, `EmptyState`, `LoadingOverlay`, `ConfirmationDialog`.

Pas de nouvelle bibliothèque UI.

---

## UI

Après login : barre latérale **Catalogue → Marques | Catégories**.

- Arbre développé par défaut (comme React)
- Création racine (nom obligatoire)
- Sous-catégorie inline
- Édition inline (OK / Annuler)
- Rattacher : liste des catégories + « — Racine — » (PUT `parentId`)
- Confirmation avant suppression
- Recherche → liste plate `nom (parentNom)` puis « Retour à l'arborescence »

Lancer :

```powershell
cd desktop\client
mvn javafx:run
```

Prérequis : backend (`npm run dev:backend`). Compte dev : `admin@erp.local`.

---

## Tests

`mvn -f desktop/client test` : **48** OK (FakeHttpServer, pas de serveur réel).

Couverture Phase 6 : arbre parent/enfant, create racine/enfant, search + `parentNom`, rename, rattachement racine/autre parent, self-parent 400, delete enfants 400, delete liés produits 400, nom vide, 401 sans JWT, mapping DTO, validator.

`mvn -f backend test` : **229** OK. Aucun changement Java backend.

Les tests Discovery HTTP-only n’utilisent plus le port UDP 38471, pour ne pas capter un backend `dev` déjà lancé.

---

## Test réel PostgreSQL → Spring → Desktop

Scénario manuel :

```text
mvn -f desktop/client javafx:run
→ discovery → login → Catégories → charger l’arbre
→ créer une racine → + sous-cat. → rename → rattacher → search → supprimer feuille puis parent
```

---

## Différences Web / Desktop

| Sujet | Web | Desktop |
|-------|-----|---------|
| Recherche | `EntitySearchField` + indices de match | Champ + `GET /api/categories/search` |
| Déplacement | **pas d’UI** (PUT garde le `parentId` actuel au rename) | bouton **Rattacher** (même PUT) |
| Boutons d’écriture | toujours visibles | selon permissions |
| Cycles descendants | non gérés dans l’UI ni (complètement) dans l’API | identique — pas de logique locale |
| Temps réel multi-postes | non | non ; reload après mutation |

---

## Concurrence

Deux postes peuvent modifier la même catégorie. Pas de verrou optimistic. Après chaque succès, rechargement de l’arbre serveur.

---

## Limites / PARTIAL

Métier Catégories : **DONE**.

Non fait (volontaire) : Produits, POS, licence UI, installateur, menus Web complets.

---

## Prochain module (ne pas lancer sans validation)

**Phase 8 — modules métier secondaires** (stocks, fournisseurs, clients, paramètres).
