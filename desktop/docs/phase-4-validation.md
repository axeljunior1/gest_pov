# Phase 4.5 — Validation réelle du package serveur offline

**Date :** 2026-08-19  
**Machine d’exécution :** PC développeur Windows (`DESKTOP-HDR5A22`), **pas** une VM vierge.  
**Phase 5 / migration POS :** non commencée.

**Verdict :** le package offline a été **construit réellement** et le cœur (runtime Java 17 embarqué + PostgreSQL embarqué + JAR + Flyway + discovery + login JWT) a été prouvé **hors installation Windows**. Les tests qui exigent une VM propre, des services WinSW, un reboot, un second PC LAN ou `install-server.ps1` en administrateur sont **NOT_EXECUTED**.

Package produit :

```text
desktop/server-package/build/GestPOV-Server-Offline/
```

---

## Tableau de synthèse

| Test                      | Résultat      | Détails |
| ------------------------- | ------------- | ------- |
| Build package             | PASS          | Script exécuté. Dossier complet : `backend/`, `runtime/`, `postgres/`, `winsw/`, `scripts/`, `config/templates/`, `install-server.ps1`, `uninstall-server.ps1`, `health-check.ps1`, `README.txt`. JAR `gest-pov-server.jar` (77,8 Mo). WinSW 2.12 (17,4 Mo). |
| Runtime Java embarqué     | PASS          | Premier jlink = Java **24** (JAVA_HOME machine). Corrigé : runtime **17.0.12**. `java.home` = dossier `runtime` du package. JAR démarré avec `JAVA_HOME` fictif et Java retiré du `PATH` (`where java` introuvable). Spring Boot 3.2.5 : « using Java 17.0.12 ». |
| PostgreSQL init           | PASS          | `initdb.exe` du package, cluster temporaire `%TEMP%\gestpov-p45\pgdata`, locale C, UTF-8, scram-sha-256. **Pas** via `install-server.ps1` vers `C:\ProgramData\GestPOV`. |
| PostgreSQL service        | NOT_EXECUTED  | Aucun `pg_ctl register` / service `GestPOV-PostgreSQL`. Preuve partielle : `pg_ctl start` utilisateur + `pg_isready` OK sur `127.0.0.1:55432`. |
| WinSW service             | NOT_EXECUTED  | `WinSW.exe` présent dans le package. Service `GestPOV-Server` non installé (droits admin + collision 5432 sur ce PC). XML généré par l’installeur contient `<depend>GestPOV-PostgreSQL</depend>` et `<startmode>Automatic</startmode>` (revue statique). |
| Flyway                    | PASS          | 20 migrations appliquées sur PostgreSQL **16.6** embarqué, schéma `public` → v20. Avertissement Flyway 9.22.3 : support officiel jusqu’à PG 15 ; migrations OK malgré tout. |
| Discovery HTTP            | PASS          | `GET http://127.0.0.1:18080/api/discovery` → 200, `application=GEST_POV`, `serverId=0e08de18-a1e0-43e4-b48c-e7a96c0d7e18`, `version=1.0.0`, `status=READY`. |
| UDP discovery LAN         | NOT_EXECUTED  | Pas de second PC/VM. Preuve locale : sonde `GEST_POV_DISCOVERY` → `127.0.0.1:38471` répond le même JSON READY. |
| Login                     | PASS          | `POST /api/auth/login` bootstrap `admin@gestpov.local` → 200, JWT 2512 caractères. Sandbox avec `--app.license.enforcement-enabled=false` (voir notes licence). |
| JWT /auth/me              | PASS          | `GET /api/auth/me` Bearer → 200, email `admin@gestpov.local`, rôle `SUPER_ADMIN`. |
| Firewall                  | NOT_EXECUTED  | `netsh advfirewall` non exécuté (installeur non lancé). |
| PostgreSQL non exposé LAN | NOT_EXECUTED  | Pas de sonde depuis une 2e machine. Preuve locale sandbox : `netstat` = `127.0.0.1:55432` uniquement (pas `0.0.0.0:55432`). API écoutait `0.0.0.0:18080` comme prévu. |
| Reboot                    | NOT_EXECUTED  | Redémarrage Windows non effectué. |
| Idempotence               | NOT_EXECUTED  | `install-server.ps1` non relancé sur une install existante. Revue : le script conserve `server.id`, secrets, cluster `PG_VERSION`, rôle/DB existants. |
| Backup                    | NOT_EXECUTED  | `backup-server.ps1` (admin + `Program Files`) non lancé. Preuve binaire : `pg_dump.exe` du package → `postgres.dump` 237 998 octets, exit 0. |
| Restore                   | NOT_EXECUTED  | `restore-server.ps1` non lancé. Preuve binaire : `last_name` `Local` → `TAMPERED` → `pg_restore --clean` → `Local` de nouveau. |
| Uninstall                 | NOT_EXECUTED  | `uninstall-server.ps1` non lancé. Revue : services + firewall + `C:\Program Files\GestPOV` ; `ProgramData` conservé sauf `-PurgeData`. |
| Reinstall                 | NOT_EXECUTED  | Dépend de l’uninstall. |
| Offline total             | NOT_EXECUTED  | Internet n’a pas été coupé ; Java/Maven/Git présents. Revue scripts **client** : aucun téléchargement (voir §19). |

Légende : **PASS** = exécuté ici avec succès. **NOT_EXECUTED** = impossible ou volontairement non fait sur ce PC de dev (pas de VM propre, pas d’install services). Aucun **FAIL** produit à ce stade.

---

## 1. Build package

Commande réelle :

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File desktop\scripts\build\build-offline-package.ps1 -SkipTests
```

(`-SkipTests` : suite backend déjà verte en Phase 4 ; le packaging n’a pas relancé les 225 tests.)

Incidents de build corrigés :

| Cause | Correction |
|-------|------------|
| Parse PowerShell 5.1 (continuation `` ` `` + tiret cadratin) | Arguments jlink en tableau ; tirets ASCII. |
| `Invoke-WebRequest` EDB bloqué après ~290 Mo (zip pourtant valide) | Cache vendor réutilisé ; téléchargements éditeur via `curl.exe` + timeout. |
| `Expand-Archive` ~15 min / 22 653 fichiers | Extraction `tar.exe` pour les builds suivants. |
| Layout `config/templates/` manquant | Copie du YAML template dans `config/templates/`. |
| jlink initial = JDK **24** | Préférence JDK **17** ; runtime du package régénéré en 17.0.12. |

Téléchargements **éditeur uniquement** (Internet OK chez l’éditeur) :

- PostgreSQL 16.6 Windows binaries (zip EDB, ~304 Mo, validé ZipFile 22 653 entrées)
- WinSW v2.12 `WinSW-x64.exe`

---

## 2. Runtime Java

```text
java.home = ...\GestPOV-Server-Offline\runtime
java.version = 17.0.12
```

Le processus Spring Boot a loggé :

```text
Starting ProductsApplication v1.0.0 using Java 17.0.12
The following 2 profiles are active: "prod", "desktop"
```

Le lanceur d’install (`start-backend.ps1`) appelle `Join-Path $P.RuntimeDir 'bin\java.exe'`, jamais `java` du PATH.

---

## 3. PostgreSQL embarqué

Binaires présents dans `postgres\bin\` :

`initdb.exe`, `pg_ctl.exe`, `pg_isready.exe`, `psql.exe`, `pg_dump.exe`, `pg_restore.exe`, `postgres.exe`.

`install-server.ps1` (package et sources) : **aucun** `Invoke-WebRequest` / `curl` / `wget` / `winget` / `choco` / `DownloadFile`.

---

## 4. Environnement de cette machine (pourquoi beaucoup de NOT_EXECUTED)

| Attendu VM propre | Cette machine |
|-------------------|---------------|
| Java absent | JDK 11, 17, 24 installés |
| PostgreSQL absent | Historique Docker / PG Windows possible sur 5432 |
| Maven / Git / Node absents | Présents |
| Internet désactivé | Actif (nécessaire au **build éditeur**) |
| Gest POV absent | Non installé en `Program Files` (sandbox TEMP seulement) |
| Hyper-V / 2e PC | `Get-VM` indisponible ; pas de seconde machine |

**On n’a pas lancé `install-server.ps1` ici** : il installerait des services Windows, prendrait le port 5432, et écrirait dans `C:\Program Files\GestPOV` et `C:\ProgramData\GestPOV` sur le PC de développement.

---

## 5–7. Sandbox fonctionnelle (remplace partiellement l’install Windows)

Protocole exécuté :

1. `initdb` + `pg_ctl start` sur **55432**, `listen_addresses = 127.0.0.1`
2. Rôle `gest_pov_app` + base `gest_pov`
3. JAR du package + `runtime\bin\java.exe` + profils `prod,desktop` + port **18080**
4. Licence enforcement **désactivée** pour ce sandbox (`--app.license.enforcement-enabled=false`)

Résultats :

- Liveness `GET /actuator/health/liveness` → `{"status":"UP"}`
- Flyway v1…v20
- `serverId` persisté
- UDP listener port 38471
- Bootstrap `AdminBootstrapService` : `admin@gestpov.local` (compte **prod**, pas `admin@erp.local`)
- Login + `/api/auth/me`

**Licence (VM réelle) :** `application.yml` d’install met `enforcement-enabled: true`. Login et discovery restent exempts ; **`GET /api/auth/me` exigera une licence valide** (`LICENSE_REQUIRED` sinon). Sur VM, importer un `.lic` (ou désactiver temporairement l’enforcement) avant de valider `/auth/me`.

Processus sandbox **arrêtés** en fin de session (`pg_ctl stop`, Java tué). Rien n’écoute plus 18080/55432.

---

## 8–11. Réseau, 2e PC, changement d’IP, reboot

Non exécutés (pas de 2e machine, pas de reboot, pas de service Automatic).

Preuves locales seulement :

- PostgreSQL sandbox lié à `127.0.0.1`
- API sandbox liée à `0.0.0.0:18080` (accessible LAN **si** le firewall Windows l’autorise — non testé)
- UDP 38471 en écoute `0.0.0.0`

---

## 12–17. Idempotence, backup script, uninstall, purge

Non exécutés (installeur Windows).

Revue + correctif **PurgeData** : `-PurgeData` exige maintenant de taper `PURGE` (ou `-ConfirmPhrase PURGE`). Impossible de purger `C:\ProgramData\GestPOV` par un simple oubli du switch.

Sandbox dump/restore (binaires du package, pas les scripts de service) : **OK** (voir tableau).

---

## 18. Secrets

| Contrôle | Résultat |
|----------|----------|
| Tests `Test-GestPovSecrets.ps1` (DPAPI CurrentUser) | PASS |
| Mot de passe dans `server.log` sandbox | Aucun |
| WinSW XML | Généré à l’install ; template sans mot de passe (env via `start-backend.ps1` + DPAPI) |
| README package | Pas de secret |
| Git | `desktop/server-package/build/` et `vendor/*.zip` gitignorés |
| Desktop JavaFX | Discovery JSON sans secret (vérifié : `application`, `serverId`, `version`, `status`, `port`) |
| DPAPI après reboot / ACL `secrets.dpapi` | NOT_EXECUTED (fichier non créé sous ProgramData) |
| `INITIAL_ADMIN.txt` | Contient le mot de passe bootstrap **en clair une fois** (voulu) ; ACL admin prévue à l’install |

Limite déjà documentée Phase 4 : DPAPI `LocalMachine` est lisible par tout administrateur local.

---

## 19. Dépendance Internet (scripts)

**Scripts d’installation client** (`desktop/scripts/server/**` et copie dans le package) :

- `Invoke-WebRequest` uniquement vers `http://127.0.0.1:...` (health / liveness / discovery)
- Aucun `curl` / `wget` / `winget` / `choco` / `Install-Package` / `DownloadFile`

**Scripts de BUILD éditeur** (`desktop/scripts/build/build-offline-package.ps1`) :

- `curl.exe` / `Invoke-WebRequest` pour PostgreSQL zip + WinSW — **autorisé**, jamais copié comme étape client

Offline **pendant `install-server.ps1`** sur VM sans NIC : NOT_EXECUTED.

---

## 20. Corrections appliquées (Web/Docker inchangés)

Aucun fichier `backend/` Java, `frontend/`, ni Docker modifié.

Fichiers touchés :

- `desktop/scripts/build/build-offline-package.ps1`
- `desktop/scripts/server/uninstall-server.ps1` (confirmation PURGE)
- Package généré sous `desktop/server-package/build/` (gitignoré)
- Cache éditeur `desktop/server-package/vendor/` (gitignoré)

`mvn -f backend test` et `mvn -f desktop/client test` **non relancés** : pas de changement Java. À relancer si une correction backend apparaît en VM.

---

## Procédure VM (à exécuter tel quel — reste bloquant pour clôturer 100 % des lignes)

Machine : Windows 10/11 64-bit **propre**, admin local, Internet **coupé**, sans Java / PostgreSQL / Maven / Git / Docker / Node / Gest POV.

1. Copier uniquement `GestPOV-Server-Offline/` (USB).
2. PowerShell **administrateur** :

```powershell
Set-ExecutionPolicy -Scope Process Bypass
cd <copie>\GestPOV-Server-Offline
.\install-server.ps1
.\health-check.ps1
Get-Service GestPOV-PostgreSQL, GestPOV-Server
```

3. Vérifier chaque `[OK]` listé Phase 4.5 §5 (répertoires, initdb, services Automatic, DB `gest_pov`, DPAPI, `server.id`, JAR, firewall, Flyway, `/api/discovery`).
4. Login avec `C:\ProgramData\GestPOV\config\INITIAL_ADMIN.txt`, puis `GET /api/auth/me` (licence requise si enforcement ON).
5. Depuis un **2e PC** du LAN : UDP `GEST_POV_DISCOVERY` → HTTP discovery → enregistrement Desktop ; `IP:5432` refusé ; `IP:8080` OK.
6. Simuler ancienne IP → rediscovery même `serverId`.
7. **Reboot** serveur → services Running sans terminal → `.\health-check.ps1`.
8. Relancer `.\install-server.ps1` (idempotence).
9. Créer des données → `.\scripts\server\backup-server.ps1` → vérifier `postgres.dump`, `manifest.json`, licence, uploads.
10. Restore destructif (`restore-server.ps1`) **sur cette VM seulement**.
11. `.\uninstall-server.ps1` (ProgramData conservé) → réinstall.
12. **VM seulement** : `.\uninstall-server.ps1 -PurgeData` puis taper `PURGE`.

---

## Critère de sortie Phase 4.5

Nous savons précisément le statut de chaque test (tableau ci-dessus).

**Phase 4.5 n’est pas « production-ready » tant que la colonne VM (services, reboot, LAN, backup scripts, uninstall) reste NOT_EXECUTED.**  
Le package USB existe et le runtime embarqué a été prouvé.

**Ne pas enchaîner sur la Phase 5** tant que la procédure VM n’a pas été jouée ou explicitement reportée.
