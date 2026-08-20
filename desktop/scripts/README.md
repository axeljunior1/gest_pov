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

Tests secrets : `desktop/scripts/server/tests/Test-GestPovSecrets.ps1`

## Build editeur

| Script | Role |
|--------|------|
| `build/build-offline-package.ps1` | Package dossier serveur USB (Internet OK chez l'editeur uniquement) |
| `build/build-client-package.ps1` | Package dossier client USB |
| `build/build-exe-stub.ps1` | Stub doc jpackage `.exe` (Phase H) — ne casse pas les builds dossier |

Roadmap `.exe` / VM / backlog : `desktop/docs/phase-h.md`.
