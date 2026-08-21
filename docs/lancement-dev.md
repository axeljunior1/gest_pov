# Guide de lancement — développement local (Web)

Ce document décrit comment démarrer Gest POV en **mode développeur** (Web/React + Spring Boot), sans impacter la future édition Desktop.

> **Édition Desktop :** voir [`desktop/docs/installation-server.md`](../desktop/docs/installation-server.md) et [`desktop/docs/environment-variables.md`](../desktop/docs/environment-variables.md).

---

## 1. Prérequis

| Outil | Version | Usage |
|-------|---------|--------|
| Java | 17+ | Backend Spring Boot |
| Maven | 3.9+ | Build backend (inclus via wrapper ou install global) |
| Node.js | 20+ | Frontend Vite |
| PostgreSQL | 16 recommandé | Base de données |

**Alternative PostgreSQL :** conteneur Docker avec port exposé :

```powershell
docker run -d --name gest-pov-pg-dev `
  -p 5432:5432 `
  -e POSTGRES_DB=erp_products `
  -e POSTGRES_USER=erp_user `
  -e POSTGRES_PASSWORD=ErpProd2026! `
  postgres:16-alpine
```

---

## 2. Variables d'environnement (dev local)

En développement, le backend utilise le profil **`dev`** (`npm run dev:backend`).  
Les valeurs par défaut sont définies dans `backend/src/main/resources/application-dev.yml`.

### Backend — variables optionnelles

Ces variables **écrasent** les valeurs YAML si elles sont définies dans le shell ou un fichier `.env` local (non commité).

| Variable | Valeur par défaut (dev) | Description |
|----------|-------------------------|-------------|
| `SPRING_PROFILES_ACTIVE` | `dev` (implicite via script) | Profils Spring actifs |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://127.0.0.1:5432/erp_products` | URL JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `erp_user` | Utilisateur PostgreSQL |
| `SPRING_DATASOURCE_PASSWORD` | `ErpProd2026!` | Mot de passe PostgreSQL |

**Exemple PowerShell (session courante) :**

```powershell
$env:SPRING_DATASOURCE_URL = "jdbc:postgresql://127.0.0.1:5432/erp_products"
$env:SPRING_DATASOURCE_USERNAME = "erp_user"
$env:SPRING_DATASOURCE_PASSWORD = "ErpProd2026!"
```

### Frontend — variable optionnelle

| Variable | Valeur par défaut | Description |
|----------|-------------------|-------------|
| `VITE_API_BASE` | `/api` | Préfixe API (build Vite). En dev, le proxy Vite redirige vers `http://127.0.0.1:8080` |

En dev local, **ne pas définir** `VITE_API_BASE` : le proxy dans `frontend/vite.config.js` suffit.

### Comportement spécifique profil `dev`

| Paramètre | Valeur dev | Impact |
|-----------|------------|--------|
| `app.license.enforcement-enabled` | `false` | Pas de blocage licence |
| `app.admin.reset-enabled` | `true` | Reset démo via `/dev-tools` |
| Swagger | activé | http://localhost:8080/swagger-ui.html |

---

## 3. Démarrage en 2 terminaux

### Terminal 1 — Backend

```powershell
cd "c:\Users\axel2\cursor projet"
npm run dev:backend
```

- API : http://localhost:8080  
- Health (public) : http://localhost:8080/actuator/health/liveness  

Attendre le message `Started ProductsApplication` avant de lancer le frontend.

### Terminal 2 — Frontend

```powershell
cd "c:\Users\axel2\cursor projet"
npm run dev:frontend
```

- Interface : http://localhost:5173  

---

## 4. Comptes de test (dev uniquement)

Créés automatiquement au premier démarrage (`AuthReferenceDataInitializer`) :

| Rôle | Email | Mot de passe |
|------|--------|--------------|
| Admin | `admin@erp.local` | `ErpAdmin2026!` |
| Caissier | `caissier@erp.local` | `Caissier2026!` |
| Vendeur | `vendeur@erp.local` | `Vendeur2026!` |

> En **production client** (Docker), ces comptes n'existent pas : le premier admin est créé via `APP_BOOTSTRAP_ADMIN_*` dans `.env`.

---

## 5. Démarrage Docker (Web — alternative)

Pour la stack complète Web (build local) :

```bash
cp .env.example .env   # éditer les secrets
docker compose up --build -d
```

Application : http://localhost (port 80 via Caddy).

Documentation client : [`client-docker-deployment.md`](client-docker-deployment.md).

---

## 6. Dépannage rapide

| Symptôme | Cause probable | Action |
|----------|----------------|--------|
| `Connection refused 127.0.0.1:5432` | PostgreSQL absent ou port non exposé | Démarrer PostgreSQL ou conteneur Docker (§1) |
| `Port 8080 already in use` | Backend déjà lancé | Arrêter l'autre processus ou changer `server.port` |
| Frontend OK, API en erreur | Backend pas prêt | Vérifier logs terminal backend |
| Proxy Vite `ECONNREFUSED 8080` | Backend arrêté | Relancer `npm run dev:backend` |

---

## 7. Fichiers à ne jamais committer

- `.env` (secrets réels)
- `*.lic` / `gest_pov.lic`
- `backups/`
- Clés privées licence
- Tokens Docker Hub

Voir `.gitignore` et `.env.example` pour le modèle documenté.

---

## 8. Desktop Phase 3 (flux discovery + login)

Prérequis : backend profil `dev` déjà démarré (`npm run dev:backend`) — la discovery UDP est activée en `dev`.

```powershell
cd desktop\client
mvn -q test
mvn -q javafx:run
```

Flux attendu : recherche serveur → validation `GET /api/discovery` → login (`POST /api/auth/login`) → affichage `GET /api/auth/me`.

Comptes : `admin@erp.local` / `ErpAdmin2026!`

Le client Desktop ne lit **jamais** `.env` ni les identifiants PostgreSQL.

