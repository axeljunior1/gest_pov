# Scripts Desktop Windows

## Serveur (Phase 4)

Canonical : `desktop/scripts/server/`

| Script | Role |
|--------|------|
| `install-server.ps1` | Install idempotente (admin) |
| `uninstall-server.ps1` | Retrait services/binaires ; `-PurgeData` optionnel |
| `start-server.ps1` / `stop-server.ps1` / `restart-server.ps1` | Services |
| `health-check.ps1` | Diagnostic (aucun secret) |
| `backup-server.ps1` / `restore-server.ps1` | pg_dump local |
| `fix-server-start.ps1` / `fix-db-password.ps1` | Maintenance (via menu) |

**Lanceurs package (seuls fichiers utiles a la racine USB) :**

| Fichier | Role |
|---------|------|
| `bat/01-Installer.bat` | Premiere install |
| `bat/02-Menu.bat` | Menu : demarrer, diagnostic, reparer, desinstaller |

Tests secrets : `desktop/scripts/server/tests/Test-GestPovSecrets.ps1`

## Build editeur

| Script | Role |
|--------|------|
| `build/build-all-installers.ps1` | **Serveur + client** → copie dans `desktop/dist/` |
| `build/build-offline-package.ps1` | Package dossier serveur USB |
| `build/build-client-package.ps1` | Package dossier client USB |
| `build/build-exe-stub.ps1` | Stub doc jpackage `.exe` (Phase H) |

Raccourci double-clic : `desktop/construire-installateurs.bat`  
Guide install : `desktop/INSTALLATION.md`  

Roadmap `.exe` / VM / backlog : `desktop/docs/phase-h.md`.
