# Gest_POV Client v1.0.0-rc1

Package léger pour déploiement via Docker Hub public.

## Contenu

- `docker-compose.hub.yml`
- `.env.example`
- `scripts/client-*.sh`
- `docs/`

## Déploiement rapide

```bash
cp .env.example .env
# renseigner les secrets réels client

docker compose -f docker-compose.hub.yml --env-file .env pull
docker compose -f docker-compose.hub.yml --env-file .env up -d
docker compose -f docker-compose.hub.yml --env-file .env ps
```

## Images attendues

- `axelmengue1/gest-pov-backend:1.0.0-rc1`
- `axelmengue1/gest-pov-frontend:1.0.0-rc1`

## Sécurité / exclusions

Ne jamais inclure ni versionner :

- `.env`
- `*.lic`
- `backups/`
- clés privées
- tokens Docker
- archives `.tar`
