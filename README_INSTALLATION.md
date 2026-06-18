# Gest_POV — Installation locale Docker (client final)

Ce package permet d'installer Gest_POV **sans code source** : uniquement des images Docker pré-buildées.

## Prérequis

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (Windows / macOS) ou Docker Engine (Linux)
- Docker Compose v2 (`docker compose`)
- Environ **4 Go de RAM** libres pour les conteneurs

## Contenu du package client

```
.
├── images/
│   ├── monapp-backend-1.0.0.tar
│   ├── monapp-frontend-1.0.0.tar
│   └── postgres.tar
├── docker-compose.yml
└── .env.example
```

## Installation (première fois)

1. Copiez la configuration :
   ```bash
   cp .env.example .env
   ```

2. Éditez `.env` et changez au minimum :
   - `POSTGRES_PASSWORD`
   - `SPRING_DATASOURCE_PASSWORD` (identique à `POSTGRES_PASSWORD`)
   - `APP_JWT_SECRET` (chaîne longue et aléatoire)

3. Chargez les images et démarrez :
   ```bash
   docker load -i images/postgres.tar
   docker load -i images/monapp-backend-1.0.0.tar
   docker load -i images/monapp-frontend-1.0.0.tar
   docker compose --env-file .env up -d
   ```

## Utilisation

| Action | Commande |
|--------|----------|
| Démarrer | `docker compose --env-file .env up -d` |
| Arrêter | `docker compose --env-file .env down` |

## Accès application

| Service | URL |
|---------|-----|
| **Interface web (frontend)** | http://localhost |
| **API backend** (direct) | http://localhost:8080 |
| **Santé backend** | http://localhost:8080/actuator/health |

> Si le port 80 est occupé, modifiez `FRONTEND_PORT=8081` dans `.env` puis accédez à http://localhost:8081

## Licence Gest_POV

Au premier lancement, l'écran d'activation s'affiche. Copiez l'**identifiant machine**, transmettez-le à votre éditeur pour obtenir le fichier `.lic`, puis importez-le.

## Données persistées

Les volumes Docker conservent :

- Base PostgreSQL (`monapp_pgdata`)
- Fichiers uploadés (`monapp_uploads`)
- Données licence / installation (`monapp_license`)

`docker compose down` n'efface pas ces données.

## Dépannage

```bash
# État des conteneurs
docker compose ps

# Logs backend
docker logs -f monapp-backend

# Logs frontend
docker logs -f monapp-frontend
```

---

## Côté éditeur (build des images — non livré au client)

Depuis le dépôt source complet :

```bash
docker build -t monapp-backend:1.0.0 -f backend/Dockerfile backend
docker build -t monapp-frontend:1.0.0 -f frontend/Dockerfile frontend
docker save monapp-backend:1.0.0 -o images/monapp-backend-1.0.0.tar
docker save monapp-frontend:1.0.0 -o images/monapp-frontend-1.0.0.tar
```

Le client reçoit uniquement le dossier `images/` + scripts + `docker-compose.yml` + `.env.example`.
