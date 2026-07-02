# Checklist activation licence client — Gest_POV

## Contexte

- Environnement : Docker client (`docker-compose.client.yml`, profil `prod,docker`)
- Fichier retour attendu : `gest_pov.lic`

> **Ne jamais committer :** `.env`, `gest_pov.lic`, `backups/`, clés privées, archives `images/*.tar`.

---

## Récupérer l'installation ID (côté client)

```bash
curl -s http://127.0.0.1/api/license/installation-id
# ou
docker exec gest-pov-backend cat /app/gest-pov-data/installation.id
```

Conserver cet UUID pour la demande éditeur — **ne pas réutiliser un ID issu d'un smoke test dev**.

## Informations à envoyer à l'éditeur

- Installation ID : `<INSTALLATION_ID_CLIENT>`
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

- [ ] Login admin bootstrap (`/api/auth/login`) ? HTTP 200
- [ ] `GET /api/products` avec token admin ? HTTP 200 (plus de `403 LICENSE_REQUIRED`)
- [ ] `docker compose -f docker-compose.client.yml --env-file .env restart`
- [ ] `GET /api/license/status` reste `valid=true` après restart
- [ ] `GET /api/auth/me` avec token admin ? HTTP 200
- [ ] `installationId` inchangé après restart
- [ ] `./scripts/client-backup.sh` ? `<BACKUP_DIR>/` contient `postgres.dump`, `installation.id`, `gest_pov.lic`

## Si la licence n'est pas encore disponible

- Statut attendu : `LICENSE_MISSING`.
- API métier reste bloquée : `403 LICENSE_REQUIRED`.
- Aucune action de contournement n'est autorisée.

---

## Exemple local non client (poste dev — 2026-07-02)

Validation effectuée sur une installation Docker vierge locale. **Ne pas traiter comme documentation client livrable.**

| Étape | Résultat |
|-------|----------|
| Install vierge + bootstrap | OK |
| Import `gest_pov.lic` éditeur | OK — `valid=true`, `activated=true` |
| `/api/products`, `/api/auth/me` | HTTP 200 |
| Restart stack (`docker compose … restart`) | OK — licence toujours `valid=true`, ID inchangé |
| Backup post-restart | OK — `backups/<timestamp>/` avec `gest_pov.lic` + `installation.id` |

L'`installationId` réel est stocké uniquement dans le volume licence local et les backups gitignored — **ne pas le copier dans le dépôt Git**.

---

Voir aussi : `docs/license-production.md`, `docs/release-readiness-v1.md`, `docs/next-release-steps.md`.
