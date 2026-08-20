# Audit du projet Gest POV existant

**Date :** 2026-08-19  
**Objectif :** identifier ce qui est réutilisable tel quel pour l'édition Desktop Windows offline, sans modifier le Web/Docker.

---

## 1. Stack actuelle

| Couche | Technologie | Emplacement |
|--------|-------------|-------------|
| Backend | Spring Boot 3.2.5, Java 17 | `backend/` |
| Frontend Web | React 19, Vite 8, Tailwind 4 | `frontend/` |
| Base | PostgreSQL 16, Flyway (20 migrations) | `backend/src/main/resources/db/migration/` |
| Auth | JWT stateless, BCrypt | `backend/.../security/` |
| Licence | Fichier signé RSA, offline | `backend/.../license/` |
| Docker | Compose + Caddy | `docker-compose*.yml`, `Caddyfile` |
| E2E | Playwright | `play/` |

---

## 2. Backend — réutilisable tel quel

### Source de vérité métier

Toute la logique métier (POS, stock, produits, ventes, retours, licence) est dans :

- **34 controllers** REST sous `/api/*`
- **~79 services**
- **54 repositories JPA**

Le Desktop **doit consommer ces API** — pas réimplémenter la logique.

### Endpoints critiques Desktop

| Domaine | Base path | Usage Desktop |
|---------|-----------|---------------|
| Auth | `POST /api/auth/login`, `GET /api/auth/me` | Connexion utilisateur |
| Licence | `GET /api/license/status`, `POST /api/license/import` | Activation offline |
| POS | `/api/pos/*` | Caisse (priorité migration) |
| Settings | `GET /api/settings/public` | Config publique client |
| Health | `/actuator/health/liveness`, `/readiness` | Diagnostic serveur |
| Produits/Stock | `/api/products`, `/api/stock/*` | Back-office |

### Profils Spring

| Profil | Fichier | Usage |
|--------|---------|--------|
| `dev` | `application-dev.yml` | Dev local, licence off, Swagger |
| `prod` | `application-prod.yml` | Client, bootstrap admin via env |
| `docker` | `application-docker.yml` | Chemins `/app/uploads`, `/app/gest-pov-data` |
| `demo` | `application-demo.yml` | Jeu démo auto |

**Profil Desktop serveur proposé :** `prod` + profil dédié `desktop` (à créer) — voir `architecture.md`.

### Variables d'environnement backend

| Variable | Rôle |
|----------|------|
| `SPRING_DATASOURCE_URL` | JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | Utilisateur DB |
| `SPRING_DATASOURCE_PASSWORD` | Mot de passe DB |
| `APP_JWT_SECRET` | Clé JWT (persistante entre redémarrages) |
| `APP_BOOTSTRAP_ADMIN_*` | Premier admin (installation vierge) |

Properties YAML (non env) :

- `app.upload.dir` — fichiers uploadés
- `app.license.data-dir` — `installation.id` + `gest_pov.lic`
- `app.license.enforcement-enabled` — gate licence

### Sécurité

- JWT Bearer, expiration 24h
- Routes publiques : login, licence, settings/public, uploads, actuator liveness/readiness
- **Pas de discovery LAN aujourd'hui** — à ajouter (voir `backend-additions-required.md`)

### Licence (100 % offline compatible)

- Fichier `gest_pov.lic` signé RSA
- Clé publique embarquée : `backend/src/main/resources/keys/public_key.pem`
- `installation.id` généré localement, stable par machine
- Transfert possible par clé USB
- **Réutilisable tel quel** pour Desktop serveur

### Sauvegardes existantes

- Script Bash : `scripts/client-backup.sh` (Docker)
- Contenu : dump PostgreSQL + licence + uploads
- **À adapter** pour Windows natif dans `desktop/scripts/` — même logique, autre exécution

### Dépendances Internet backend

**Aucune** à l'exécution. Pas de `WebClient` / API externe.

---

## 3. Frontend Web — référence, pas remplacement

### Configuration API

```javascript
// frontend/src/api/client.js
baseURL: import.meta.env.VITE_API_BASE ?? '/api'
```

Dev : proxy Vite → `http://127.0.0.1:8080`  
Docker : nginx/Caddy same-origin `/api`

### Dépendance Internet identifiée

| Ressource | Fichier | Impact offline Web |
|-----------|---------|-------------------|
| Google Fonts (Inter) | `frontend/index.html` | Nécessite Internet au 1er chargement |

**Desktop JavaFX :** pas concerné (polices système ou embarquées).

### Routes fonctionnelles (référence migration)

Voir `migration-status.md` pour la matrice complète.

Priorité Desktop : **POS** (`/pos`, `/pos/pending`, retours, historique).

---

## 4. Docker — inchangé

| Fichier | Rôle |
|---------|------|
| `docker-compose.yml` | Dev/démo build local |
| `docker-compose.client.yml` | Production client |
| `docker-compose.hub.yml` | Images Docker Hub |
| `scripts/client-*.sh` | Start/stop/backup |

Le Desktop est un **deuxième canal de distribution** — Docker reste officiel.

---

## 5. Ce qui doit être développé (Desktop)

| Composant | Statut actuel | Action |
|-----------|---------------|--------|
| Application JavaFX | Absent | Créer `desktop/client/` |
| Découverte LAN UDP | Absent | `desktop/client/` + endpoint backend |
| Endpoint `/api/discovery` | Absent | Ajout backend minimal |
| `serverId` persistant | Absent | Installateur serveur |
| PostgreSQL service Windows | Absent | `desktop/server-package/` |
| Spring Boot service Windows | Absent | `desktop/server-package/` |
| Installateurs `.exe` | Absent | `desktop/installer/` |
| Secrets DB protégés Windows | Absent | DPAPI ou équivalent |
| Backup Windows natif | Absent | Adapter logique existante |
| Tests Desktop | Absent | `desktop/tests/` |

---

## 6. Ce qu'il ne faut PAS faire

- Dupliquer les services métier Spring Boot dans le client Desktop
- Remplacer le frontend React
- Refactoriser le monorepo en multi-module Maven global
- Exposer PostgreSQL au LAN pour les postes clients
- Stocker `db.username` / `db.password` côté Desktop

---

## 7. Risques identifiés

| Risque | Mitigation |
|--------|------------|
| Modification backend casse Web/Docker | Ajouts isolés, tests existants conservés |
| Google Fonts offline Web | Documenté ; hors scope Desktop JavaFX |
| Version client ≠ serveur | Endpoint discovery + matrice compatibilité |
| Secrets DB en clair | Génération install + protection Windows |
| Tests E2E Playwright = profil dev | Tests Desktop séparés |

---

## 8. Conclusion

**Réutilisation maximale :** backend Spring Boot complet, Flyway, JWT, licence RSA, API POS.  
**Nouveau développement :** shell Desktop JavaFX, packaging Windows, discovery LAN, services Windows, installateurs.  
**Isolation :** tout sous `desktop/` + ajouts backend documentés et minimaux.
