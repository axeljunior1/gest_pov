# Prochaines étapes release — Gest_POV

**Après vérification post-commit** — passage à l'installation client vierge et activation licence réelle.

Références : `docs/release-readiness-v1.md`, `docs/license-activation-checklist.md`, `docs/client-docker-deployment.md`.

---

## État actuel (2026-07-02)

| Étape | Statut |
|-------|--------|
| Reset volumes smoke | **Fait** |
| `.env` client réel | **Fait** — secrets locaux (non commités) |
| Stack client | **Up** — postgres/backend/frontend healthy, proxy `127.0.0.1:80` |
| Bootstrap admin | **OK** |
| Comptes seed | **Absents** |
| Import licence (`gest_pov.lic`) | **Fait** — `valid=true`, `activated=true` |
| API métier débloquée | **OK** — `/api/products` et `/api/auth/me` → HTTP 200 |
| Restart validation | **OK** — licence persistante, `<INSTALLATION_ID_CLIENT>` inchangé |
| Backup post-activation + post-restart | **Fait** — `backups/<BACKUP_DIR>/` (postgres + `installation.id` + `gest_pov.lic`) |
| Parcours métier POS (catalogue → stock → vente → retour → clôture) | **Validé** (exemple local non client) |
| Backup post-parcours POS | **Fait** — `backups/<BACKUP_DIR>/` |
| Export images `.tar` | **GO** (après commit docs) |

> **Rappel :** ne jamais committer `.env`, `gest_pov.lic`, `backups/` ni `images/*.tar`.

**Prochaine action :** commit des docs de validation puis export images `.tar` et préparation du package livraison client (étape 9 ci-dessous).

---

## Prérequis

- Dépôt Git propre (commits Docker, bootstrap, licence et docs intégrés).
- Docker Desktop disponible sur le poste de préparation.
- Accès à l'outil éditeur pour générer `gest_pov.lic` (clé privée production, hors repo).
- **Ne pas** réutiliser volumes ou backups issus des smoke tests dev.

---

## Étape 1 — Réinitialiser les volumes smoke

Sur le poste de **préparation uniquement** (pas en production client existante) :

```powershell
docker compose -f docker-compose.client.yml --env-file .env down
```

Volumes à supprimer (données smoke test) :

```powershell
docker volume rm gest-pov-client_gest_pov_postgres_data
docker volume rm gest-pov-client_gest_pov_license
```

Optionnel (reprise à zéro complète) :

```powershell
docker volume rm gest-pov-client_gest_pov_uploads
docker volume rm gest-pov-client_gest_pov_caddy_config gest-pov-client_gest_pov_caddy_data
```

Supprimer les backups smoke locaux (gitignored) :

```powershell
Remove-Item -Recurse -Force backups\*
```

---

## Étape 2 — Créer le `.env` client réel

```powershell
cp .env.example .env
```

Éditer `.env` avec des valeurs **fortes et uniques** :

| Variable | Action |
|----------|--------|
| `POSTGRES_PASSWORD` | Générer : `openssl rand -base64 24` |
| `SPRING_DATASOURCE_PASSWORD` | Identique à `POSTGRES_PASSWORD` |
| `APP_JWT_SECRET` | Générer : `openssl rand -base64 32` |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Email admin client réel |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Mot de passe fort (min. 12 caractères) |
| `APP_BOOTSTRAP_ADMIN_NAME` / `LAST_NAME` | Identité admin |

> `.env` ne doit **jamais** être commité.

---

## Étape 3 — Démarrer la stack vierge

```powershell
docker compose -f docker-compose.client.yml --env-file .env up --build -d
docker compose -f docker-compose.client.yml --env-file .env ps
```

Attendre : PostgreSQL **healthy**, backend **healthy**, proxy **up**.

Vérifications initiales :

```powershell
curl -s http://127.0.0.1/api/license/status
curl -s http://127.0.0.1/api/auth/login -H "Content-Type: application/json" -d "{\"email\":\"<APP_BOOTSTRAP_ADMIN_EMAIL>\",\"password\":\"<APP_BOOTSTRAP_ADMIN_PASSWORD>\"}"
```

Attendu sans licence : `LICENSE_MISSING`, login admin **200**, `/api/products` **403**.

---

## Étape 4 — Récupérer le nouvel `installationId`

```powershell
curl -s http://127.0.0.1/api/license/installation-id
```

Ou via le volume :

```powershell
docker exec gest-pov-backend cat /app/gest-pov-data/installation.id
```

Conserver cet UUID — il est **unique à cette installation** et requis pour la licence.

---

## Étape 5 — Transmettre à l'éditeur

Remplir la demande selon `docs/license-activation-checklist.md` :

- Installation ID (étape 4)
- Produit : `gest_pov`
- Nom / site client, plan, expiration, `maxUsers`

L'éditeur génère `gest_pov.lic` signé avec la clé privée **production** et le transmet par canal sécurisé (pas Git).

---

## Étape 6 — Importer `gest_pov.lic`

```powershell
curl -X POST http://127.0.0.1/api/license/import -F "file=@gest_pov.lic"
curl -s http://127.0.0.1/api/license/status
```

Attendu : `valid=true`, `activated=true`.

---

## Étape 7 — Valider le déblocage API

```powershell
# Token admin (depuis réponse login étape 3)
curl -s -H "Authorization: Bearer <TOKEN>" http://127.0.0.1/api/products
```

Attendu : **HTTP 200** (plus de `403 LICENSE_REQUIRED`).

Après restart :

```powershell
docker compose -f docker-compose.client.yml --env-file .env restart
curl -s http://127.0.0.1/api/license/status
```

Attendu : `valid=true` conservé.

---

## Étape 8 — Backup post-activation

```bash
bash scripts/client-backup.sh
```

Vérifier le dossier `backups/<timestamp>/` :

- `postgres.dump`
- `gest-pov-license.tar.gz` (contient `installation.id` + `gest_pov.lic`)
- `gest-pov-uploads.tar.gz`
- `README.txt`

---

## Étape 9 — Rebuild et export images `.tar`

Si livraison offline au client :

```powershell
docker compose -f docker-compose.client.yml --env-file .env build
docker save -o images/gest-pov-backend-1.0.0.tar gest-pov-backend:1.0.0
docker save -o images/gest-pov-frontend-1.0.0.tar gest-pov-frontend:1.0.0
```

Valider le déploiement images :

```powershell
docker compose -f deploy/compose.client.images.yml --env-file .env.example config
```

> Les archives `images/*.tar` sont gitignored — les inclure uniquement dans le package de livraison.

---

## Étape 10 — Livraison client

Package à transmettre :

- Images `.tar` (ou accès registry)
- `.env.example` (pas le `.env` réel)
- `docker-compose.client.yml`, `docker-compose.tunnel.yml` (si tunnel requis)
- `deploy/compose.client.images.yml`, `Caddyfile`
- Scripts `scripts/client-*.sh`
- Documentation : `docs/client-docker-deployment.md`, guide utilisateur

**Ne pas livrer :** `.env`, backups, `gest_pov.lic` d'une autre machine, clés privées, générateur éditeur.

---

## Checklist rapide

- [ ] Volumes smoke supprimés
- [ ] `.env` réel créé (secrets forts)
- [ ] Stack vierge démarrée
- [ ] `installationId` récupéré et transmis à l'éditeur
- [ ] `gest_pov.lic` importé
- [ ] `/api/products` → 200
- [ ] Licence valide après restart
- [ ] Backup post-activation exécuté
- [ ] Images `.tar` exportées (si livraison offline)
- [ ] Package client préparé sans secrets
