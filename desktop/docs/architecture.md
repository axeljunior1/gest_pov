# Architecture — Gest POV Desktop (Windows / LAN / offline)

**Version document :** 0.1 (Phase 2)  
**Décision technologique Desktop :** Java 17 + JavaFX + Maven (module isolé `desktop/client/`)

---

## 1. Vue d'ensemble

```
┌─────────────────────────────────────────────────────────────┐
│                    PC SERVEUR (Windows)                      │
│  ┌──────────────┐   REST    ┌──────────────┐   JDBC        │
│  │ Gest POV     │ ────────> │ Spring Boot  │ ────────>     │
│  │ Desktop      │  :8080    │ (JAR existant)│  127.0.0.1   │
│  │ (JavaFX)     │           └──────────────┘       │       │
│  └──────────────┘                                  ▼       │
│                                          ┌──────────────┐  │
│                                          │ PostgreSQL   │  │
│                                          │ (service Win)│  │
│                                          └──────────────┘  │
│  ProgramData\GestPOV\  config | logs | data | backups       │
└─────────────────────────────────────────────────────────────┘
                              ▲
                              │ LAN (TCP API + UDP discovery)
              ┌───────────────┼───────────────┐
              │               │               │
         ┌────┴────┐    ┌────┴────┐    ┌────┴────┐
         │ PC2     │    │ PC3     │    │ PC4-5   │
         │ Desktop │    │ Desktop │    │ Desktop │
         │ only    │    │ only    │    │ only    │
         └─────────┘    └─────────┘    └─────────┘
```

**Règle absolue :** le Desktop ne parle jamais à PostgreSQL. Toujours via REST.

---

## 2. Décisions techniques

| Sujet | Décision | Raison |
|-------|----------|--------|
| UI Desktop | JavaFX | Écosystème Java, packaging Windows, offline |
| Métier | Backend Spring Boot existant | Une seule source de vérité |
| Frontend React | Conservé pour Web/Docker | Pas de remplacement |
| Runtime Java client | Embarqué (`jpackage` / `jlink`) | Client sans JDK |
| PostgreSQL | Uniquement poste serveur | Sécurité + simplicité clients |
| Discovery | UDP broadcast + validation HTTP | Pas de saisie IP manuelle |
| serverId | UUID persistant install serveur | Retrouver serveur si IP change |
| Profil Spring serveur | `prod,desktop` (proposé) | Séparer config Desktop sans toucher docker |
| Secrets DB | Générés à l'install, protégés Windows | Jamais sur postes clients |

---

## 3. Communication

### Poste serveur (utilisateur local)

```
Desktop → http://127.0.0.1:8080/api/...
```

### Postes clients LAN

```
Desktop → http://<IP_SERVEUR>:8080/api/...
```

### Découverte (3 étapes)

1. Tester config mémorisée (`server.id`, `server.host`, `server.port`)
2. Tester hostname local connu (`gest-pov-server.local` — optionnel mDNS futur)
3. Broadcast UDP `GEST_POV_DISCOVERY` → réponses JSON → validation HTTP `GET /api/discovery`

Le client **refuse** un serveur si :

- `application != "GEST_POV"`
- HTTP discovery échoue
- `serverId` inconnu (première connexion : confirmation utilisateur)
- Version incompatible

---

## 4. Configuration

### Desktop (client.properties)

```properties
# Jamais de credentials PostgreSQL ici
server.id=
server.host=
server.port=8080
server.name=
last.connected.at=

# Compatibilité
client.version=1.0.0
min.server.version=1.0.0
```

Emplacement proposé : `%APPDATA%\GestPOV\client.properties`

### Serveur (application-desktop.yml + secrets)

```yaml
# Chemins Windows — voir server-package/config/
app:
  upload:
    dir: C:/ProgramData/GestPOV/data/uploads
  license:
    data-dir: C:/ProgramData/GestPOV/data/license
  desktop:
    server-id-file: C:/ProgramData/GestPOV/config/server.id
    discovery:
      enabled: true
      udp-port: 38471
```

Secrets DB : fichier protégé DPAPI ou variable service Windows — voir `installation-server.md`.

---

## 5. Services Windows

| Service | Nom proposé | Dépendance |
|---------|-------------|------------|
| PostgreSQL | `GestPOV-PostgreSQL` | Aucune |
| Backend | `GestPOV-Server` | PostgreSQL démarré |

Démarrage automatique au boot Windows. Logs dans `C:\ProgramData\GestPOV\logs\`.

---

## 6. Firewall (réseau privé uniquement)

| Protocole | Port | Direction | Usage |
|-----------|------|-----------|-------|
| TCP | 8080 (configurable) | Entrant LAN | API REST |
| UDP | 38471 (configurable) | Entrant LAN | Discovery |

**PostgreSQL 5432 :** écoute `127.0.0.1` uniquement — **pas** ouvert au LAN.

---

## 7. Compatibilité versions

Réponse discovery / health inclut `version` (semver backend).

| Statut client | Condition |
|---------------|-----------|
| `COMPATIBLE` | Versions dans la plage supportée |
| `UPDATE_REQUIRED` | Client trop ancien |
| `SERVER_TOO_OLD` | Serveur trop ancien pour ce client |

Matrice exacte à maintenir dans `build-release.md`.

---

## 8. Relation Web/Docker

```
Gest POV
    │
    ├── Web/Docker (inchangé)
    │     React → /api → Spring Boot → PostgreSQL (conteneur)
    │
    └── Desktop/LAN (nouveau)
          JavaFX → /api → Spring Boot → PostgreSQL (service Windows)
```

Même JAR backend (ou variante empaquetée identique), profils différents.

---

## 9. Phases d'implémentation

| Phase | Livrable | Statut |
|-------|----------|--------|
| 1 | Audit (`current-project-audit.md`) | ✅ |
| 2 | Architecture (ce document) | ✅ |
| 3 | Skeleton : REST, config, discovery, login | 🔲 |
| 4 | Packaging serveur (PG + JAR + services) | 🔲 |
| 5 | Migration écrans Desktop (POS prioritaire) | 🔲 |
| 6 | Installateurs `.exe` | 🔲 |
| 7 | Tests offline VM | 🔲 |

---

## 10. Modifications backend autorisées (minimales)

Liste détaillée : [`backend-additions-required.md`](backend-additions-required.md).

Principe : **ajouts only** — pas de refactoring des controllers existants.
