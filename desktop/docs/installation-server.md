# Installation serveur Desktop — Gest POV (Phase 4)

Cible future : `GestPOV-Server-Setup.exe` (pas encore produit).

**Livrable actuel :** dossier `GestPOV-Server-Offline` + scripts PowerShell.  
Détails : [phase-4.md](phase-4.md) · test VM : [offline-vm-test.md](offline-vm-test.md) · client : [installation-client.md](installation-client.md)

## Installer (offline)

```powershell
# 1. Build editeur (avec Internet)
powershell -File desktop\scripts\build\build-offline-package.ps1

# 2. Sur le PC serveur, USB, PowerShell Administrateur
cd GestPOV-Server-Offline
.\install-server.ps1
.\health-check.ps1
```

Compte `admin@erp.local` : **dev uniquement**. Production Desktop : `INITIAL_ADMIN.txt` généré à l'install.

## Arborescence runtime

Voir phase-4.md (Program Files + ProgramData).

## Réseau (caisse LAN)

Ouvrir sur le **serveur** :

- TCP **8080** (API HTTP)
- UDP **38471** (découverte)

PostgreSQL reste en localhost. Les clients ne parlent qu’au backend HTTP.
