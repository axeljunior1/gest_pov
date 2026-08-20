# Phase 5 — Premier module métier Desktop (Marques)

**Statut :** module Marques livré comme gabarit architectural. Phase 5 **arrêtée ici** — pas d’industrialisation des autres écrans.

**Choix détaillé :** [`phase-5-module-choice.md`](phase-5-module-choice.md)

---

## Module

**Marques** (React `/brands`).

Lecture, création, modification, suppression, recherche, validation locale du nom, erreurs API, refresh. Règles d’unicité et « marque liée à des produits » **uniquement dans Spring Boot**.

---

## Architecture JavaFX

```text
GestPovDesktopApp
    → AppFlow          discovery / login / logout (Phase 3 conservée)
    → MainWindow       navigation + zone contenu + user + logout
    → BrandsView       premier écran métier
    → SessionContext   JWT, user, rôles, permissions, serverId / version
    → ApiClient        HTTP unique (GET/POST/PUT/DELETE, 401→login)
        ├── AuthClient
        └── BrandClient
    → BrandValidator   nom obligatoire (UX seulement)
```

Packages : `session/`, `net/`, `model/`, `service/`, `ui/`, `ui/component/`, `ui/brands/`, `util/`.

Aucun `HttpClient` par écran. Le code ne distingue pas `127.0.0.1` et une IP LAN.

---

## Endpoints (existants — aucun ajout backend)

| Méthode | Chemin | Permission |
|---------|--------|------------|
| GET | `/api/brands` | `products.read` |
| GET | `/api/brands/search?nom=` | `products.read` |
| POST | `/api/brands` | `products.create` |
| PUT | `/api/brands/{id}` | `products.update` |
| DELETE | `/api/brands/{id}` | `products.delete` |

Phase 3 inchangée : `GET /api/discovery`, `POST /api/auth/login`, `GET /api/auth/me`.

---

## Permissions UI

| Action | Permission | UI |
|--------|------------|----|
| Voir le module | `products.read` | entrée de nav + table |
| Créer | `products.create` | champ + bouton Créer |
| Modifier | `products.update` | Modifier, double-clic |
| Supprimer | `products.delete` | Suppr. |

Le masquage n’est **pas** une sécurité : le backend `@PreAuthorize` reste la source de vérité.

**Écart Web :** React `/brands` affiche Créer/Modifier/Suppr. même sans droit (échec 403). Desktop les masque. Même résultat métier.

---

## Composants créés (besoin réel)

- `ErrorBanner`
- `EmptyState`
- `LoadingOverlay` (thread UI non bloqué : `FxAsync`)
- `ConfirmationDialog`

Pas de bibliothèque UI exhaustive.

---

## UI

Après login : barre latérale **Catalogue → Marques**, utilisateur + déconnexion, table + recherche + actualiser.

- État vide : « Aucune marque — créez-en une ci-dessus. »
- Recherche sans résultat : « Aucun résultat »
- Édition inline (OK / Annuler) ; saisie conservée si l’API refuse
- Confirmation avant suppression
- CSS : `desktop/client/src/main/resources/com/gestpov/desktop/ui/styles.css`

Lancer :

```powershell
cd desktop\client
mvn javafx:run
```

Prérequis : backend (`npm run dev:backend`). Compte dev : `admin@erp.local`.

---

## Tests

`mvn -f desktop/client test` : **34** OK (FakeHttpServer, pas de serveur réel).

Couverture : ApiClient 200/401/403/500/timeout/JSON invalide ; BrandClient CRUD + unicité + suppression liée ; BrandValidator ; SessionContext login/logout/401 ; mapping DTO ; Phase 3 discovery/login conservés.

`mvn -f backend test` : **225** OK. Aucun changement Java backend.

---

## Test réel PostgreSQL → Spring → Desktop

- Liveness `8080` : UP (processus déjà lancé sur la machine).
- `POST /api/auth/login` et `GET /api/discovery` : **403** sur ce processus (licence / stack déjà en cours, hors Phase 5). **Non rejoué en UI Desktop cliquée.**
- CRUD Marques : **prouvé** via FakeHttpServer + `BrandControllerTest` backend existant.

Scénario manuel à rejouer (backend `dev` + licence off ou `.lic` valide) :

```text
mvn -f desktop/client javafx:run
→ discovery → login → Marques → charger → créer → voir en liste → actualiser
```

---

## Différences Web / Desktop

| Sujet | Web | Desktop |
|-------|-----|---------|
| Recherche | `EntitySearchField` + indices de match | Champ + `GET /api/brands/search` |
| Lien « Créer un produit » | oui | non (produits non migrés) |
| Boutons d’écriture | toujours visibles | selon permissions |
| Pagination | non (liste complète) | identique |
| Temps réel multi-postes | non | non ; reload après mutation ; 400 unicité affiché |

---

## Concurrence

Deux postes peuvent modifier la même marque. Pas de verrou optimistic. Dernier `PUT` gagne ; nom déjà pris → message backend. Après chaque succès, rechargement serveur.

---

## Limites / PARTIAL

Métier Marques : **DONE**.

Non fait (volontaire) : POS, produits, catégories, licence UI, menus Web complets.

---

## Prochain module recommandé

**Prochain module recommandé :** Produits (Phase 7). Ne pas lancer sans validation de la Phase 6.
