# ERP - Module 1 : Gestion des produits

Application full-stack pour la gestion du catalogue produits (Feature 1).

## Stack

| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3.2, JPA, PostgreSQL, Flyway |
| Frontend | React 19, Vite, Tailwind CSS 4 |
| Tests | JUnit 5, MockMvc (17 tests) |

## Git local (une feature = un commit)

```powershell
# Premier démarrage (déjà fait si .git existe)
git init

# Après chaque modification conséquente :
.\commit-feature.ps1 "nom-feature" "feat: description courte"

# Option : fusionner directement dans main
.\commit-feature.ps1 "nom-feature" "feat: description" -MergeToMain
```

Branches : `main` (stable) · `feature/nom-feature` (travail en cours)

## Démarrage rapide

```powershell
.\db.ps1      # PostgreSQL (Docker)
.\dev.ps1     # Backend (profil dev) + frontend
```

Comptes par défaut : `admin@erp.local` / `ErpAdmin2026!` · caissier : `caissier@erp.local` / `Caissier2026!`

## Base de données et migrations (Flyway)

Le schéma PostgreSQL est géré par **Flyway** (`backend/src/main/resources/db/migration/`).

| Fichier | Rôle |
|---------|------|
| `V1__baseline_schema.sql` | Schéma complet (nouvelle base) |
| `V2__ensure_pos_and_packaging_columns.sql` | Colonnes POS / conditionnements (idempotent) |

Configuration :

- `spring.jpa.hibernate.ddl-auto: validate` — Hibernate ne modifie plus le schéma
- `spring.flyway.baseline-on-migrate: true` — bases existantes marquées sans rejouer V1

Migration manuelle :

```powershell
cd backend
mvn flyway:migrate
```

## Reset développement

### Script PowerShell `reset-db.ps1`

```powershell
# Purge métier uniquement (garde users, rôles, paramètres, WH-MAIN)
.\reset-db.ps1 -Mode demo

# Purge + jeu de démo (backend doit tourner sur :8080)
.\reset-db.ps1 -Mode demo -SeedDemo

# Reset PostgreSQL complet (volume Docker effacé + Flyway + seed référentiel)
.\reset-db.ps1 -Mode full
```

**Conservé en mode demo :** utilisateurs, rôles, permissions, `app_settings`, unités, entrepôt WH-MAIN.

**Jeu de démo (`-SeedDemo`) :** produit `DEMO-EAU-1L` — unité 500, carton 5 500, palette 250 000, stock 500 L.

### API admin (profil `dev` uniquement)

Backend lancé avec `SPRING_PROFILES_ACTIVE=dev` (fait par `dev.ps1`).

**Interface :** menu Administration → **Outils dev** (build Vite dev + compte SUPER_ADMIN).

```http
GET  /api/admin/dev-tools/status
POST /api/admin/reset-demo
POST /api/admin/seed-demo
Authorization: Bearer <token admin>
X-Reset-Token: dev-reset-token-change-me
```

Jeton configurable : `app.admin.reset-token` ou variable `APP_RESET_TOKEN`.

## Démarrage manuel

## Fonctionnalités couvertes

- Fiches produits (nom, SKU, description, marque, statut, cycle de vie)
- **Unités de mesure globales** avec conversions universelles (kg↔g, L↔mL…)
- **Conditionnements produit** (carton, palette…) avec quantité en unité de base
- **Stock toujours en unité de base** du produit (ex: bouteilles, pas cartons)
- Catégories hiérarchiques illimitées
- Variantes (couleur, taille, SKU, stock, prix, code-barres)
- Codes-barres EAN13, UPC, Code128, QR Code (ZXing)
- Prix achat / vente / promotionnel + historique
- Unités de mesure et conversions
- Multi-fournisseurs avec fournisseur principal
- Images et documents (upload)
- Attributs personnalisés dynamiques
- Recherche avancée avec filtres
- Audit complet des modifications

## Démarrage

### Backend (port 8080)

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.12"
cd backend
mvn spring-boot:run
```

### Frontend (port 5173)

```powershell
cd frontend
npm install
npm run dev
```

Ouvrir http://localhost:5173

### Tests backend

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17.0.12"
cd backend
mvn test
```

## API principale

| Endpoint | Description |
|----------|-------------|
| `GET/POST /api/products` | Liste / création |
| `GET /api/products/{id}` | Détail complet |
| `GET /api/products?query=&categorieId=&stockFaible=` | Recherche avancée |
| `PATCH /api/products/{id}/price` | Mise à jour prix + historique |
| `PATCH /api/products/{id}/lifecycle` | Cycle de vie |
| `GET/POST /api/categories` | Arborescence catégories |
| `GET/POST /api/suppliers` | Fournisseurs |
| `GET/POST /api/units/conversions` | Conversions globales (kg→g) |
| `GET/POST /api/products/{id}/packagings` | Conditionnements produit |
| `POST /api/products/{id}/packagings/convert-to-base` | Réception → stock unité de base |
