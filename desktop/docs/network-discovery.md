# Découverte réseau LAN — Gest POV Desktop

---

## Objectif

Permettre aux postes clients de trouver le serveur Gest POV **sans saisie manuelle d'IP**.

---

## Protocole UDP (proposé)

| Paramètre | Valeur défaut |
|-----------|---------------|
| Port UDP | `38471` |
| Message client | `GEST_POV_DISCOVERY` (UTF-8) |
| Broadcast | `255.255.255.255` + interface locale |

### Réponse serveur (JSON)

```json
{
  "application": "GEST_POV",
  "serverId": "550e8400-e29b-41d4-a716-446655440000",
  "serverName": "CAISSE-PRINCIPALE",
  "host": "192.168.1.50",
  "port": 8080,
  "version": "1.0.0",
  "status": "READY"
}
```

Le serveur **ne répond** que si le service backend est UP et discovery activée.

---

## Validation HTTP obligatoire

Après réception UDP, le client **doit** appeler :

```
GET http://<host>:<port>/api/discovery
```

Réponse attendue (même schéma JSON).  
Si HTTP échoue ou `application != GEST_POV` → serveur ignoré.

> Un port ouvert ne suffit **jamais** à identifier Gest POV.

---

## Algorithme client (démarrage)

```
1. Si client.properties contient server.id + host + port :
     a. GET /api/discovery
     b. Si OK et serverId match → connecter
     c. Si IP change mais serverId OK → mettre à jour host
2. Sinon hostname connu (optionnel futur)
3. Sinon UDP discovery (timeout 3s, retry 2x)
4. Si plusieurs réponses → liste choix utilisateur
5. Si aucune → message "Serveur introuvable" + saisie manuelle secours (option admin)
6. Enregistrer config + login
```

---

## serverId

- Généré **une fois** à l'installation serveur (UUID v4)
- Fichier : `C:\ProgramData\GestPOV\config\server.id`
- Stable après redémarrage, changement IP, redémarrage routeur
- Le client mémorise le couple `(serverId, host, port)`

---

## Firewall serveur

| Règle | Profil |
|-------|--------|
| UDP 38471 entrant | Privé |
| TCP 8080 entrant | Privé |

Domaine public : **aucune** règle par défaut.

---

## Implémentation prévue

| Composant | Emplacement |
|-----------|-------------|
| Listener UDP serveur | Module Java dans backend (profil `desktop`) ou sidecar |
| Client discovery | `desktop/client/src/.../discovery/` |
| Endpoint REST | `GET /api/discovery` — voir `backend-additions-required.md` |

---

## Tests requis

- Serveur absent → message clair
- Serveur retrouvé après changement IP
- Mauvais serveur (autre app sur port 8080) → rejeté
- Plusieurs serveurs Gest POV → choix utilisateur
- Version incompatible → `UPDATE_REQUIRED`
