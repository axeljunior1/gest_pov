# Phase H — Packaging / ops

**Statut :** documenté + scaffolding. Packages **dossier** USB déjà livrés (Phase 4 / 10). Installateurs `.exe`, validation VM LAN complète et backlog MFA / reset / ESC-POS = **TODO**.

---

## 1. Roadmap installateurs `.exe` (jpackage / WiX)

Les builds éditeur actuels produisent des **dossiers offline**, pas des Setup.exe :

| Artefact actuel | Script |
|-----------------|--------|
| `GestPOV-Server-Offline/` | [`desktop/scripts/build/build-offline-package.ps1`](../scripts/build/build-offline-package.ps1) |
| `GestPOV-Client-Offline/` | [`desktop/scripts/build/build-client-package.ps1`](../scripts/build/build-client-package.ps1) |

### Cible (non livré)

| Fichier | Rôle |
|---------|------|
| `GestPOV-Server-Setup.exe` | PostgreSQL + backend + services WinSW + runtime (contenu du package serveur) |
| `GestPOV-Client-Setup.exe` | Desktop JavaFX + JRE embarqué |
| `GestPOV-Update-X.Y.Z.exe` | Upgrade offline versionné (plus tard) |

### Étapes proposées

1. Continuer à bâtir les dossiers via les scripts ci-dessus (source de vérité USB).
2. Sur machine éditeur : JDK 17+ avec `jpackage`, **WiX Toolset** (requis sous Windows pour le type `exe`).
3. Emballer le client (puis le serveur) avec `jpackage --type exe` — voir le stub documentaire [`build-exe-stub.ps1`](../scripts/build/build-exe-stub.ps1) (ne remplace pas les packages dossier).
4. Signer le code (certificat éditeur — futur).
5. Valider sur VM offline (checklist ci-dessous).

Détail pipeline / prérequis : [build-release.md](build-release.md).  
Installateurs skeleton : [desktop/installer/README.md](../installer/README.md).

---

## 2. Validation VM / LAN

Checklist et résultats connus (PC dév, pas VM vierge) :

→ **[phase-4-validation.md](phase-4-validation.md)**

Protocole VM Windows propre (Internet coupé) :

→ **[offline-vm-test.md](offline-vm-test.md)**

| Scénario | État |
|----------|------|
| Build package offline | PASS (voir phase-4-validation) |
| `install-server.ps1` admin + services + reboot | NOT_EXECUTED |
| 2e PC / UDP discovery LAN | NOT_EXECUTED |
| Client `.exe` + serveur `.exe` sur VM | TODO (dépend jpackage) |

---

## 3. Backlog explicite (hors scope code Phase H immédiat)

| Sujet | Notes | Statut |
|-------|-------|--------|
| **MFA** | Auth multi-facteur (web + Desktop) | TODO backlog |
| **Reset mot de passe e-mail** | Flux oubli / reset via mail (backend + UI) | TODO backlog |
| **Imprimante ESC-POS native** | Tickets POS via pilote/port ESC-POS (au-delà impression système / PDF) | TODO backlog |

Ces trois points restent hors industrialisation install ; les documenter ici évite de les confondre avec le packaging `.exe`.

---

## Liens utiles

- Packages USB : [installation-server.md](installation-server.md), [installation-client.md](installation-client.md)
- Matrice migration : [migration-status.md](migration-status.md)
- Polish Phase 10 : [phase-10.md](phase-10.md)
