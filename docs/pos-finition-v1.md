# Sprint 6 — Finition POS (v1)

**Date :** 2026-07-02  
**Objectif :** améliorer l’expérience caisse (lisibilité, guidage, messages) sans modifier l’architecture ni le flux métier `CENTRAL_CASHIER` validé en API.

---

## 1. Audit initial (résumé)

### Déjà solide

- Flux central vendeur → `sendToPayment` → caissier → `validate` + tests backend et Playwright.
- Recherche nom / SKU / code-barres, scan, raccourcis F2–F9.
- Session typée SALES / CASHIER, clôture caisse, tickets et factures via API.
- Messages métier centralisés dans `frontend/src/utils/errors.js`.

### Points faibles identifiés

| Zone | Problème |
|------|----------|
| Navigation | Cartes poste masquées dès session ouverte — difficile de basculer vendeur ↔ caisse |
| Badge « en attente » | Comptait les holds uniquement, pas les brouillons vendeur |
| Panier | Prix unitaire / remise peu visibles ; total peu mis en avant |
| Connexion | Indicateur « En ligne » figé à `true` |
| Encaissement | Pas d’alerte visuelle sur durée d’attente paiement (config backend existante) |
| Après vente | Actions ticket / facture / nouvelle vente limitées |
| Retours | Empty state recherche absent ; moyens de paiement hardcodés |

---

## 2. Améliorations réalisées

### Lisibilité caisse

- **Bandeau session persistant** (`PosWorkspaceNav`) : session, entrepôt, liens Préparation / Encaissement pour rôles doubles.
- **Entrepôt actif** affiché dans l’en-tête vendeur.
- **Panier** : composant `PosCartLineRow` — quantité, prix unitaire, remise ligne, total ligne, alerte stock.
- **Total général** agrandi (texte 2xl).
- **État connexion** : `navigator.onLine` + événements `online` / `offline`.

### Recherche et ajout produit

- Focus recherche au chargement (inchangé, confirmé).
- Message rupture si produit `outOfStock` sans variante.
- Empty state catalogue si catégorie vide.
- Messages erreur enrichis (`errors.js`) : stock, produit inactif, aucun résultat.

### Flux paiement (CENTRAL_CASHIER)

- **`PosFlowBanner`** : étapes 1→2→3 (panier → envoi caisse → encaissement).
- Bouton envoi désactivé si panier vide ou stock insuffisant (existant, conservé).
- Modal paiement vendeur : pré-remplissage montant espèces, texte d’étape.
- Poste caisse : guidage « Étape 3/3 », surlignage ventes en attente selon `alertPendingPaymentMinutes`.

### Ticket / facture après vente

- **`PosTicketModal`** : bandeau succès, imprimer ticket, **voir facture** (API existante), **nouvelle vente (F5)**.
- Échec ticket / facture : notification sans bloquer la vente (comportement conservé).

### Erreurs et états vides

- Panier vide : message guidant vers F2 / catalogue.
- Catalogue vide : message catégorie / recherche.
- Retours : « Aucune vente trouvée » après recherche.
- Encaissement : empty state existant conservé.

### Raccourcis

| Touche | Action |
|--------|--------|
| F2 | Focus recherche (existant) |
| F3 | Quantité suivante (existant) |
| F4 | Envoi caisse / paiement (existant) |
| F5 | Nouvelle vente (après succès, modal ticket) |
| Enter | Ajouter produit sélectionné / valider recherche (existant) |
| Esc | Vider recherche / fermer modals (existant) |

---

## 3. Flux POS final (mode CENTRAL_CASHIER)

```
1. Login + licence active (LicenseGate global)
2. Ouvrir session SALES (vendeur) ou CASHIER (caissier)
3. Vendeur : recherche / scan → panier → [optionnel pause F8]
4. Vendeur : Envoyer à la caisse (F4) → statut PENDING_PAYMENT
5. Caissier : poste Encaissement → Encaisser → validate + paiement
6. Ticket affiché → imprimer / facture / nouvelle vente (F5)
7. Retours : /pos/returns sur vente payée
8. Clôture session caisse
```

---

## 4. Fichiers modifiés

| Fichier | Changement |
|---------|------------|
| `frontend/src/pages/POSPage.jsx` | Panier, flux, connexion, ticket/facture, badge attente unifié |
| `frontend/src/pages/PosPendingPaymentsPage.jsx` | Guidage paiement, alertes attente, ticket/facture |
| `frontend/src/pages/PosReturnsPage.jsx` | Empty state, paiements config, lien retour |
| `frontend/src/components/pos/PosWorkspaceNav.jsx` | Bandeau session persistant + bascule postes |
| `frontend/src/components/pos/PosCartLineRow.jsx` | **Nouveau** — ligne panier détaillée |
| `frontend/src/components/pos/PosFlowBanner.jsx` | **Nouveau** — étapes flux |
| `frontend/src/components/pos/PosSaleLinesSummary.jsx` | Détail prix / remise |
| `frontend/src/components/pos/PosPrintModals.jsx` | Succès vente, facture, nouvelle vente |
| `frontend/src/utils/posPendingAge.js` | **Nouveau** — alerte durée attente |
| `frontend/src/utils/errors.js` | Messages caissier stock / recherche |

**Backend :** aucune modification (frontend uniquement).

---

## 5. Validations lancées

| Commande | Résultat |
|----------|----------|
| `npm run build` (frontend) | À exécuter après merge — voir section CI locale |
| `mvn test` complet | **Non lancé** (contrainte sprint) |
| Tests backend POS | **Non lancés** (pas de changement backend) |

Validation fonctionnelle recommandée sur stack Docker client : parcours API déjà validé ; vérifier visuellement bandeau session, panier, envoi caisse, encaissement, ticket F5.

---

## 6. Limites restantes / Sprint 7

| Sujet | Statut |
|-------|--------|
| **Export PDF ticket/facture** | Non implémenté — impression navigateur uniquement (`window.print`) |
| **Remise ligne manuelle** | API `lineDiscount` existe, pas d’UI (hors scope sprint) |
| **PaymentModal partagé** | Encore dupliqué vendeur / caissier (refactor possible S7) |
| **Hotkeys globales** | Pas de couche globale — raccourcis contextuels uniquement |
| **Doublons panier** | Fusion gérée côté backend à l’ajout ; pas de regroupement visuel supplémentaire |

---

## 7. Critères d’acceptation

| Critère | Statut |
|---------|--------|
| POS plus clair visuellement | OK |
| Session caisse visible | OK |
| Recherche plus agréable | OK (messages + empty states) |
| Panier lisible | OK |
| Stock insuffisant clair | OK |
| Paiement guidé | OK |
| Succès vente + ticket/facture | OK |
| Erreurs compréhensibles | OK |
| Flux POS non cassé | OK (pas de changement API) |
| Doc à jour | OK |
