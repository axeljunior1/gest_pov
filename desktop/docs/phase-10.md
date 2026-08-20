# Phase 10 — Polish, package client, marche d’install

**Statut :** navigation complète, raccourcis, package dossier client (pas d’installateur `.exe`). Phase 4.5 VM/LAN **NOT_EXECUTED** tant qu’une VM n’a pas été testée.

---

## Livré

- Nav Catalogue / Stock / Ventes / Paramètres selon permissions
- Raccourcis : F4 POS, F2 recherche POS, Ctrl+1…8 écrans
- Package serveur inchangé : `desktop/scripts/build/build-offline-package.ps1`
- Package client : `desktop/scripts/build/build-client-package.ps1` → `GestPOV-Client-Offline`
- Install client : `install-client.ps1` (copie + raccourcis, pas besoin d’admin sauf `-AllUsers`)

## Non livré

| Élément | État |
|---------|------|
| `GestPOV-Server-Setup.exe` / `GestPOV-Client-Setup.exe` | TODO (WiX / jpackage) |
| Upgrade offline versionné `.exe` | TODO |
| Test VM Windows vierge + 2e PC LAN | NOT_EXECUTED — [offline-vm-test.md](offline-vm-test.md) |
| UI licence côté Desktop | TODO (import `.lic` **sur le serveur**) |
| POS complet (retours, hold, reports) | PARTIAL — Phase 9 cœur |

---

## Raccourcis

| Touche | Écran |
|--------|--------|
| Ctrl+1 | Produits |
| Ctrl+2 | Catégories |
| Ctrl+3 | Marques |
| Ctrl+4 | Fournisseurs |
| Ctrl+5 | Unités |
| Ctrl+6 | Stock |
| Ctrl+7 | Clients |
| Ctrl+8 | Paramètres |
| F4 | Caisse POS |
| F2 | Recherche POS |

---

## Packages

Voir [installation-server.md](installation-server.md) et [installation-client.md](installation-client.md).
