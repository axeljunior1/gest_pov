# Ajouts backend minimaux requis (Desktop)

Modifications **additives** — Docker/prod inchangés si le profil `desktop` n'est pas activé.

UDP discovery **désactivée par défaut** ; activée seulement en `dev` et `desktop`.

---

## 1. GET /api/discovery — FAIT (Phase 3)

Route publique, exemptée licence, aucune donnée sensible.

Fichiers :

- `backend/src/main/java/com/erp/products/controller/DiscoveryController.java`
- `backend/src/main/java/com/erp/products/discovery/ServerDiscoveryService.java`
- `backend/src/main/java/com/erp/products/discovery/ServerIdentityService.java`
- `backend/src/main/java/com/erp/products/discovery/UdpDiscoveryListener.java`

**Sécurité :**

- `SecurityConfig` : `/api/discovery` permitAll
- `LicenseEnforcementFilter` : exempt `/api/discovery`

---

## 2. Profil application-desktop.yml — FAIT

Activation : `SPRING_PROFILES_ACTIVE=prod,desktop`  
Docker : profil `desktop` **non** utilisé.

---

## 3. Listener UDP — FAIT

`@ConditionalOnProperty(app.desktop.discovery.enabled=true)`

---

## 4. Health enrichi — futur

---

## Stockage serverId

Voir `desktop/docs/phase-3.md`.
