# Test VM Windows vierge — Phase 4

## Machine

- Windows 10/11 64-bit **propre**
- Desactiver Internet (ou VM sans NIC WAN)
- Verifier absence : Java, PostgreSQL, Maven, Git, Docker, Node

## Protocole

1. Chez l'editeur (avec Internet) : `desktop\scripts\build\build-offline-package.ps1`
2. Copier `desktop\server-package\build\GestPOV-Server-Offline` sur cle USB
3. Sur la VM : copier le dossier, PowerShell admin :

```powershell
Set-ExecutionPolicy -Scope Process Bypass
cd C:\Temp\GestPOV-Server-Offline
.\install-server.ps1
.\health-check.ps1
```

4. Attendu : PostgreSQL Running, liveness 200, `/api/discovery` GEST_POV, serverId non vide
5. Lire `C:\ProgramData\GestPOV\config\INITIAL_ADMIN.txt` (ne pas le committer)
6. Relancer `.\install-server.ps1` → pas de recreation DB / server.id
7. Reboot VM → services Automatic, health-check OK
8. Desktop Phase 3 vers `127.0.0.1:8080` (si le client est deja sur la machine ; sinon skip POS)

## Backup test (sur la VM)

```powershell
.\scripts\server\backup-server.ps1
# noter le dossier timestamp
.\scripts\server\restore-server.ps1 -BackupDir 'C:\ProgramData\GestPOV\backups\<stamp>'
.\health-check.ps1
```

Verifier qu'un dump `postgres.dump` a une taille > 32 octets (le script refuse un fichier vide).

## Non couvert tant que la VM n'a pas tourne

- `pg_ctl register` selon la build PostgreSQL EDB
- WinSW + ExecutionPolicy LocalSystem
- jlink Java 24 vs 17 (preferer JDK 17 editeur)
- Collision port 5432 si un PostgreSQL Windows existait (cette VM ne doit pas en avoir)
