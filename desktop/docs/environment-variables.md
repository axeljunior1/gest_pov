# Variables d'environnement — Web vs Desktop

Ce document clarifie **qui connaît quoi** entre l'édition Web/Docker et l'édition Desktop.

---

## Règle fondamentale

| Secret / config | Web/Docker | Desktop client | Desktop serveur |
|-----------------|------------|----------------|-----------------|
| `SPRING_DATASOURCE_*` | ✅ Backend | ❌ **Jamais** | ✅ Service backend |
| `APP_JWT_SECRET` | ✅ Backend | ❌ | ✅ Service backend |
| `APP_BOOTSTRAP_ADMIN_*` | ✅ Install Docker | ❌ | ✅ Install serveur |
| `server.host` / `server.port` | ❌ | ✅ Fichier client | ❌ |
| `server.id` | ❌ | ✅ Fichier client | ✅ Fichier serveur |
| Token JWT utilisateur | ❌ (navigateur) | ✅ Mémoire client | ❌ |

---

## Web / Docker — fichier `.env`

Modèle documenté : **`.env.example`** à la racine du dépôt.

```bash
# Copier puis éditer (ne jamais committer .env)
cp .env.example .env
```

| Variable | Obligatoire client | Description |
|----------|-------------------|-------------|
| `POSTGRES_DB` | Oui | Nom base PostgreSQL |
| `POSTGRES_USER` | Oui | Utilisateur PostgreSQL |
| `POSTGRES_PASSWORD` | Oui | Mot de passe fort (générer) |
| `SPRING_PROFILES_ACTIVE` | Oui | `prod,docker` en client |
| `APP_JWT_SECRET` | Oui | Min. 32 caractères aléatoires |
| `APP_BOOTSTRAP_ADMIN_EMAIL` | Oui (1er install) | Email admin initial |
| `APP_BOOTSTRAP_ADMIN_PASSWORD` | Oui (1er install) | Mot de passe admin initial |
| `APP_BIND` | Non | IP écoute proxy (défaut `127.0.0.1`) |
| `APP_PORT` | Non | Port proxy (défaut `80`) |

Voir aussi [`docs/lancement-dev.md`](../../docs/lancement-dev.md) pour le **mode dev local** (sans `.env` obligatoire).

---

## Desktop serveur — variables (installateur)

Générées **automatiquement** à l'installation — pas saisies par l'utilisateur final sauf admin Gest POV.

| Variable / fichier | Génération | Stockage proposé |
|--------------------|------------|------------------|
| Mot de passe PostgreSQL | Aléatoire install | DPAPI / secret service |
| `APP_JWT_SECRET` | Aléatoire install | `%ProgramData%\GestPOV\config\` |
| `serverId` (UUID) | Install serveur | `%ProgramData%\GestPOV\config\server.id` |
| Chemins data | Fixés install | `%ProgramData%\GestPOV\data\` |

Template : `desktop/server-package/config/application-desktop-server.yml.template`

---

## Desktop client — configuration fichier

Fichier `%APPDATA%\GestPOV\client.properties` :

```properties
# --- Connexion serveur (PAS de secrets DB) ---
server.id=
server.host=
server.port=8080
server.name=

# --- Session ---
# Le token JWT est stocké en mémoire / keystore OS, pas dans ce fichier en clair

# --- Version ---
client.version=1.0.0
```

Template : `desktop/client/config/client.properties.example`

---

## Profils Spring Desktop (serveur)

Proposition — profil **`desktop`** additionnel (n'impacte pas `docker`) :

```yaml
# backend/src/main/resources/application-desktop.yml (futur)
spring:
  config:
    activate:
      on-profile: desktop

app:
  desktop:
    discovery-enabled: true
    discovery-udp-port: 38471
```

Activation : `SPRING_PROFILES_ACTIVE=prod,desktop`

---

## Checklist sécurité

- [ ] Desktop client ne contient jamais `db.password`
- [ ] PostgreSQL écoute localhost sur serveur
- [ ] JWT secret identique entre redémarrages serveur
- [ ] `.env` / secrets exclus de Git
- [ ] Logs sans mots de passe ni tokens
