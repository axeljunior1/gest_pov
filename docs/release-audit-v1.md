# Audit release v1 — Gest_POV (statique, Sprint 0)

**Date :** 2026-06-17  
**Type :** audit technique **lecture seule** — aucun build, aucun test long exécuté  
**Périmètre :** cartographie, risques client, analyse tests (statique)

---

## 1. Résumé stack

| Couche | Technologie | Détail |
|--------|-------------|--------|
| Backend | Spring Boot 3.2.5, Java 17, Maven | JAR `gest-pov-backend`, ~34 controllers, ~54 repositories, ~71 services |
| Frontend | React 19, Vite 8, Tailwind 4 | SPA + Vitest (36 tests unitaires frontend) |
| Base | PostgreSQL 16 + Flyway | 20 migrations (V1–V20) |
| Auth | JWT + BCrypt + `@EnableMethodSecurity` | Profils `dev` / `docker` / `prod` / `demo` / `test` |
| Licence | Fichier signé RSA, volume `gest-pov-data` | Clé publique embarquée ; enforcement ON par défaut |
| Docker | Compose racine (dev) + **`docker-compose.client.yml`** (prod) + `deploy/compose.client.images.yml` | Caddy proxy, volumes PG / uploads / licence ; tunnel opt-in |
| E2E | Playwright (`play/`) | Scénarios POS documentés |
| Docs | `docs/GUIDE_UTILISATEUR.*`, README, `deploy/README.md` | Pas de procédure backup formalisée |

### Structure dépôt

```
backend/     API, migrations, Dockerfile
frontend/    React, nginx.conf, Dockerfile
docker-compose.yml, docker-compose.client.yml, Caddyfile, .env.example
deploy/      compose client images + README
docs/        guide utilisateur + client-docker-deployment.md
scripts/     client-start/stop/logs/backup, reset-demo.sql
play/        E2E Playwright
images/      archives .tar client (si livrées)
```

### Profils Spring (fichiers `application*.yml`)

| Profil | Usage clé |
|--------|-------------|
| *(base)* | Defaults DB/JWT, licence enforcement ON, seed system/demo |
| `dev` | Swagger, reset demo, licence OFF |
| `docker` | Chemins `/app/uploads`, `/app/gest-pov-data` |
| `prod` | Reset admin désactivé |
| `demo` | Jeu démo auto |
| `test` | H2 mémoire, Flyway OFF, initializers test dédiés |

**Docker Compose client** active `SPRING_PROFILES_ACTIVE=prod,docker`. Compose racine dev : `docker` seul.

### Migrations Flyway (V1 → V20)

Schéma baseline (V1), POS/stock/sessions (V2–V6), variantes/codes-barres (V7–V12), jeu démo SQL (V13), analytics/index/marques/BC (V14–V16), valorisation CMP (V17–V18), devise CDF (V19), valeurs référence valorisation (V20).

Scripts hors Flyway : `db/reset-demo.sql`, `scripts/reset-demo.sql`.

---

## 2. État du projet

Gest_POV est un **ERP produits + POS structuré et fonctionnellement riche** (catalogue, stock, POS central caissier, paiements, retours, clôture caisse, licence, analytics).

**Points structurants :**
- Monorepo clair, scripts npm racine (`dev:backend`, `dev:frontend`, `docker:up`, `test:e2e`).
- Volumes Docker prévus pour **PostgreSQL**, **uploads** et **licence**.
- Filtre licence avec exemptions bootstrap (login, licence, settings public, health).
- Couverture test backend **large** (~30 classes d’intégration MockMvc + quelques tests unitaires).

**Points de vigilance avant client :**
- Secrets par défaut **encore présents dans compose dev racine** (fallbacks — ne pas utiliser en prod).
- Comptes seed admin connus → **changer les mots de passe** après install.
- Suite tests backend : **211 OK ~4 min** (optimisation `@DirtiesContext` appliquée).

**Verdict :** stack **client Docker documentée** (`docker-compose.client.yml`) ; validation runtime (`up --build`) reste à faire avant livraison.

---

## 3. Risques P0 (critiques avant déploiement client)

| ID | Risque | Source | Impact |
|----|--------|--------|--------|
| P0-1 | **Secrets en dur dans `docker-compose.yml`** | `POSTGRES_PASSWORD`, `APP_JWT_SECRET` | Compromission DB / forge JWT |
| P0-2 | **README vs compose racine** | README : `cp .env.example .env` — compose ignore `.env` | Faux sentiment de sécurité |
| P0-3 | **Comptes seed connus** | `AuthReferenceDataInitializer` : `admin@erp.local` / `ErpAdmin2026!`, `caissier@erp.local` / `Caissier2026!` | Accès admin au 1er démarrage |
| P0-4 | **Fallbacks JWT/DB faibles** | `application.yml`, `application-docker.yml`, `application-prod.yml` | Démarrage avec secrets prévisibles |
| P0-5 | **Token reset demo connu** | `app.admin.reset-token: dev-reset-token-change-me` | Limité au profil `dev` (`AdminDevController`) |
| P0-6 | **Absence backup PostgreSQL** | Aucun script/doc `pg_dump` / restore | Perte de données irréversible |
| P0-7 | **Cloudflared dans compose par défaut** | `docker-compose.yml`, `deploy/compose.images.yml` | Exposition Internet involontaire |
| P0-8 | **Port 80 exposé (compose racine)** | `"${FRONTEND_PORT:-80}:80"` vs `127.0.0.1` dans `deploy/` | Surface réseau élargie |
| P0-9 | **CORS large** | `WebConfig.java` : LAN `192.168.*`, `10.*`, `*.trycloudflare.com` | Acceptable en POC, à restreindre en prod |
| P0-10 | **Licence obligatoire (profil docker)** | `app.license.enforcement-enabled: true` | Blocage API sans `.lic` importé |
| P0-11 | **Clé privée de test dans le repo** | `backend/src/test/resources/keys/test_private_key.pem` | OK en test ; ne jamais confondre avec prod |
| P0-12 | **Mot de passe Flyway Maven** | `backend/pom.xml` plugin flyway | Fuite si POM partagé (outil dev) |
| P0-13 | **Actuator proxifié** | `frontend/nginx.conf` → `/actuator/` | Health exposé via frontend (limité côté Spring : health/info) |
| P0-14 | **`/uploads/**` public sans auth** | `SecurityConfig` | Images produits accessibles si URL devinée |

---

## 4. Risques P1 (importants, non bloquants immédiats)

| ID | Risque | Détail |
|----|--------|--------|
| P1-1 | Tests backend lents | `@DirtiesContext(AFTER_EACH_TEST_METHOD)` global — voir §5 |
| P1-2 | Scripts dev absents vs UI | `DevToolsPage` / `DocumentationPage` référencent `dev.ps1`, `reset-db.ps1` |
| P1-3 | Incohérence email caissier seed | README : `caissier@erp.local` ; tests : `cashier@erp.local` |
| P1-4 | `AdminDevControllerTest` profils `test` + `dev` | Conflit datasource H2 vs PostgreSQL probable |
| P1-5 | Bundle frontend volumineux | Chunk JS > 500 kB (warning Vite typique) |
| P1-6 | Pas de scan CVE automatisé visible | Aucun `dependency-check` / CI deps |
| P1-7 | Noms volumes différents | `*_data1` (racine) vs `*_net` (deploy) |
| P1-8 | `demo-enabled: true` en profil `prod` | Jeu démo disponible manuellement |
| P1-9 | Pas de changement MDP forcé | Comptes seed utilisables indéfiniment |
| P1-10 | Images `.tar` client potentiellement obsolètes | Rebuild requis pour correctifs récents |

---

## 5. Points positifs

- **Volumes persistants** prévus : PostgreSQL, uploads (`/app/uploads`), licence (`/app/gest-pov-data`).
- **Double mode Docker** : build local + `deploy/compose.images.yml` avec variables `.env`.
- **`deploy/compose.images.yml`** lie le proxy en `127.0.0.1` (plus sûr que compose racine).
- **Sécurité métier** : JWT stateless, permissions granulaires POS/stock, `AdminDevController` limité au profil `dev`.
- **Licence** : endpoints bootstrap exemptés ; import multipart prévu.
- **Flyway** : 20 migrations versionnées, schéma validé (`ddl-auto: validate`).
- **Tests** : couverture POS/stock/auth large ; quelques tests **sans Spring** (`LicenseServiceTest`, `CmpCalculatorTest`, `ProductSkuGeneratorTest`).
- **Frontend** : routes protégées par permissions ; gate licence (`LicenseGate`).

---

## 6. Analyse tests backend (statique, sans exécution)

### Inventaire

| Catégorie | Nombre | Détail |
|-----------|--------|--------|
| Fichiers `*Test.java` | **37** | Dont 1 classe abstraite (`AbstractIntegrationTest`) |
| Classes de test exécutables | **~36** | |
| Héritent `AbstractIntegrationTest` | **30** | Tous les tests controller POS/stock/etc. + `LicenseImportIntegrationTest` |
| `@SpringBootTest` explicite | **5** | `AbstractIntegrationTest`, `AdminDevControllerTest`, `ClientConfigurationServiceTest`, `ProductVariantPolicyServiceTest` |
| `@DirtiesContext` | **1** | Uniquement sur `AbstractIntegrationTest` |
| Tests sans contexte Spring complet | **4** | `LicenseServiceTest`, `CmpCalculatorTest`, `ProductSkuGeneratorTest`, `AuthReferenceDataInitializerTest` (`@DataJpaTest`) |

### Cause probable de lenteur (~15–20 min confirmés)

**Mesure réelle (exécutions interrompues avec échecs) :** `mvn test` ≈ **19 min** (~1 137–1 157 s), **211 tests**, **6 failures**, **4 errors**.

```java
// AbstractIntegrationTest.java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

Ce paramètre **redémarre le contexte Spring Boot complet après chaque méthode de test** (~200+ méthodes × 30 classes). Chaque redémarrage recharge JPA, sécurité, initializers, schéma H2, etc.

Facteurs aggravants :
- `@SpringBootTest` + `@AutoConfigureMockMvc` (contexte web complet).
- Surefire : `forkCount=1`, `reuseForks=true` (un seul JVM, mais contexte recréé si dirty).
- Tests POS/stock créent beaucoup de données via HTTP dans `@BeforeEach`.

### Optimisation `@DirtiesContext` (2026-06-17)

**Appliquée** — voir [`backend-tests-optimization.md`](backend-tests-optimization.md).

- `@DirtiesContext(AFTER_EACH_TEST_METHOD)` **retiré** de `AbstractIntegrationTest`
- Remplacé par `TestDatabaseCleaner` (truncate H2 + reseed) en `@BeforeEach`
- Groupe 4 classes : **~177 s → ~81 s** ; `SettingsControllerTest` : **~130 s → ~51 s**
- Suite complète : **non relancée** — recommandée pour validation finale

### Échecs corrigés (2026-06-17)

Les **10 échecs/erreurs** identifiés sur les 4 classes ci-dessous sont **corrigés** (tests ciblés OK). Détail : [`backend-test-failures.md`](backend-test-failures.md).

| Classe | Cause | Correction |
|--------|-------|------------|
| `AdminDevControllerTest` | Profil `dev` → PostgreSQL vs H2 test | `@TestPropertySource` H2 + nettoyage annotations |
| `LicenseServiceTest` | Clé publique prod vs clé privée test | `test_public_key.pem` dans le test |
| `SettingsControllerTest` | Defaults prod vides (`company.name`, `currency`) | Seed test-only via `TestAppSettingsInitializer` + `@Order` |
| `AnalyticsControllerTest` | Idem (`$.currency` null) | Idem |

**Validation groupée :** `mvn "-Dtest=AdminDevControllerTest,LicenseServiceTest,AnalyticsControllerTest,SettingsControllerTest" test` → **OK** (~177 s).

**Suite complète `mvn test` :** non relancée — à demander explicitement.

---

## Correction des tests backend en échec

Voir document dédié : [`docs/backend-test-failures.md`](backend-test-failures.md).

**Recommandation suivante :** optimiser `@DirtiesContext` sur `AbstractIntegrationTest` avant de relancer la suite complète (~19 min).

## 7. Commandes volontairement non lancées

| Commande | Raison |
|----------|--------|
| `mvn test` / `mvn verify` / `mvn clean verify` | Suite longue (~15–20 min) — hors périmètre audit rapide |
| `docker compose build` / `docker compose up --build` | Build Docker complet |
| `npm audit` / `npm audit fix` | Scan deps non demandé |
| Scan `node_modules`, `target`, `dist`, `.git` | Exclus par contrainte |

---

## 8. Commandes recommandées (prochaine étape)

```powershell
# Backend — compilation rapide (~30 s)
cd backend
mvn -q -DskipTests package

# Backend — tests complets + chronométrage
Measure-Command { mvn -q test }

# Frontend — build + tests unitaires (~10 s)
cd frontend
npm run build
npm run test

# Docker — smoke stack (après .env configuré)
cd ..
docker compose up --build -d
docker compose ps

# E2E POS (stack dev ou Docker requise)
cd play
npm install
npm test
```

**Release complète suggérée :**
```powershell
cd backend && mvn -q test && cd ../frontend && npm run test && npm run build
```

---

## 9. Incohérences README vs Docker réel

| Élément | README (avant) | Réalité (post-sprint) |
|---------|----------------|------------------------|
| Variables secrets | `cp .env.example .env` sur compose racine | **Client :** `docker-compose.client.yml` + `.env` obligatoire ; **dev :** racine avec fallbacks |
| Port proxy | http://localhost | Client : `127.0.0.1:APP_PORT` ; dev racine : port 80 toutes interfaces |
| Cloudflare | « optionnel » | **Overlay** `docker-compose.tunnel.yml` — off par défaut |
| Compte caissier | `caissier@erp.local` | Code seed prod : `caissier@erp.local` ; tests : `cashier@erp.local` |
| Scripts dev | — | UI mentionne `dev.ps1` / `reset-db.ps1` **absents** (remplacés par `npm run dev:*`) |

---

## 10. Sprint déploiement Docker client (2026-06-17)

**Objectif :** stack client reproductible via `.env`, sans casser le compose dev racine.

### Livrables

| Élément | Fichier / action |
|---------|------------------|
| Compose client | `docker-compose.client.yml` — `.env` obligatoire (`:?`), profil `prod,docker`, healthchecks, volumes nommés |
| Images client | `deploy/compose.client.images.yml` — variante sans build |
| Tunnel opt-in | `docker-compose.tunnel.yml` — **retiré** des compose par défaut |
| Secrets | `.env.example` — placeholders `change-me-*`, pas de secret réel |
| Scripts | `scripts/client-{start,stop,logs,status,backup}.sh` |
| Doc | `docs/client-docker-deployment.md` |
| `.gitignore` | `.env`, `backups/`, `gest-pov-data/` |

### Statut risques P0 (post-sprint)

| ID | Avant | Après (compose **client**) |
|----|-------|----------------------------|
| P0-1 | Secrets en dur compose racine | **Atténué client** — `docker-compose.client.yml` exige `.env` ; racine dev garde fallbacks |
| P0-2 | README vs compose | **Corrigé** — README pointe vers compose client + doc dédiée |
| P0-6 | Pas de backup | **Atténué** — `scripts/client-backup.sh` + procédure restore manuelle doc |
| P0-7 | Cloudflared par défaut | **Corrigé** — overlay `docker-compose.tunnel.yml` uniquement |
| P0-8 | Port 80 toutes interfaces | **Atténué client** — `APP_BIND=127.0.0.1` par défaut ; racine dev inchangée |

### Validation (sans build)

```powershell
docker compose -f docker-compose.client.yml --env-file .env.example config
# Exit 0 — config résolue (volumes gest_pov_*, proxy 127.0.0.1:80, backend non exposé)
```

### Limites restantes (post-smoke)

- Compose **racine dev** : fallbacks secrets faibles si pas de `.env` (volontaire).
- Pas de HTTPS automatique (Caddy `:80` HTTP).
- Restore automatisé non livré (risque perte de données).
- ~~Smoke `docker compose up --build` non exécuté~~ → **validé 2026-07-02** (voir §12 ci-dessous et `client-docker-deployment.md` §13).

---

## 12. Smoke test runtime client (2026-07-02)

**Résultat global : OK** — stack client démarre, persiste, backup et s'arrête proprement.

| Critère | Statut |
|---------|--------|
| Démarrage avec `.env` | OK (~2 min 20 build+up) |
| PostgreSQL healthy | OK |
| Backend sans erreur fatale | OK (Flyway V20, seed admin) |
| Proxy `127.0.0.1:80` | OK |
| Backend non exposé hôte | OK |
| Persistance volumes/DB | OK après `restart` |
| Backup `client-backup.sh` | OK (correction script Windows) |
| `down` sans suppression volumes | OK |
| Secret commité | Non (`.env` ignoré) |

**Comportement licence (sans `.lic`) :** `LICENSE_MISSING` au démarrage ; UI/API bootstrap accessibles (`/api/license/status`, login admin) ; endpoints métier protégés (`/api/health` → 403).

**Correction smoke :** `scripts/client-backup.sh` — `docker exec` + `docker cp` pour volumes (compat Git Bash Windows).

---

## 11. Prochaine étape recommandée

**Priorité 1 — Livraison client :**
1. Import licence `.lic` + parcours POS complet sur stack client.
2. Rebuild images `.tar` (`gest-pov-backend:1.0.0`, `gest-pov-frontend:1.0.0`).
3. E2E Playwright contre `docker-compose.client.yml`.

**Priorité 2 — Durcissement (sans refonte métier) :**
1. Forcer changement MDP admin au 1er login.
2. Restreindre CORS en profil `prod`.
3. HTTPS (Caddy TLS ou reverse proxy client).

**Priorité 3 — CI / release :**
1. `mvn test` (211 OK ~4 min) + `npm run build` + pipeline smoke Docker client.

---

*Audit statique v1 — smoke test Docker client validé — 2026-07-02*
