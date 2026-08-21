# Installation Gest POV Desktop (serveur + clients)

Ce guide explique **où trouver les fichiers** et **comment installer** sur Windows.

> Les « installateurs » livrés aujourd’hui sont des **dossiers offline** prêts à copier sur clé USB  
> (pas encore de `Setup.exe` — roadmap dans `docs/phase-h.md`).

---

## 1. Où sont les fichiers utiles ?

### Sur ce PC développeur (après construction)

| Emplacement | Contenu |
|-------------|---------|
| **`desktop/dist/`** | **Point d’entrée** : packages prêts à copier + `LIRE-MOI.txt` |
| `desktop/dist/GestPOV-Server-Offline/` | Installateur **serveur** |
| `desktop/dist/GestPOV-Client-Offline/` | Installateur **client** (caisse) |
| `desktop/construire-installateurs.bat` | **Double-clic** pour (re)construire les deux packages |
| `desktop/INSTALLATION.md` | Ce guide |

Copies techniques (mêmes contenus avant copie vers `dist/`) :

- `desktop/server-package/build/GestPOV-Server-Offline/`
- `desktop/client-package/build/GestPOV-Client-Offline/`

### Dans chaque package

**Serveur** (`GestPOV-Server-Offline`) — **seulement 2 lanceurs** :

| Fichier | Rôle |
|---------|------|
| `01-Installer.cmd` | **1ʳᵉ installation** (UAC admin) |
| `02-Menu.cmd` | Démarrer / arrêter / diagnostic / réparer / désinstaller |
| `LIRE-MOI.txt` | Rappel |

La logique est en **PowerShell** (`scripts\server\ui\`). Les `.cmd` sont de minuscules lanceurs ASCII générés au build (fiables sous `cmd.exe`).

Ne pas ouvrir le dossier `scripts\` à la main.

**Client** (`GestPOV-Client-Offline`) — **3 lanceurs** :

| Fichier | Rôle |
|---------|------|
| `01-Installer.cmd` | Installation (utilisateur courant) |
| `02-Menu.cmd` | Installer / lancer |
| `GestPOV-Client.cmd` | Lancer sans installer (depuis USB) |
| `LIRE-MOI.txt` | Rappel |

Même principe que le serveur : logique en PowerShell, `.cmd` ASCII générés au build.

---

## 2. Construire les packages (PC éditeur, avec Internet)

1. Double-cliquez :  
   **`desktop\construire-installateurs.bat`**
2. Attendez la fin (plusieurs minutes).
3. Le dossier **`desktop\dist\`** s’ouvre automatiquement.
4. Copiez **tout** `GestPOV-Server-Offline` et `GestPOV-Client-Offline` sur une **clé USB**.

En PowerShell :

```powershell
cd "chemin\vers\cursor projet"
powershell -ExecutionPolicy Bypass -File desktop\scripts\build\build-all-installers.ps1 -SkipTests
```

Options :

```powershell
# Serveur seulement
...\build-all-installers.ps1 -SkipTests -ServerOnly

# Client seulement
...\build-all-installers.ps1 -SkipTests -ClientOnly
```

Prérequis éditeur : **JDK 17+**, **Maven 3.8+**, Internet (téléchargement PostgreSQL / WinSW la 1ʳᵉ fois).

---

## 3. Installer le SERVEUR (1 PC boutique)

1. Sur le PC serveur : Windows 10/11 64 bits, compte **Administrateur**.
2. Copiez le dossier `GestPOV-Server-Offline` depuis la USB (ex. `C:\Install\GestPOV-Server-Offline`).
3. Double-clic : **`01-Installer.cmd`** (accepte l’UAC).
4. Double-clic : **`02-Menu.cmd`** → choix **4) Diagnostic**.
5. Firewall Windows : autoriser **TCP 8080** (API) et **UDP 38471** (découverte caisses).

Si le serveur ne démarre pas : `02-Menu.cmd` → **5) Réparer démarrage**.  
Si erreur base de données : `02-Menu.cmd` → **6) Resync mot de passe**.

### Compte admin initial

Fichier généré sur le serveur :

`C:\ProgramData\GestPOV\config\INITIAL_ADMIN.txt`

(À lire une fois, puis changer le mot de passe.)

### Licence

Importer la licence **sur le serveur** (pas sur le client), via l’UI Paramètres / Licence ou le script `import-license.bat` du dépôt.

---

## 4. Installer un CLIENT (chaque caisse)

Préalable : le **serveur tourne** sur le réseau local.

1. Copiez `GestPOV-Client-Offline` sur le PC caisse.
2. Double-clic : **`01-Installer.cmd`**  
   (utilisateur normal suffit ; pour tous les profils : `02-Menu.cmd` → option 2, admin).
3. Lancez **Gest POV** depuis le menu Démarrer, ou `GestPOV-Client.cmd` / `02-Menu.cmd` → 3.
4. Au 1ʳᵉ lancement : découverte du serveur (UDP) ou saisie IP + port `8080`.
5. Connexion avec un utilisateur créé sur le serveur.

Config locale client : `%APPDATA%\GestPOV\client.properties`

---

## 5. Ordre recommandé en boutique

```
1. Construire les packages (PC éditeur)
2. Installer + démarrer le SERVEUR
3. Vérifier health-check + INITIAL_ADMIN.txt
4. Installer les CLIENTS
5. Ouvrir une caisse, se connecter, tester une vente
```

---

## 6. Dépannage rapide

| Problème | Action |
|----------|--------|
| `construire-installateurs` échoue | Vérifier `JAVA_HOME` (JDK 17), `mvn -v`, Internet |
| Client ne trouve pas le serveur | Firewall TCP 8080 / UDP 38471 ; même réseau Wi‑Fi/LAN |
| Catalogue POS vide | Produits en cycle **Actif** ; redémarrer serveur après mise à jour |
| Mot de passe DB | `02-Menu.cmd` → option **6** |

---

## 7. Setup.exe (futur)

Les `.exe` type `GestPOV-Server-Setup.exe` / `GestPOV-Client-Setup.exe` sont prévus (jpackage + WiX) — voir `docs/phase-h.md`.  
**Aujourd’hui**, utilisez les dossiers `GestPOV-*-Offline` décrits ci-dessus : c’est le mode d’installation supporté.
