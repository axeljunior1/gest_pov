# Release readiness v1 — Gest_POV

**Date :** 2026-07-02  
**Branche :** `main`  
**Contexte :** préparation licence validée **sans** fichier `.lic` réel ; l’activation finale dépend de l’éditeur externe.

---

## 1. État actuel

| Domaine | Statut | Détail |
|---------|--------|--------|
| Tests backend | **Validé** | 214+ tests ; optimisations `@DirtiesContext` → `TestDatabaseCleaner` (commit `0d7d71b`) |
| Stack Docker client | **Validé** | `docker-compose.client.yml`, scripts `client-*`, smoke runtime OK (commit `31f19c1`) |
| Bootstrap admin prod | **Validé** | Seed désactivé en `prod`, admin via `.env` (commit `4c47661`) |
| Licence (sans `.lic` réelle) | **Validé** | `LICENSE_MISSING`, gate API 403, persistance `installation.id`, backup licence |
| Licence (activation prod) | **Bloqué** | Nécessite `gest_pov.lic` signé par l’éditeur (clé privée hors repo) |
| Compose client | **Validé** | `docker compose … config` OK sur les 3 variantes (voir §6) |
| Secrets dans Git | **OK** | `.env`, `*.lic`, backups ignorés ; seule `test_private_key.pem` (CI) versionnée |
| Working tree | **À committer** | Tests licence, `.gitignore`, docs licence/release (voir §7) |

### Commits déjà intégrés sur `main`

| Commit | Contenu |
|--------|---------|
| `0d7d71b` | Optimisation tests backend (`TestDatabaseCleaner`, reseed) |
| `2565e84` | Docs tests + audit release initial |
| `31f19c1` | Docker client (compose, scripts, backup, tunnel opt-in) |
| `4c47661` | Bootstrap admin sécurisé + tests |

### Artefacts smoke test locaux (hors Git)

| Artefact | Emplacement | Action avant livraison client |
|----------|-------------|-------------------------------|
| Backups smoke | `backups/20260702-*` (6 dossiers) | Supprimer localement ou archiver hors livraison |
| `smoke-test.txt` | Volume `gest-pov-client_gest_pov_license` | Supprimer fichier ou réinitialiser volume licence |
| Admin bootstrap test | Volume `gest-pov-client_gest_pov_postgres_data` | **Réinitialiser** (DB vierge client) |
| `.env` local | Racine projet | **Ne pas committer** — recréer côté client |
| Uploads / Caddy smoke | Volumes `gest_pov_uploads`, `gest_pov_caddy_*` | Réinitialiser si livraison depuis poste dev |

---

## 2. Ce qui est validé

- **Docker client** : build, healthchecks, Flyway 20 migrations, proxy Caddy, port 8080 non exposé.
- **Bootstrap** : un seul admin depuis `APP_BOOTSTRAP_ADMIN_*` ; comptes seed (`admin@erp.local`, etc.) absents en `prod`.
- **Licence sans fichier** :
  - `GET /api/license/status` → `valid:false`, `reason:LICENSE_MISSING`
  - `GET /api/products` (authentifié) → `403 LICENSE_REQUIRED`
  - `installation.id` persistant au restart
  - Import licence testé en intégration Maven (clé test, pas prod)
- **Backup** : `scripts/client-backup.sh` — dump PostgreSQL + archives volumes licence/uploads.
- **Sécurité repo** : clé privée prod absente ; `gest-pov-licgen/`, `licen/`, `*.lic` dans `.gitignore`.

Références détaillées :

- `docs/client-docker-deployment.md` (§13–16)
- `docs/license-production.md`
- `docs/license-activation-checklist.md`
- `docs/release-audit-v1.md`

---

## 3. Bloqué par licence réelle

| Étape | Bloquant |
|-------|----------|
| Génération `gest_pov.lic` | Outil éditeur + clé privée production (hors dépôt) |
| Import licence en stack Docker prod | Fichier `.lic` non disponible localement |
| Déblocage `/api/products` en runtime Docker | Non validé avec signature prod |
| Validation post-activation complète (restart + backup avec `.lic`) | En attente fichier éditeur |
| Export images `.tar` pour livraison | Possible techniquement ; à faire **après** validation licence |

**Installation ID smoke test** (volume local actuel, à ne pas réutiliser en prod) : récupérer un nouvel ID après réinitialisation des volumes client.

---

## 4. Risques restants

| Risque | Sévérité | Mitigation |
|--------|----------|------------|
| Volumes Docker contiennent données smoke | **Élevé** | Réinitialiser PG + licence avant livraison (checklist §8) |
| `test_private_key.pem` dans le repo | **Moyen** | Documenté ; absente du JAR prod ; ≠ paire prod |
| JWT / mots de passe par défaut en dev | **Moyen** | Client doit remplir `.env` réel (`APP_JWT_SECRET`, `POSTGRES_PASSWORD`, bootstrap) |
| `/api/auth/me` bloqué sans licence | **Faible** | Comportement attendu ; login suffit pour valider auth |
| Pas de changement MDP forcé au 1er login | **Faible** | Documenté hors scope v1 |
| HTTPS non automatique (Caddy local HTTP) | **Moyen** | Documenter TLS côté client si exposition Internet |
| Encodage UTF-8 docs Windows | **Faible** | Vérifier rendu des caractères accentués |

---

## 5. Commandes de validation rapide

### Git / secrets (sans afficher de valeurs)

```powershell
git status --short
git check-ignore -v .env backups/
git ls-files | Select-String '\.env$|\.lic$|private_key'
```

### Docker compose (léger, sans `up`)

```powershell
docker compose -f docker-compose.client.yml --env-file .env.example config
docker compose -f docker-compose.client.yml -f docker-compose.tunnel.yml --env-file .env.example config
docker compose -f deploy/compose.client.images.yml --env-file .env.example config
```

### Stack client (smoke manuel, hors sprint cleanup)

```powershell
cp .env.example .env   # puis éditer secrets réels
docker compose -f docker-compose.client.yml --env-file .env up -d
curl -s http://127.0.0.1/api/license/status
curl -s http://127.0.0.1/api/license/installation-id
```

### Tests backend ciblés (si modification code licence)

```powershell
cd backend
mvn -Dtest=LicenseServiceTest,LicenseImportIntegrationTest test
```

> Ne pas lancer `mvn test` complet ni `mvn clean verify` dans le cadre du nettoyage release.

---

## 6. Ordre recommandé avant livraison client

1. **Committer** les changements restants (plan §7).
2. **Nettoyer** artefacts smoke locaux (`backups/`, volumes Docker).
3. **Préparer** `.env` client réel (secrets forts, admin bootstrap).
4. **Démarrer** stack vierge : `docker compose -f docker-compose.client.yml --env-file .env up -d`.
5. **Récupérer** `installationId` via API ou volume.
6. **Demander** `gest_pov.lic` à l’éditeur (canal sécurisé).
7. **Importer** licence : `POST /api/license/import`.
8. **Valider** : `/api/license/status` → `valid:true` ; `/api/products` → `200`.
9. **Backup** : `./scripts/client-backup.sh`.
10. **Rebuild** images et archives `.tar` si livraison offline.
11. **Livrer** : images, `.env.example`, docs (`client-docker-deployment.md`, guide utilisateur).

### Réinitialisation volumes Docker (smoke test uniquement)

```powershell
docker compose -f docker-compose.client.yml --env-file .env down

# PostgreSQL — données admin smoke
docker volume rm gest-pov-client_gest_pov_postgres_data

# Licence — installation.id smoke + smoke-test.txt
docker volume rm gest-pov-client_gest_pov_license

# Optionnel — uploads / caddy si reprise à zéro
# docker volume rm gest-pov-client_gest_pov_uploads
# docker volume rm gest-pov-client_gest_pov_caddy_config gest-pov-client_gest_pov_caddy_data
```

> **Ne pas exécuter** sur un environnement client déjà en production avec données réelles.

### Suppression artefact `smoke-test.txt` (sans supprimer tout le volume)

```powershell
docker run --rm -v gest-pov-client_gest_pov_license:/data alpine rm -f /data/smoke-test.txt
```

---

## 7. Plan de commits recommandé

Les commits 2 et 3 sont **déjà sur `main`**. Proposition pour le working tree actuel :

### Commit 1 — Optimisation / tests backend licence *(déjà partiellement commité)*

Fichiers restants :

- `backend/src/test/java/com/erp/products/license/LicenseServiceTest.java`
- `backend/src/test/java/com/erp/products/license/LicenseImportIntegrationTest.java`

*Note : l’optimisation `TestDatabaseCleaner` est déjà dans `0d7d71b`.*

### Commit 2 — Docker client *(déjà commité `31f19c1`)*

Fichiers déjà versionnés : `docker-compose.client.yml`, `docker-compose.tunnel.yml`, `deploy/compose.client.images.yml`, `scripts/client-*.sh`, `.env.example`, `docs/client-docker-deployment.md` (partie initiale).

### Commit 3 — Bootstrap admin *(déjà commité `4c47661`)*

Fichiers déjà versionnés : `AdminBootstrapService`, `BootstrapAdminProperties`, `application-prod.yml`, tests bootstrap.

### Commit 4 — Licence production (docs, gitignore, nettoyage)

- `.gitignore` (renfort `*.lic`, `gest-pov-licgen/`, clés privées)
- `backend/cxdf` (suppression note locale licgen)
- `backend/src/test/java/com/erp/products/license/LicenseServiceTest.java`
- `backend/src/test/java/com/erp/products/license/LicenseImportIntegrationTest.java`
- `docs/license-production.md` *(nouveau)*
- `docs/license-activation-checklist.md` *(nouveau)*

### Commit 5 — Docs release

- `docs/release-audit-v1.md`
- `docs/client-docker-deployment.md` (sections smoke / bootstrap / licence)
- `docs/release-readiness-v1.md` *(ce document)*

**Alternative :** fusionner commits 4 et 5 en un seul « docs + licence release » si historique plus simple souhaité.

**À ne jamais committer :** `.env`, `backups/`, `*.lic`, clés privées prod, `gest-pov-licgen/`, `images/*.tar`.

---

## 8. Checklist avant livraison client

- [ ] Réinitialiser volume PostgreSQL de test (`gest-pov-client_gest_pov_postgres_data`)
- [ ] Réinitialiser volume licence (ou supprimer `smoke-test.txt` + ancien `installation.id`)
- [ ] Supprimer dossiers `backups/` smoke locaux
- [ ] Remplir `.env` client réel (secrets forts, `APP_BOOTSTRAP_ADMIN_*`)
- [ ] Générer licence via outil éditeur (hors repo)
- [ ] Importer `gest_pov.lic` (`POST /api/license/import`)
- [ ] Valider `GET /api/products` → HTTP 200 après activation
- [ ] Valider `GET /api/license/status` → `valid:true` après restart
- [ ] Exécuter `./scripts/client-backup.sh` après activation
- [ ] Rebuild et exporter images `.tar` si livraison offline
- [ ] Livrer documentation client (`docs/client-docker-deployment.md`, guide utilisateur, `.env.example`)
- [ ] Vérifier qu’aucun secret ni backup n’est inclus dans le package livré

---

## 9. Prochaine action recommandée

1. Exécuter les commits 4 et 5 (ou fusion selon préférence).
2. Réinitialiser les volumes Docker smoke sur le poste de préparation.
3. Transmettre l’`installationId` d’une **install vierge** à l’éditeur pour génération de `gest_pov.lic`.
4. Valider le cycle complet import → API débloquée → backup → export images.
