# Installateurs Windows — Gest POV

## Aujourd’hui (recommandé)

Packages **dossiers offline** (USB), pas de Setup.exe :

1. Double-clic : [`../construire-installateurs.bat`](../construire-installateurs.bat)
2. Récupérer les dossiers dans [`../dist/`](../dist/)
3. Suivre le guide : [`../INSTALLATION.md`](../INSTALLATION.md)

Scripts unitaires :

| Script | Sortie |
|--------|--------|
| `../scripts/build/build-all-installers.ps1` | Serveur + client → `desktop/dist/` |
| `../scripts/build/build-offline-package.ps1` | `GestPOV-Server-Offline` |
| `../scripts/build/build-client-package.ps1` | `GestPOV-Client-Offline` |

## Futur (Phase H)

`GestPOV-*-Setup.exe` via jpackage / WiX — voir [`../docs/phase-h.md`](../docs/phase-h.md) et `../scripts/build/build-exe-stub.ps1`.
