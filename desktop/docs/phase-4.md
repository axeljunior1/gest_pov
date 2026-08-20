# Phase 4 — Packaging serveur Windows offline

**Statut :** scripts + structure de package — installateur `.exe` **non** produit (Phase 6).

---

## Mecanisme services Windows

| Service | Outil | Pourquoi |
|---------|--------|----------|
| `GestPOV-PostgreSQL` | `pg_ctl register` (binaire PostgreSQL embarque) | Officiel, pas de wrapper tiers pour le cluster |
| `GestPOV-Server` | **WinSW 2.12** embarque (`WinSW.exe`) | Wrapper Java/Windows eprouve ; binaire copie dans le package ; **aucun telechargement client** |

WinSW lance `start-backend.ps1`, qui dechiffre les secrets **DPAPI LocalMachine** puis execute :

```text
runtime\bin\java.exe -jar gest-pov-server.jar --spring.config.additional-location=file:C:/ProgramData/GestPOV/config/
```

Dependance de service : Gest POV attend PostgreSQL (`<depend>`).

Le demarrage API n'utilise **pas** un sleep fixe : `pg_isready` puis HTTP `GET /actuator/health/liveness` en boucle.

Profils Spring : `prod,desktop` (seed `admin@erp.local` **desactive** ; bootstrap admin unique genere a l'install).

---

## Donnees / logs / backups / secrets

| Element | Chemin |
|---------|--------|
| Binaires | `C:\Program Files\GestPOV\` (`server`, `runtime`, `postgres`) |
| Config | `C:\ProgramData\GestPOV\config\` |
| `server.id` | `C:\ProgramData\GestPOV\config\server.id` |
| Secrets DPAPI | `C:\ProgramData\GestPOV\config\secrets.dpapi` (ACL SYSTEM + Administrators) |
| Admin initial (une fois) | `C:\ProgramData\GestPOV\config\INITIAL_ADMIN.txt` |
| Cluster PG | `C:\ProgramData\GestPOV\postgres\data` |
| Licence | `C:\ProgramData\GestPOV\license\` |
| Uploads | `C:\ProgramData\GestPOV\uploads\` |
| Logs | `C:\ProgramData\GestPOV\logs\` (`installer.log`, `server.log`, `backup.log`, logs WinSW) |
| Backups | `C:\ProgramData\GestPOV\backups\yyyyMMdd-HHmmss\` |

**Protection secrets :** DPAPI `LocalMachine` + ACL NTFS. Pas le meme mot de passe pour tous les clients. Jamais dans Git, logs, ni XML WinSW.

**Limite :** tout administrateur local de la machine peut dechiffrer DPAPI LocalMachine. Acceptable pour un PC serveur unique ; documente. Pas de mot de passe PostgreSQL cote Desktop JavaFX.

PostgreSQL : `listen_addresses = 127.0.0.1`, `pg_hba` localhost only. Role applicatif `gest_pov_app`, base `gest_pov`. Superuser cluster `gest_pov_admin` (maintenance), **pas** pour l'API.

---

## Commandes

### Build editeur (Internet OK)

```powershell
cd <repo>
powershell -ExecutionPolicy Bypass -File desktop\scripts\build\build-offline-package.ps1
# Option : -SkipTests
```

Sortie : `desktop/server-package/build/GestPOV-Server-Offline\`

### Install client (offline)

PowerShell **Administrateur** :

```powershell
cd X:\GestPOV-Server-Offline
.\install-server.ps1
```

Idempotent : relancer ne detruit pas DB, `server.id`, licence, ni secrets.

### Desinstall

```powershell
.\uninstall-server.ps1
# Purge ProgramData :
.\scripts\server\uninstall-server.ps1 -PurgeData
```

### Health / backup

```powershell
.\health-check.ps1
.\scripts\server\backup-server.ps1
.\scripts\server\restore-server.ps1 -BackupDir 'C:\ProgramData\GestPOV\backups\YYYYMMDD-HHMMSS'
```

---

## Structure du package offline

```
GestPOV-Server-Offline/
  README.txt
  install-server.ps1
  uninstall-server.ps1
  health-check.ps1
  backend/gest-pov-server.jar
  runtime/bin/java.exe     # jlink (ou copie JDK editeur)
  postgres/bin/initdb.exe  # binaires PostgreSQL 16
  winsw/WinSW.exe
  scripts/server/*.ps1
  config/
```

---

## Bootstrap admin

Profil `prod` : `app.seed.default-users-enabled=false` → `AdminBootstrapService` (existant).

L'installateur genere un mot de passe unique et `admin@gestpov.local`, injecte `APP_BOOTSTRAP_ADMIN_*` dans le process service. Les comptes `admin@erp.local` / `ErpAdmin2026!` restent **uniquement** profil `dev`/`test`.

---

## Flyway

Inchange. Premier start Spring : V1…Vn sur base vide. Mises a jour futures : nouvelles migrations dans le JAR.

---

## Pare-feu

Regles **profil prive** uniquement :

- TCP 8080 `GestPOV-API-TCP`
- UDP 38471 `GestPOV-Discovery-UDP`

PostgreSQL non ouvert au LAN.

---

## Tests scripts (sans installer PG)

```powershell
powershell -ExecutionPolicy Bypass -File desktop\scripts\server\tests\Test-GestPovSecrets.ps1
```
