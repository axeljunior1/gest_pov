# ERP - Module 1 : Gestion des produits

Application full-stack pour la gestion du catalogue produits (Feature 1).

## Stack

| Couche | Technologie |
|--------|-------------|
| Backend | Spring Boot 3.2, JPA, H2 |
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
.\dev.ps1
```

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
