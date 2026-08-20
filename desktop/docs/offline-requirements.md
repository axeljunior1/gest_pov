# Exigences fonctionnement 100 % offline

Contrainte majeure : installation et exploitation **sans Internet** chez le client.

---

## Au moment de l'installation (VM vierge)

Le package éditeur sur clé USB doit contenir **tout** :

| Composant | Server Setup | Client Setup |
|-----------|--------------|--------------|
| JRE embarqué | ✅ | ✅ |
| PostgreSQL embarqué | ✅ | ❌ |
| JAR Spring Boot | ✅ | ❌ |
| Flyway migrations | ✅ (dans JAR) | ❌ |
| Binaire Desktop JavaFX | ✅ | ✅ |
| Clé publique licence | ✅ (dans JAR) | ❌ (via API) |

**Interdit pendant install client :** téléchargement npm, Maven, Docker pull, Windows Update composants.

---

## Audit dépendances Internet — projet actuel

### Backend ✅ Offline

- Aucun appel HTTP sortant runtime
- Licence : fichier local signé RSA
- PostgreSQL : local

### Frontend Web ⚠️ (hors scope Desktop JavaFX)

| Dépendance | Fichier | Risque offline |
|------------|---------|----------------|
| Google Fonts CDN | `frontend/index.html` | Écran blanc fonts si offline |
| npm packages | Build time only | N/A runtime Web Docker (bundlé) |

### Desktop JavaFX ✅ (cible)

- Polices système Windows ou fonts embarquées dans JAR
- Pas de CDN
- API REST vers serveur LAN uniquement

---

## Build éditeur (avec Internet)

Chez l'éditeur, le build **peut** utiliser Internet :

- `mvn package` backend
- `mvn package` desktop/client
- Téléchargement PostgreSQL Windows embed
- `jpackage` installateurs

Le **résultat** (`.exe` sur clé USB) doit être autonome.

---

## Décision export PDF (Sprint 7 Web — rappel)

| Option | Statut |
|--------|--------|
| Impression navigateur → PDF | Disponible Web |
| Génération PDF serveur | **Non implémenté** — futur |
| Desktop ticket | Impression JavaFX / PDF futur |

Voir `docs/tickets-factures-v1.md` (Web) quand créé.

---

## Checklist validation offline (Phase 7)

VM Windows vierge :

- [ ] Internet désactivé
- [ ] Java absent
- [ ] PostgreSQL absent
- [ ] `GestPOV-Server-Setup.exe` → succès
- [ ] Import licence USB
- [ ] Vente POS complète
- [ ] VM2 + `GestPOV-Client-Setup.exe` → discovery → login → POS

---

## Éléments à surveiller lors du développement

- Ne pas ajouter de telemetry cloud
- Ne pas ajouter d'activation licence online obligatoire
- Ne pas référencer de CDN dans le client JavaFX
- Documenter toute nouvelle dépendance réseau dans ce fichier
