# Checklist activation licence client — Gest_POV

## Contexte

- Environnement : Docker client (`docker-compose.client.yml`, profil `prod,docker`)
- Fichier retour attendu : `gest_pov.lic`

## Récupérer l'installation ID (côté client)

```bash
curl -s http://127.0.0.1/api/license/installation-id
# ou
docker exec gest-pov-backend cat /app/gest-pov-data/installation.id
```

Conserver cet UUID pour la demande éditeur — **ne pas réutiliser un ID issu d'un smoke test dev**.

## Informations à envoyer à l'éditeur

- Installation ID : `<UUID depuis API ou volume>`
- Produit attendu (`app`) : `gest_pov`
- Nom client : `<A_COMPLETER_PAR_LE_CLIENT>`
- Site client : `<A_COMPLETER>`
- Edition / plan : `<STANDARD|PRO|ENTERPRISE>`
- Date d'expiration souhaitée : `<YYYY-MM-DD>`
- Limite utilisateurs (`maxUsers`) : `<NOMBRE>`

## Attendu côté éditeur (hors projet client)

1. Générer la licence avec la **clé privée production** (jamais livrée au client).
2. Signer la payload avec RSA SHA256.
3. Vérifier que `installationId` correspond exactement.
4. Renvoyer un fichier nommé `gest_pov.lic`.
5. Transmettre le fichier via canal sécurisé (pas Git).

## Procédure côté client à réception de `gest_pov.lic`

```bash
curl -X POST http://127.0.0.1/api/license/import \
  -F "file=@gest_pov.lic"

curl -s http://127.0.0.1/api/license/status
```

Résultat attendu : `valid=true`, `activated=true`.

## Validation fonctionnelle après activation

1. Login admin bootstrap (`/api/auth/login`) OK.
2. `GET /api/products` avec token admin retourne `200` (au lieu de `403`).
3. `docker compose -f docker-compose.client.yml --env-file .env restart`.
4. `GET /api/license/status` reste `valid=true`.
5. `./scripts/client-backup.sh` inclut volume licence.

## Si la licence n'est pas encore disponible

- Statut attendu : `LICENSE_MISSING`.
- API métier reste bloquée : `403 LICENSE_REQUIRED`.
- Aucune action de contournement n'est autorisée.

Voir aussi : `docs/license-production.md`, `docs/release-readiness-v1.md`.
