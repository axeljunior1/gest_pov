# Licence Gest_POV — production client

Guide de séparation **client** (vérification) vs **éditeur** (émission), et procédures d'activation.

---

## 1. Architecture

| Composant | Rôle |
|-----------|------|
| `LicenseService` | Lecture `installation.id`, validation `.lic`, cache statut |
| `LicenseSignatureVerifier` | Vérification RSA **SHA256withRSA** avec clé **publique** embarquée |
| `LicenseEnforcementFilter` | Bloque l'API métier si licence invalide (`403 LICENSE_REQUIRED`) |
| `LicenseController` | `/api/license/status`, `/installation-id`, `/import` |
| Volume Docker `gest_pov_license` | Persistance `installation.id` + `gest_pov.lic` |

**Profils :**

| Profil | Enforcement | Clé publique | Données |
|--------|-------------|--------------|---------|
| `prod,docker` (client) | **ON** | `classpath:keys/public_key.pem` | `/app/gest-pov-data` |
| `dev` | OFF (configurable) | idem | `./gest-pov-data` |
| `test` | OFF par défaut | `test_public_key.pem` en tests ciblés | `target/test-*` |

---

## 2. Livré au client

- Image backend avec **clé publique uniquement** (`BOOT-INF/classes/keys/public_key.pem`)
- Volume `gest_pov_license` monté sur `/app/gest-pov-data`
- Endpoints bootstrap licence (sans JWT requis)
- Gate licence **actif** en production

**Jamais livré au client :**

- Clé privée RSA de production
- Outil `gest-pov-licgen` / `licen/` (répertoires hors repo ou `.gitignore`)
- Fichiers `.lic` réels (générés par l'éditeur par installation)

---

## 3. Côté éditeur (hors package client)

1. Conserver la **clé privée** dans un coffre / poste éditeur (`gest-pov-licgen/keys/private_key.pem` — **gitignored**).
2. Générer une licence pour l'`installationId` fourni par le client.
3. Transmettre le fichier `gest_pov.lic` par canal sécurisé (pas par Git).

> Le dépôt ne contient pas actuellement le générateur `gest-pov-licgen/` — à maintenir hors repo ou dans un dépôt éditeur privé.

**Paire de clés de test CI** (uniquement tests Maven) :

- `backend/src/test/resources/keys/test_private_key.pem` — **dans Git pour CI**, absente du JAR prod
- `backend/src/test/resources/keys/test_public_key.pem` — utilisée par `LicenseServiceTest` / intégration

**≠ clé publique production** (empreintes SHA256 différentes).

---

## 4. Récupérer l'installation ID

```bash
# API (sans licence requise)
curl -s http://127.0.0.1/api/license/installation-id

# Ou fichier dans le volume
docker exec gest-pov-backend cat /app/gest-pov-data/installation.id
```

Transmettre cet UUID à l'éditeur pour génération de la licence.

---

## 5. Importer une licence

### UI

Écran licence au premier démarrage → import du fichier `.lic`.

### API

```bash
curl -X POST http://127.0.0.1/api/license/import \
  -F "file=@gest_pov.lic"
```

Réponse attendue : `"valid": true`, `"activated": true`.

### Vérifier l'état

```bash
curl -s http://127.0.0.1/api/license/status
```

---

## 6. Comportements

### Sans licence (attendu)

| Élément | Comportement |
|---------|--------------|
| UI / login | Accessibles |
| `/api/license/*` | OK |
| `/api/settings/public` | OK |
| `/api/auth/login` | OK |
| API métier (`/api/products`, etc.) | **403** `LICENSE_REQUIRED` |

### Après licence valide

| Élément | Comportement |
|---------|--------------|
| `/api/license/status` | `valid: true` |
| API métier (avec JWT) | **200** (selon permissions) |
| Redémarrage Docker | Licence **persistée** dans le volume |

---

## 7. Cas d'erreur

| Raison | Cause | Message import |
|--------|-------|----------------|
| `LICENSE_MISSING` | Pas de fichier | — |
| `INVALID_FORMAT` | JSON invalide / enveloppe incomplète | Format invalide |
| `INVALID_SIGNATURE` | Signature RSA incorrecte | Signature invalide |
| `INSTALLATION_MISMATCH` | `installationId` licence ≠ machine | Installation différente |
| `EXPIRED` | Date dépassée | Licence expirée |
| `INVALID_APP` | Champ `app` ≠ `gest_pov` | Application incorrecte |

Ne jamais désactiver la vérification de signature en production.

---

## 8. Fichiers à sauvegarder

Inclus dans `scripts/client-backup.sh` :

- `postgres.dump`
- Volume licence (`installation.id` + `gest_pov.lic` si présent)

**Critique :** sans `installation.id`, une nouvelle licence devra être réémise.

---

## 9. Fichiers à ne jamais livrer / committer

| Fichier | Protection |
|---------|------------|
| `*.lic` (client réel) | `.gitignore` |
| `gest-pov-data/` | `.gitignore` |
| `**/private_key.pem` | `.gitignore` (sauf `test_private_key.pem` CI) |
| `gest-pov-licgen/`, `licen/` | `.gitignore` |
| `.env` | `.gitignore` |

---

## 10. Validation Docker (2026-07-02)

| Test | Résultat |
|------|----------|
| JAR prod : `public_key.pem` seulement | OK |
| JAR prod : pas de `test_private_key` | OK |
| Volume `/app/gest-pov-data/installation.id` | OK, persiste au restart |
| Import JSON invalide | HTTP 400 |
| `/api/products` sans licence | HTTP 403 |
| Activation licence prod réelle | **Non testée** — clé privée éditeur non disponible localement |

**Checklist activation client :**

1. Démarrer stack, noter `installationId`
2. Éditeur génère `gest_pov.lic` signée avec clé **production**
3. Importer via UI ou `POST /api/license/import`
4. Vérifier `GET /api/license/status` → `valid: true`
5. Login admin, `GET /api/products` → 200
6. `docker compose restart` → licence toujours valide
7. `scripts/client-backup.sh`

---

## 11. Validation parcours activation (sans licence disponible) — 2026-07-02

Validation effectuée **sans générateur local** et sans fichier `.lic` réel :

| Vérification | Résultat |
|--------------|----------|
| Stack Docker client (`up -d`) | OK |
| Login admin bootstrap | HTTP 200 |
| `GET /api/license/status` | `valid=false`, `reason=LICENSE_MISSING` |
| `GET /api/license/installation-id` | `c4e0d425-0d7e-41b6-85fe-963ffb1d0010` |
| `GET /api/products` avec token admin | HTTP 403 `LICENSE_REQUIRED` |
| `installation.id` après restart backend | inchangé |
| Backup volume licence | OK (`gest-pov-license.tar.gz`) |

Activation finale reste dépendante de l'éditeur : voir `docs/license-activation-checklist.md`.

---

*Voir aussi : [client-docker-deployment.md](client-docker-deployment.md)*
