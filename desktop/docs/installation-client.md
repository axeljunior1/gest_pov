# Installation client Desktop — Gest POV

**Livrable actuel :** dossier `GestPOV-Client-Offline` (Phase 10).  
**Pas encore produit :** `GestPOV-Client-Setup.exe`.

---

## Rôle du poste client

Poste caisse / bureau **sans** PostgreSQL ni Spring Boot.

Contient :

- Gest POV Desktop (JavaFX)
- Runtime Java embarqué + JAR JavaFX Windows
- Configuration locale (`%APPDATA%\GestPOV\`)

---

## Prérequis

| Élément | Requis |
|---------|--------|
| Windows 10/11 64 bits | Oui |
| Internet | **Non** (build éditeur seulement) |
| Serveur Gest POV sur le LAN | Oui |
| Droits admin | Non, sauf `install-client.ps1 -AllUsers` |

---

## Build éditeur (avec Internet)

```powershell
powershell -ExecutionPolicy Bypass -File desktop\scripts\build\build-client-package.ps1
# Option : -SkipTests
```

Sortie : `desktop\client-package\build\GestPOV-Client-Offline\`

## Installer sur un poste (offline)

```text
1. Copier GestPOV-Client-Offline sur le PC
2. Double-clic : 01-Installer.cmd
   (ou 02-Menu.cmd)
3. Lancer : raccourci menu Demarrer, ou GestPOV-Client.cmd
```

Tous les utilisateurs (Program Files, **admin**) : `02-Menu.cmd` → option 2.
---

## Premier lancement — découverte serveur

```
1. Lire %APPDATA%\GestPOV\client.properties (vide au 1er run)
2. Tester dernier serveur connu (si existant)
3. Broadcast UDP GEST_POV_DISCOVERY
4. Lister serveurs trouvés (serverName, serverId, version)
5. Utilisateur sélectionne / confirme (ou saisie IP + port)
6. GET http://<host>:<port>/api/discovery → validation
7. Vérifier compatibilité version
8. Enregistrer server.id, server.host, server.port
9. Écran login → POST /api/auth/login
10. Application prête
```

Compte : **pas** `admin@erp.local` en production. Lire `C:\ProgramData\GestPOV\config\INITIAL_ADMIN.txt` **sur le serveur**.

---

## Configuration utilisateur

Fichier `%APPDATA%\GestPOV\client.properties` :

```properties
server.id=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
server.host=192.168.1.50
server.port=8080
server.name=CAISSE-SERVEUR
```

**Changement d'IP serveur :** relancer discovery → même `serverId` retrouvé → mise à jour automatique de `server.host`.

---

## Dépannage

| Problème | Action |
|----------|--------|
| Aucun serveur trouvé | Vérifier LAN, firewall **serveur** TCP 8080 + UDP 38471, service GestPOV-Server |
| Version incompatible | Mettre à jour client ou serveur (packages USB) |
| Login refusé | Identifiants Gest POV (fichier INITIAL_ADMIN ou utilisateur créé) |
| Licence invalide | Importer `.lic` sur le **serveur** |
| JavaFX / écran noir | Relancer le build client (`-Djavafx.platform=win`) |

---

## État actuel

Package dossier : **oui**. Installateur `.exe` : **non**.
