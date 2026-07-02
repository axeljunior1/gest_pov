# Déploiement client — Gest_POV (Docker)

Guide d'installation on-premise pour un client. Le mode **dev local** (npm / compose racine sans `.env` obligatoire) reste inchangé.

---

## 1. Prérequis

- Docker Engine 24+ et Docker Compose v2
- 4 Go RAM minimum (8 Go recommandé)
- Ports : **80** (ou `APP_PORT` dans `.env`) sur `127.0.0.1` par défaut
- Fichier **licence** `.lic` (fourni par l'éditeur après récupération de l'installation ID)

---

## 2. Fichiers compose

| Fichier | Usage |
|---------|--------|
| **`docker-compose.client.yml`** | **Production client** — build local + `.env` obligatoire |
| `deploy/compose.client.images.yml` | Client avec images `.tar` pré-buildées |
| `docker-compose.yml` | Dev / démo rapide (defaults, pas de `.env` requis) |
| `docker-compose.tunnel.yml` | Overlay **optionnel** Cloudflare (POC uniquement) |

---

## 3. Installation (build sur site)

```bash
# 1. Copier et éditer les secrets
cp .env.example .env
# Renseigner POSTGRES_PASSWORD, APP_JWT_SECRET, APP_BOOTSTRAP_ADMIN_EMAIL, APP_BOOTSTRAP_ADMIN_PASSWORD

# 2. Démarrer
./scripts/client-start.sh
# ou :
docker compose -f docker-compose.client.yml --env-file .env up --build -d

# 3. Accès
# http://127.0.0.1  (ou APP_BIND:APP_PORT du .env)
```

**Profil Spring :** `prod,docker` (désactive outils dev, chemins conteneur `/app/uploads`, `/app/gest-pov-data`).

---

## 3bis. Premier administrateur (bootstrap sécurisé)

En **profil `prod`** (déploiement client), les comptes seed connus (`admin@erp.local`, etc.) **ne sont plus créés**.

| Situation | Comportement |
|-----------|--------------|
| Base vide, variables bootstrap renseignées | Création d'un compte **SUPER_ADMIN** (email/mot de passe depuis `.env`) |
| Un admin (SUPER_ADMIN ou ADMIN) existe déjà | Aucune création — redémarrages idempotents |
| Base vide, variables absentes | **Échec au démarrage** avec message explicite |

Variables obligatoires au **premier** démarrage client :

| Variable | Description |
|----------|-------------|
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Email du premier admin (ex. `admin@votre-entreprise.example`) |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Mot de passe fort (min. 12 caractères) |
| `APP_BOOTSTRAP_ADMIN_NAME` | Prénom affiché (optionnel, défaut `Admin`) |

```bash
# Exemple génération mot de passe
openssl rand -base64 24
```

**Après première connexion :** conserver le mot de passe dans un gestionnaire de secrets ou le modifier via l'écran Utilisateurs (pas de « changement forcé » automatique pour l'instant).

**Licence :** voir [license-production.md](license-production.md) — installation ID, import `.lic`, gate API.

**Migration depuis une install existante** avec `admin@erp.local` : l'ancien compte reste en base tant que le volume PostgreSQL n'est pas réinitialisé — créer un nouvel admin via l'UI puis désactiver l'ancien.

---

## 4. Installation (images pré-buildées)

```bash
docker load -i images/monapp-backend-1.0.0.tar
docker load -i images/monapp-frontend-1.0.0.tar

cp .env.example .env
# BACKEND_IMAGE, FRONTEND_IMAGE, secrets...

cd deploy
docker compose -f compose.client.images.yml --env-file ../.env up -d
```

---

## 5. Variables `.env` essentielles

| Variable | Description |
|----------|-------------|
| `POSTGRES_DB` / `POSTGRES_USER` / `POSTGRES_PASSWORD` | Base PostgreSQL |
| `APP_JWT_SECRET` | JWT (≥ 32 caractères aléatoires) |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Premier admin (obligatoire si base vide) |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Mot de passe bootstrap (min. 12 car.) |
| `APP_BOOTSTRAP_ADMIN_NAME` | Prénom admin (optionnel) |
| `APP_BIND` | Interface d'écoute proxy (`127.0.0.1` recommandé) |
| `APP_PORT` | Port HTTP (`80`) |
| `SPRING_PROFILES_ACTIVE` | `prod,docker` (défaut dans `.env.example`) |

Ne **jamais** committer `.env`.

---

## 6. Scripts client

| Script | Action |
|--------|--------|
| `scripts/client-start.sh` | Démarrer (build + up) |
| `scripts/client-stop.sh` | Arrêter (volumes conservés) |
| `scripts/client-logs.sh` | Logs (`-f` suivi) |
| `scripts/client-status.sh` | `docker compose ps` |
| `scripts/client-backup.sh` | Backup PG + licence + uploads |

---

## 7. Volumes persistants

| Volume Docker | Contenu |
|---------------|---------|
| `gest_pov_postgres_data` | Base ERP |
| `gest_pov_uploads` | Images produits |
| `gest_pov_license` | Licence + installation ID |
| `gest_pov_caddy_data` / `gest_pov_caddy_config` | Certificats Caddy (si HTTPS local) |

---

## 8. Ports exposés (client)

| Service | Exposition hôte |
|---------|-----------------|
| **proxy (Caddy)** | `${APP_BIND}:${APP_PORT}` → 80 (seul point d'entrée) |
| backend | **Non exposé** (réseau Docker interne) |
| frontend | **Non exposé** (via Caddy + nginx interne) |
| postgres | **Non exposé** (compose client build) ; `127.0.0.1:5432` optionnel (images) |

---

## 9. Licence

1. Démarrer la stack.
2. Ouvrir l'UI → écran licence, ou `GET /api/license/installation-id`.
3. Importer le fichier `.lic` fourni.
4. Vérifier : `GET /api/license/status` → `"valid": true`.

---

## 10. Backup

```bash
./scripts/client-backup.sh
```

Crée `backups/YYYYMMDD-HHMMSS/` :

- `postgres.dump` (format custom `pg_dump -Fc`)
- `gest-pov-license.tar.gz`
- `gest-pov-uploads.tar.gz`

### Restore PostgreSQL (procédure manuelle — tester d'abord hors prod)

```bash
# Arrêter le backend
docker compose -f docker-compose.client.yml --env-file .env stop backend

# Restore (remplace les données)
docker exec -i gest-pov-postgres pg_restore -U erp_user -d erp_products --clean --if-exists \
  < backups/YYYYMMDD-HHMMSS/postgres.dump

docker compose -f docker-compose.client.yml --env-file .env start backend
```

Volumes licence/uploads : extraire les `.tar.gz` dans des volumes vides **uniquement** si vous maîtrisez Docker volumes (voir doc admin).

---

## 11. Tunnel Cloudflare (optionnel — non recommandé en prod)

```bash
docker compose -f docker-compose.client.yml -f docker-compose.tunnel.yml --env-file .env up -d
docker compose -f docker-compose.client.yml -f docker-compose.tunnel.yml logs cloudflared
```

**Désactivé par défaut** dans le compose client.

---

## 12. Limites restantes

- Pas de HTTPS automatique (Caddy `:80` HTTP) — reverse proxy client ou certificat à ajouter.
- CORS inclut réseaux privés + trycloudflare (code applicatif inchangé).
- Comptes seed admin connus au 1er démarrage → **uniquement dev/docker local** ; client prod utilise bootstrap `.env`.
- Pas de script restore automatisé (risque de perte de données).
- `/api/health` et `/actuator/*` via proxy : **403** tant que licence absente (comportement attendu) ; liveness interne backend OK.
- Scripts `.sh` : sous Windows, utiliser **Git Bash** (`C:\Program Files\Git\bin\bash.exe`).

---

## 13. Smoke test runtime (2026-07-02)

**Environnement :** Windows 10, Docker Desktop, Git Bash pour les scripts.

### Commandes exécutées

```powershell
cp .env.example .env          # secrets générés (non commités)
docker compose -f docker-compose.client.yml --env-file .env up --build -d
docker compose -f docker-compose.client.yml --env-file .env ps
docker compose -f docker-compose.client.yml --env-file .env logs backend --tail 40
docker compose -f docker-compose.client.yml --env-file .env restart
bash scripts/client-backup.sh   # Git Bash
docker compose -f docker-compose.client.yml --env-file .env down
```

**Durée approximative :** build + démarrage ~2 min 20 s ; vérifications + restart + backup ~3 min ; **total ~6 min**.

### Résultats

| Vérification | Résultat |
|--------------|----------|
| PostgreSQL healthcheck | **healthy** |
| Backend healthcheck | **healthy** (liveness interne `UP`) |
| Frontend / proxy | **up** — proxy `127.0.0.1:80` |
| Backend port hôte 8080 | **non exposé** (8080/tcp interne uniquement) |
| Flyway | 20 migrations appliquées |
| Frontend `/` | HTTP 200 (SPA) |
| `/api/settings/public` | HTTP 200 |
| `/api/license/status` | HTTP 200 — `valid:false`, `reason:LICENSE_MISSING`, `installationId` généré |
| `/api/auth/login` (smoke v1) | HTTP 200 + token avec ancien seed — **obsolète** (voir §15 bootstrap) |
| `/api/health` via proxy | HTTP 403 `LICENSE_REQUIRED` (attendu sans `.lic`) |
| Persistance volumes | Fichiers smoke-test conservés après restart (smoke v1, volume PG avec seed) |

> Smoke test v1 (2026-07-02 matin) — avant sprint bootstrap. Voir **§15** pour validation bootstrap sur DB vierge.

---

## 15. Validation bootstrap admin Docker (2026-07-02)

**Objectif :** confirmer qu'en profil `prod,docker` aucun compte seed connu n'est créé et que l'admin provient de `.env`.

### Préparation

- `.env` complété avec `APP_BOOTSTRAP_ADMIN_*`, `APP_JWT_SECRET`, `POSTGRES_PASSWORD` (non commité).
- Volume PostgreSQL smoke test supprimé : `gest-pov-client_gest_pov_postgres_data` (données jetables du smoke précédent).
- Volumes **conservés** : licence, uploads, caddy.

### Commandes exécutées

```powershell
docker compose -f docker-compose.client.yml --env-file .env down
docker volume rm gest-pov-client_gest_pov_postgres_data   # smoke test uniquement
docker compose -f docker-compose.client.yml --env-file .env up --build -d
docker compose -f docker-compose.client.yml --env-file .env logs backend
docker exec gest-pov-postgres psql -U erp_user -d erp_products -c "SELECT email FROM users;"
docker compose -f docker-compose.client.yml --env-file .env restart
bash scripts/client-backup.sh
docker compose -f docker-compose.client.yml --env-file .env down
```

**Durée :** build + up ~2 min 30 ; validations + restart + backup ~4 min.

### Résultats

| Vérification | Résultat |
|--------------|----------|
| PostgreSQL / backend / proxy | **healthy** / **up** |
| Log bootstrap | `Bootstrap admin created for email bootstrap-admin@client.test` — **sans mot de passe** |
| Users en base | **1 seul** : email bootstrap (pas `admin@erp.local`, pas `caissier@erp.local`) |
| Rôle | **SUPER_ADMIN** (requête SQL `user_roles`) |
| Login anciens comptes seed | **HTTP 400** — comptes absents |
| Login admin bootstrap | **HTTP 200** + token JWT |
| `/api/license/status` | `valid:false`, `LICENSE_MISSING` |
| `/api/products` (avec token) | **HTTP 403** `LICENSE_REQUIRED` — gate licence OK |
| Restart | 1 user, login bootstrap OK, **pas de second log** bootstrap |
| Backup | `backups/20260702-071136/` — `postgres.dump` ~236 Ko + licence/uploads |
| Arrêt | `down` sans `-v` — 5 volumes conservés |

### Risques restants

- Volume PG du smoke bootstrap contient encore l'admin de test — réinitialiser le volume PG avant livraison client réelle.
- `/api/auth/me` bloqué par licence (403) même avec token valide — login suffit pour valider l'auth.
- Pas de changement de mot de passe forcé au 1er login.

---

## 16. Validation licence production (2026-07-02)

Voir détail : [license-production.md](license-production.md).

| Vérification | Résultat |
|--------------|----------|
| JAR client : clé publique seule | OK (`public_key.pem`, pas de clé privée) |
| Volume `gest_pov_license` → `/app/gest-pov-data` | OK |
| `installation.id` persistant au restart | OK |
| Sans `.lic` : API métier | HTTP 403 |
| Import format invalide | HTTP 400 |
| Activation licence prod réelle | **Non testée** (clé privée éditeur hors environnement) |
| Tests `mvn -Dtest=*License* test` | **14 tests OK** |

### Parcours sans licence validé (activation en attente éditeur)

- `GET /api/license/installation-id` : UUID unique (récupérer sur install vierge, ne pas réutiliser un ID smoke dev)
- `GET /api/license/status` : `valid=false`, `reason=LICENSE_MISSING`
- Login admin bootstrap : **HTTP 200**
- `GET /api/products` avec token admin : **HTTP 403** `LICENSE_REQUIRED`
- `installation.id` stable après `docker compose restart backend`
- Backup licence OK : `backups/<timestamp>/gest-pov-license.tar.gz`

Checklist éditeur prête : `docs/license-activation-checklist.md`

---

## 14. Vérification config (sans démarrer)

```bash
docker compose -f docker-compose.client.yml --env-file .env.example config
```

---

*Voir aussi : [release-audit-v1.md](release-audit-v1.md), [deploy/README.md](../deploy/README.md)*
