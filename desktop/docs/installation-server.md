# Installation serveur Desktop — Gest POV (Phase 4)

Cible future : `GestPOV-Server-Setup.exe` (pas encore produit).

**Livrable actuel :** dossier `GestPOV-Server-Offline` + **2 lanceurs** à la racine.  
Détails : [phase-4.md](phase-4.md) · guide : [../INSTALLATION.md](../INSTALLATION.md) · client : [installation-client.md](installation-client.md)

## Installer (offline)

```text
1. Build editeur : desktop\construire-installateurs.bat
2. Sur le PC serveur, USB :
     01-Installer.bat     (1ere install, admin)
     02-Menu.bat          → 4) Diagnostic
```

Compte `admin@erp.local` : **dev uniquement**. Production Desktop : `INITIAL_ADMIN.txt` généré à l'install.

## Arborescence runtime

Voir phase-4.md (Program Files + ProgramData).

## Réseau (caisse LAN)

Ouvrir sur le **serveur** :

- TCP **8080** (API HTTP)
- UDP **38471** (découverte)

PostgreSQL reste en localhost. Les clients ne parlent qu’au backend HTTP.
