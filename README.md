# ERP Gest_POV — Gestion produits & POS

## Structure du dépôt

```
.
├── backend/              # API Spring Boot
├── frontend/             # Interface React (Vite)
├── docker-compose.yml    # Stack Docker (build local + proxy + tunnel)
├── Caddyfile             # Reverse proxy /api → backend
├── .env.example          # Variables Docker
├── play/                 # Tests E2E Playwright
├── docs/                 # Documentation utilisateur
├── deploy/               # Compose images pré-buildées, notes déploiement
├── scripts/              # Utilitaires (reset SQL, génération PDF)
└── images/               # Archives Docker pour livraison client (.tar)
```

## Démarrage rapide (développement)

**Prérequis :** Java 17, Node 20, PostgreSQL (ou Docker).

```bash
# Terminal 1 — backend (profil dev)
npm run dev:backend

# Terminal 2 — frontend
npm run dev:frontend
```

Frontend : http://localhost:5173 · API : http://localhost:8080

Comptes seed : `admin@erp.local` / `ErpAdmin2026!` · caissier : `caissier@erp.local` / `Caissier2026!`

## Docker (production / démo)

```bash
cp .env.example .env
docker compose up --build -d
```

→ http://localhost

Voir [deploy/README.md](deploy/README.md) pour les images pré-buildées et le tunnel Cloudflare.

## Tests E2E

```bash
cd play && npm install
npm run test:e2e
```

(App backend + frontend doivent tourner.)

## Documentation

- [Guide utilisateur](docs/GUIDE_UTILISATEUR.md) (PDF : `docs/GUIDE_UTILISATEUR.pdf`)
- Migrations Flyway : `backend/src/main/resources/db/migration/`

## Reset données (profil dev)

Outils admin : http://localhost:5173/dev-tools (backend en profil `dev`).

API : `POST /api/admin/reset-demo` et `seed-demo` avec en-tête `X-Reset-Token` (voir `application-dev.yml`).
