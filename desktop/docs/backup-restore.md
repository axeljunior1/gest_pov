# Sauvegarde et restauration — Desktop serveur

Réutilise la **logique** du script Docker existant (`scripts/client-backup.sh`) adaptée à Windows natif.

---

## Contenu d'une sauvegarde

| Élément | Source | Fichier backup |
|---------|--------|----------------|
| Base PostgreSQL | `pg_dump -Fc` localhost | `postgres.dump` |
| Licence + installationId | `%ProgramData%\GestPOV\data\license\` | `license.tar` |
| Uploads (images, logos) | `%ProgramData%\GestPOV\data\uploads\` | `uploads.tar` |
| Métadonnées | Généré | `manifest.json` |

---

## Emplacement

```
C:\ProgramData\GestPOV\backups\YYYYMMDD-HHMMSS\
├── postgres.dump
├── license.tar
├── uploads.tar
├── manifest.json
└── README.txt
```

---

## Rotation

| Paramètre | Défaut proposé |
|-----------|----------------|
| Fréquence | Quotidienne 02:00 |
| Rétention | 14 jours |
| Espace minimum alerte | 1 Go libre |

---

## Sauvegarde manuelle + export USB

Menu admin Desktop serveur (futur) :

- **Sauvegarder maintenant**
- **Exporter vers dossier** (clé USB)

---

## Restauration (procédure)

```
1. Arrêter service GestPOV-Server
2. Restaurer postgres.dump (pg_restore --clean)
3. Restaurer license/ et uploads/
4. Redémarrer service
5. Health check + test login
```

> **Attention :** restauration = écrasement données courantes. Confirmation obligatoire.

---

## Référence Docker existante

Script : `scripts/client-backup.sh`  
Doc : `docs/client-docker-deployment.md` §10

**Phase 4 :** scripts `desktop/scripts/server/backup-server.ps1` (Windows natif, `pg_dump` embarque).  
Dump dans `C:\ProgramData\GestPOV\backups\<timestamp>\`. Voir [phase-4.md](phase-4.md).

---

## Logs backup

`C:\ProgramData\GestPOV\logs\backup.log`

Ne jamais logger mots de passe DB.
