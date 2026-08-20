# Build et release — installateurs Desktop

---

## Artefacts cibles

| Fichier | Contenu |
|---------|---------|
| `GestPOV-Server-Setup.exe` | PostgreSQL + backend + services + Desktop |
| `GestPOV-Client-Setup.exe` | Desktop + JRE embarqué |
| `GestPOV-Update-X.Y.Z.exe` | Mise à jour offline (futur) |

---

## Pipeline build éditeur (proposé)

```
1. mvn -f backend/pom.xml package -DskipTests
   → backend/target/gest-pov-backend.jar

2. mvn -f desktop/client/pom.xml package
   → desktop/client/target/gest-pov-desktop.jar

3. jpackage (Server)
   --input backend/target
   --main-jar gest-pov-backend.jar
   + PostgreSQL zip embarqué
   + scripts service Windows

4. jpackage (Client)
   --input desktop/client/target
   --main-jar gest-pov-desktop.jar
   --main-class com.gestpov.desktop.GestPovDesktopApp

5. Signer code (certificat éditeur — futur)

6. Tests VM offline

7. Publier sur clé USB / portail éditeur
```

---

## Versions

| Composant | Source version |
|-----------|----------------|
| Backend | `pom.xml` + `info.app.version` |
| Desktop client | `desktop/client/pom.xml` |
| Compatibilité | Matrice semver dans release notes |

---

## Matrice compatibilité (exemple)

| Client \ Serveur | 1.0.x | 1.1.x |
|------------------|-------|-------|
| 1.0.x | COMPATIBLE | UPDATE_REQUIRED |
| 1.1.x | SERVER_TOO_OLD | COMPATIBLE |

---

## Prérequis build machine éditeur

- JDK 17+
- Maven 3.9+
- WiX Toolset (jpackage Windows)
- Accès Internet (build only)

---

## État actuel

- Skeleton Maven client : `desktop/client/pom.xml`
- Package serveur dossier : `GestPOV-Server-Offline` (`build-offline-package.ps1`)
- Package client dossier : `GestPOV-Client-Offline` (`build-client-package.ps1`)
- Installateurs `.exe` : **non produits** — roadmap Phase H [`phase-h.md`](phase-h.md)
- Stub jpackage (doc / sortie soft si outils absents) : `desktop/scripts/build/build-exe-stub.ps1`

---

## Git — ce qui est versionné

```
desktop/          ✅ code + docs + templates
backend/          ✅ inchangé sauf ajouts discovery documentés
*.exe             ❌ jamais dans Git
secrets/          ❌ jamais dans Git
```
