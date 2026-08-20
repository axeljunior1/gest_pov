# Gest POV — édition Desktop (Windows / LAN / offline)

Ce dossier contient **toute la déclinaison Desktop** de Gest POV.

## Principe

```
                  Gest POV (métier centralisé)
                         │
           ┌─────────────┴─────────────┐
           │                           │
     Web / Docker                 Desktop / LAN
     (inchangé)                   (ce dossier)
           │                           │
     React + API                 JavaFX + API
           │                           │
           └──── backend Spring Boot ──┘
                 (source de vérité)
```

**Le projet Web/Docker à la racine du dépôt n'est pas remplacé.**  
Aucun build existant (`docker compose`, `npm run dev`, `mvn test`) ne doit être cassé par le travail Desktop.

## Structure

| Dossier | Rôle |
|---------|------|
| `client/` | Application Desktop JavaFX (postes vendeur/caissier) |
| `server-package/` | Modèles de config, scripts et empaquetage serveur (Spring Boot + PostgreSQL) |
| `installer/` | Sources des installateurs Windows (Server / Client) |
| `scripts/` | Scripts build, health, backup Desktop |
| `tests/` | Tests Desktop + intégration LAN |
| `docs/` | Documentation architecture, installation, offline |

## Documentation

| Document | Contenu |
|----------|---------|
| [docs/current-project-audit.md](docs/current-project-audit.md) | Audit du dépôt existant |
| [docs/architecture.md](docs/architecture.md) | Architecture cible Desktop |
| [docs/environment-variables.md](docs/environment-variables.md) | Variables d'environnement Desktop vs Web |
| [docs/lancement-dev.md](../docs/lancement-dev.md) | **Lancement dev Web** (racine `docs/`) |
| [docs/installation-server.md](docs/installation-server.md) | Installation poste serveur |
| [docs/installation-client.md](docs/installation-client.md) | Installation postes clients |
| [docs/offline-requirements.md](docs/offline-requirements.md) | Contraintes 100 % offline |
| [docs/network-discovery.md](docs/network-discovery.md) | Découverte LAN + serverId |
| [docs/backup-restore.md](docs/backup-restore.md) | Sauvegardes Desktop |
| [docs/build-release.md](docs/build-release.md) | Build installateurs |
| [docs/migration-status.md](docs/migration-status.md) | Matrice migration React → Desktop |
| [docs/phase-3.md](docs/phase-3.md) | Flux discovery + login |
| [docs/phase-4.md](docs/phase-4.md) | Packaging serveur offline |
| [docs/phase-5.md](docs/phase-5.md) | Premier module métier : Marques |
| [docs/phase-6.md](docs/phase-6.md) | Catégories (arbre, CRUD, rattachement) |
| [docs/phase-7.md](docs/phase-7.md) | Produits (liste + fiche cœur) |

## État actuel

**Phase 1–2 :** audit + architecture — fait.  
**Phase 3 :** discovery + login + GET /api/auth/me — fait. Voir [docs/phase-3.md](docs/phase-3.md).  
**Phase 4 :** package serveur offline + scripts PowerShell — fait. Voir [docs/phase-4.md](docs/phase-4.md).  
**Phase 5 :** Marques — fait. Voir [docs/phase-5.md](docs/phase-5.md).  
**Phase 6 :** Catégories — fait. Voir [docs/phase-6.md](docs/phase-6.md).  
**Phase 7 :** Produits (cœur) — fait. Voir [docs/phase-7.md](docs/phase-7.md).  
**Phase 8+ :** modules secondaires, POS, installateur `.exe` — à venir (une phase à la fois).

## Lancer le client Desktop (dev)

Le backend profil `dev` doit tourner (UDP discovery activée).

```bash
cd desktop/client
mvn test
mvn javafx:run
```
