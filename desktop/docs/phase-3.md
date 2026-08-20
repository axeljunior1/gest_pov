# Phase 3 — Flux Desktop fonctionnel

**Statut :** implémenté (discovery HTTP + UDP, login, GET /api/auth/me)

---

## Flux validé

```
Desktop
  → serveur mémorisé / hostname / UDP GEST_POV_DISCOVERY
  → GET /api/discovery (validation obligatoire)
  → application == GEST_POV + compatibilité version
  → POST /api/auth/login (JWT existant)
  → GET /api/auth/me (appel métier authentifié → PostgreSQL)
  → affichage JavaFX
```

---

## serverId

| Propriété | Valeur |
|-----------|--------|
| Format | UUID v4 |
| Fichier défaut | `{app.license.data-dir}/server.id` |
| Surcharge | `app.desktop.server-id-file` ou `GEST_POV_SERVER_ID_FILE` |
| Dev | `./gest-pov-data/server.id` |
| Test | `target/test-gest-pov-data/server.id` |
| Desktop Windows (futur) | `C:\ProgramData\GestPOV\config\server.id` |

Généré **une fois** au démarrage backend. Indépendant de l'IP. Web/Docker : fichier créé aussi, sans changer les APIs existantes (seul ajout : `GET /api/discovery`).

UDP discovery : **désactivé** par défaut. Activé uniquement profils `dev` et `desktop`.

---

## Compatibilité versions

Même **major** → `COMPATIBLE`  
Client major < serveur → `UPDATE_REQUIRED`  
Client major > serveur → `SERVER_TOO_OLD`

---

## Lancer

Voir [`docs/lancement-dev.md`](../../docs/lancement-dev.md) §8.

```powershell
# terminal 1
npm run dev:backend

# terminal 2
cd desktop\client
mvn javafx:run
```

---

## Tests

```powershell
mvn -f backend/pom.xml -Dtest=DiscoveryControllerTest,ServerIdentityServiceTest test
mvn -f desktop/client/pom.xml test
```
