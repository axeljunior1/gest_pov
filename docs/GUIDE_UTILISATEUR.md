# Guide Utilisateur — Gest_POV

**Gestion des produits, du stock et de la caisse**

Version destinée aux commerçants, caissiers et gestionnaires de stock.

---

## 1. Présentation

### À quoi sert l'application

**Gest_POV** (affiché « ERP Produits » à la connexion) est un logiciel de gestion pour les boutiques et entreprises de vente. Il permet de :

- gérer votre catalogue de produits ;
- suivre les entrées, sorties et inventaires de stock ;
- vendre en caisse (point de vente — POS) ;
- consulter l'historique des ventes et des retours ;
- analyser l'activité commerciale ;
- configurer votre entreprise (nom, logo, devise, taxes, tickets).

L'application fonctionne dans un navigateur web (Chrome, Edge, Firefox recommandés). Une connexion Internet ou un réseau local vers le serveur est nécessaire.

### Fonctionnalités principales

| Module | Ce que vous pouvez faire |
|--------|--------------------------|
| **Catalogue** | Produits, catégories, marques, variantes, codes-barres, conditionnements |
| **Stock** | Consultation, entrées, sorties, inventaires, mouvements, alertes, valorisation |
| **Caisse (POS)** | Vente, encaissement, tickets, mise en attente, scan code-barres |
| **Ventes** | Consultation des ventes et des retours |
| **Clients** | Fiches clients, historique d'achats, fidélité (si activée) |
| **Fournisseurs** | Fiches fournisseurs, commandes et réceptions |
| **Tableau de bord** | Vue d'ensemble du stock et des alertes |
| **Analytics** | Chiffre d'affaires, ventes, produits, paiements, exports |
| **Administration** | Utilisateurs, rôles, import/export, configuration |

### Types d'utilisateurs

Chaque personne se connecte avec son **email** et son **mot de passe**. Les droits dépendent du **rôle** attribué :

| Rôle | Usage typique |
|------|----------------|
| **Super administrateur** | Accès complet, licence, outils techniques |
| **Administrateur** | Gestion catalogue, stock, utilisateurs, paramètres |
| **Manager** | Stock, inventaires, analytics, caisse, clients |
| **Caissier** | Ouverture de caisse, encaissement, tickets, rapports de session |
| **Vendeur** | Préparation des ventes (sans encaissement en mode caisse centrale) |
| **Opérateur** | Saisie de brouillons stock, consultation |
| **Consultation** | Lecture seule (produits, stock, ventes) |

> **Conseil pratique** : donnez à chaque employé uniquement les droits nécessaires à son poste. Le caissier n'a pas besoin de modifier les produits ; le vendeur n'a pas besoin d'accéder aux paramètres.

[Capture d'écran ici — Page de connexion]

---

## 2. Première connexion

### Activation de la licence (premier démarrage)

Avant la première utilisation, l'application peut demander l'**activation de la licence Gest_POV** :

1. Notez l'**identifiant d'installation** affiché à l'écran.
2. Transmettez cet identifiant à votre revendeur ou support pour obtenir un fichier `.lic`.
3. Importez le fichier de licence sur la page d'activation.
4. Une fois la licence validée, l'application se recharge et la connexion devient possible.

[Capture d'écran ici — Page d'activation de la licence]

### Connexion

1. Ouvrez l'adresse de l'application dans votre navigateur (fournie par votre installateur).
2. Saisissez votre **email** et votre **mot de passe**.
3. Cliquez sur **Se connecter**.

Après connexion, vous êtes redirigé vers :
- la **caisse** si vous êtes caissier ou vendeur ;
- le **tableau de bord** ou un autre écran selon vos droits.

[Capture d'écran ici — Formulaire de connexion]

### Changement de mot de passe

L'application **ne propose pas** de page « Mon compte » pour changer son mot de passe soi-même.

Pour modifier un mot de passe :

1. Un **administrateur** se connecte.
2. Menu **Administration → Utilisateurs**.
3. Ouvre la fiche de l'utilisateur concerné.
4. Saisit un **nouveau mot de passe** (champ optionnel en modification).
5. Enregistre.

> **Conseil pratique** : changez les mots de passe par défaut dès la mise en service et utilisez des mots de passe différents pour chaque employé.

### Gestion des rôles et permissions

Les **rôles** définissent ce que chaque groupe d'utilisateurs peut faire (voir, créer, modifier, valider, etc.).

**Consulter ou ajuster les permissions d'un rôle :**

1. Menu **Administration → Rôles**.
2. Sélectionnez un rôle (ex. Caissier, Vendeur).
3. Cochez ou décochez les permissions.
4. Enregistrez.

**Créer un utilisateur :**

1. Menu **Administration → Utilisateurs**.
2. Cliquez sur **Nouvel utilisateur**.
3. Renseignez nom, email, mot de passe et **rôle(s)**.
4. Enregistrez.

> **Note** : la création de nouveaux rôles personnalisés n'est pas disponible dans l'interface ; vous travaillez avec les rôles fournis par l'application.

[Capture d'écran ici — Liste des utilisateurs]

---

## 3. Configuration initiale

Avant d'encaisser vos premières ventes, configurez votre entreprise. Menu **Administration → Configuration client**.

### Informations entreprise

1. Ouvrez **Configuration client**.
2. Section **Entreprise**, renseignez :
   - **Nom de l'entreprise** (obligatoire) — apparaît sur les tickets et dans l'interface ;
   - **Adresse**, **ville**, **pays** ;
   - **Téléphone**, **email** ;
   - **Numéro fiscal / RCCM** si applicable.
3. Cliquez sur **Enregistrer**.

[Capture d'écran ici — Section Entreprise]

### Devise

1. Dans la section **Entreprise**, choisissez la **devise** (ex. EUR, XOF, USD).
2. Tous les montants de l'application (caisse, rapports, analytics) utilisent cette devise.
3. Enregistrez.

> **Conseil pratique** : choisissez la devise une seule fois au déploiement. Un changement ultérieur n'convertit pas les anciennes ventes.

### Taxes

1. Section **Taxes** :
   - Activez les taxes si vous facturez la TVA (ou équivalent).
   - Indiquez le **nom** (ex. TVA) et le **taux par défaut** (%).
   - Cochez **Prix affichés TTC** si vos étiquettes sont en toutes taxes comprises.
   - Cochez **Application automatique sur les ventes** pour appliquer le taux en caisse.
2. Enregistrez.

[Capture d'écran ici — Section Taxes]

### Logo

1. Section **Entreprise → Logo**.
2. Cliquez sur **Parcourir** et sélectionnez une image (PNG, JPG recommandés).
3. Le logo est enregistré automatiquement après l'upload.
4. Cochez **Afficher le logo sur le ticket** dans la section **Caisse & tickets** si souhaité.

Le logo apparaît dans la barre latérale du back-office, dans l'en-tête de la caisse et sur les tickets (si activé).

[Capture d'écran ici — Aperçu du logo et du ticket]

### Paramètres caisse

Section **Caisse & tickets** :

| Paramètre | Description |
|-----------|-------------|
| **Nom caisse** | Libellé sur le ticket (ex. « Caisse 1 ») |
| **Préfixe ticket** | Début du numéro de ticket (ex. TK) |
| **Pied de ticket** | Message de remerciement en bas du ticket |
| **Impression automatique** | Lance l'impression du navigateur après chaque vente |
| **Rendu monnaie** | Affiche le montant à rendre au client |
| **Moyens de paiement** | Espèces, Carte, Mobile money, Virement — cochez ceux utilisés |
| **Paiement partiel / fractionné** | Autorise plusieurs modes de paiement ou un paiement incomplet |

> **Mode caisse centrale** : dans **Paramètres avancés** (`/settings`), le paramètre **Mode flux ventes** permet de séparer le vendeur (préparation du panier) et le caissier (encaissement). Par défaut, le vendeur envoie la vente à la file d'attente caisse.

[Capture d'écran ici — Section Caisse & tickets]

### Paramètres stock

Section **Stock** :

| Paramètre | Description |
|-----------|-------------|
| **Autoriser vente si stock insuffisant** | Si décoché, la caisse bloque la vente en rupture |
| **Seuil stock faible** | Quantité en dessous de laquelle une alerte est générée |
| **Méthode de valorisation** | Coût d'achat, coût moyen pondéré (CMP) ou prix de vente |
| **Alertes stock faible** | Active les notifications d'alerte |
| **Multi-entrepôt** | Active la gestion par entrepôt |

Enregistrez après chaque modification.

[Capture d'écran ici — Section Stock]

---

## 4. Gestion des produits

Menu **Catalogue → Produits**.

### Créer un produit

1. Cliquez sur **Nouveau produit** (ou accédez à la fiche vide).
2. Renseignez les informations principales :
   - **Nom**, **SKU** (référence interne), **code-barres** (optionnel) ;
   - **Catégorie**, **marque**, **fournisseur** ;
   - **Prix d'achat**, **prix de vente** ;
   - **Prix promotionnel** avec dates de début et fin (optionnel) ;
   - **Unité** de vente (pièce, kg, litre…).
3. Ajoutez une **image** si souhaité.
4. Enregistrez le produit.
5. Si nécessaire, **soumettez** le produit pour validation puis **activez-le** (workflow de cycle de vie).

> **Conseil pratique** : utilisez un SKU court et unique pour chaque article. Le code-barres peut être identique au SKU ou au code EAN du fournisseur.

[Capture d'écran ici — Fiche produit]

### Modifier un produit

1. Menu **Catalogue → Produits**.
2. Recherchez le produit (nom, SKU, code-barres).
3. Cliquez sur le produit pour ouvrir sa fiche.
4. Modifiez les champs souhaités.
5. Enregistrez.

L'historique des prix est conservé pour consultation sur la fiche produit.

### Catégories

Menu **Catalogue → Catégories**.

1. Les catégories sont organisées en **arborescence** (catégorie parente / sous-catégories).
2. Cliquez sur **Ajouter** pour créer une catégorie.
3. Indiquez le nom et, si besoin, la catégorie parente.
4. Enregistrez.

Les catégories servent au filtrage en caisse et dans les rapports.

[Capture d'écran ici — Arbre des catégories]

### Marques

Menu **Catalogue → Marques**.

1. Cliquez sur **Ajouter une marque**.
2. Saisissez le nom.
3. Enregistrez.

Associez ensuite la marque aux produits depuis leur fiche.

### Variantes

Pour un produit avec plusieurs tailles, couleurs, etc. :

1. Ouvrez la fiche produit.
2. Section **Attributs de variante** : définissez les attributs (ex. Taille, Couleur).
3. **Générez** ou ajoutez manuellement les variantes.
4. Pour chaque variante : SKU, stock, prix et code-barres propres.

En caisse, si un produit a des variantes, l'application demande de choisir la variante avant l'ajout au panier.

[Capture d'écran ici — Variantes produit]

### Codes-barres

**Sur la fiche produit ou variante :**

1. Saisissez le code-barres dans le champ prévu.
2. Enregistrez.

**Génération d'étiquette code-barres :**

L'application peut générer une image de code-barres (EAN-13, UPC, Code 128, QR) via l'API dédiée (utilisée par les intégrations ou outils internes).

**En caisse :**

- Scannez le code-barres avec un lecteur (comportement clavier) dans le champ de recherche.
- Ou saisissez le code manuellement.
- Si le scan automatique est activé dans les paramètres avancés, le produit est ajouté directement au panier.

[Capture d'écran ici — Champ code-barres en caisse]

### Recherche produit

**Dans le catalogue :**

- Utilisez la barre de recherche et les filtres (catégorie, marque, stock faible, rupture, statut).

**En caisse :**

- Tapez le nom, le SKU ou le code-barres dans le champ de recherche.
- Parcourez les catégories dans la grille produits.
- Format quantité + scan : saisir la quantité puis scanner (ex. `3*` puis scan).

[Capture d'écran ici — Recherche produit en caisse]

---

## 5. Gestion du stock

### Consultation du stock

Menu **Stock → Consultation**.

- Visualisez les quantités par produit, variante, entrepôt et emplacement.
- Colonnes utiles : stock physique, réservé, disponible.
- Onglets rapides : **Réception**, **Sortie**, **Ajustement** sur une ligne de stock.

[Capture d'écran ici — Grille de stock]

### Entrée de stock

Menu **Stock → Entrées**.

1. Cliquez sur **Nouvelle entrée**.
2. Choisissez l'**entrepôt** et le **fournisseur** (optionnel).
3. Ajoutez les **lignes** : produit, variante, quantité, prix d'achat, lot et date de péremption si applicable.
4. Enregistrez en **brouillon**.
5. Quand tout est correct, **validez** l'entrée — le stock est mis à jour.

> **Conseil pratique** : validez les entrées le jour de la réception marchandise pour garder un stock fiable.

[Capture d'écran ici — Document d'entrée de stock]

### Sortie de stock

Menu **Stock → Sorties**.

1. Créez une nouvelle sortie.
2. Choisissez le **motif** : usage interne, casse, perte, don, retour fournisseur, autre.
3. Ajoutez les lignes et quantités.
4. Validez le document — le stock diminue.

Les ventes en caisse génèrent automatiquement des sorties de stock à la validation de la vente.

### Inventaire

Menu **Stock → Inventaires**.

1. **Créez** un inventaire pour un entrepôt.
2. **Démarrez** le comptage.
3. Saisissez les **quantités comptées** ligne par ligne (ou via import).
4. **Validez** l'inventaire — les écarts sont enregistrés comme mouvements d'ajustement.
5. Vous pouvez **annuler** un inventaire non validé.

[Capture d'écran ici — Écran d'inventaire]

### Ajustement de stock

Depuis **Stock → Consultation** :

1. Repérez la ligne de stock concernée.
2. Onglet **Ajustement**.
3. Indiquez la nouvelle quantité ou l'écart.
4. Confirmez — un mouvement d'ajustement est créé.

### Historique des mouvements

Menu **Stock → Mouvements**.

- Consultez tous les mouvements (entrées, sorties, ventes, ajustements, inventaires).
- Filtrez par date, produit, entrepôt, type.
- **Exportez** en CSV si vous avez la permission d'export.

[Capture d'écran ici — Historique des mouvements]

### Alertes stock faible

Menu **Stock → Alertes**.

Types d'alertes possibles :

- stock faible ;
- rupture de stock ;
- surstock ;
- péremption proche ou produit expiré ;
- produit dormant ;
- retard fournisseur ;
- écart d'inventaire.

Pour chaque alerte : consultez le détail, **acquittez** ou **résolvez** selon le cas.

Les **notifications** (cloche en haut à droite) signalent aussi les alertes importantes.

> **Conseil pratique** : consultez les alertes chaque matin avant d'ouvrir la boutique.

[Capture d'écran ici — Liste des alertes]

---

## 6. Vente en caisse (POS)

Accès : menu **Vue d'ensemble → Caisse POS** ou `/pos`.

### Ouvrir une session caisse

1. À l'arrivée sur la caisse, si aucune session n'est ouverte, cliquez sur **Ouvrir la session**.
2. Indiquez le **fond de caisse** (espèces en caisse au démarrage).
3. Confirmez.

**Types de session :**

- **Session caisse** : pour l'encaissement (caissier).
- **Session vente** : pour la préparation des paniers (vendeur, en mode caisse centrale).

Fermez la session en fin de journée via **Fermer la session** — un rapport de clôture est généré (ventes, écarts espèces, etc.).

[Capture d'écran ici — Ouverture de session]

### Scanner un produit

1. Cliquez dans le champ de recherche.
2. Scannez le code-barres avec le lecteur.
3. Le produit est trouvé et ajouté au panier (selon configuration du scan automatique).

**Raccourci quantité :** saisir la quantité suivie de `*` puis scanner (ex. `5*`).

### Rechercher un produit

1. Tapez quelques lettres du nom ou le SKU dans le champ de recherche.
2. Sélectionnez le produit dans les résultats.
3. Choisissez la variante ou le conditionnement si demandé.

### Modifier les quantités

Dans le panier :

- Boutons **−** et **+** sur chaque ligne ;
- Saisie directe de la quantité ;
- Bouton **Supprimer** (quantité à 0) pour retirer une ligne.

### Appliquer une remise

Les remises en caisse peuvent provenir de :

1. **Prix promotionnel** — configuré sur la fiche produit avec dates de validité ; appliqué automatiquement.
2. **Fidélité** — si le programme fidélité est activé : associez un client au panier et utilisez ses **points** pour obtenir une réduction.
3. Le total des remises apparaît sur le ticket et dans le récapitulatif du panier.

> **Note** : l'application gère les remises au niveau des lignes côté serveur ; l'interface caisse met surtout en avant la fidélité et les prix promo. Contactez votre administrateur si des remises manuelles spécifiques sont nécessaires.

### Encaisser une vente

1. Vérifiez le panier et le total.
2. Cliquez sur **Encaisser** (ou **Envoyer à la caisse** en mode caisse centrale).
3. Dans la fenêtre de paiement, vérifiez le montant.
4. Cliquez sur **Encaisser** / **Valider paiement**.

En **mode caisse centrale** : le vendeur clique sur **Envoyer à la caisse** (raccourci **F4**) ; le caissier encaisse depuis **File d'attente** (`/pos/pending`).

### Paiement espèces

1. Laissez le mode **Espèces** sélectionné.
2. Saisissez éventuellement le **montant reçu** du client.
3. L'application affiche la **monnaie à rendre** (si le rendu monnaie est activé).
4. Validez le paiement.

### Paiement carte

1. Sélectionnez **Carte** comme mode de paiement.
2. Vérifiez le montant.
3. Validez — effectuez l'opération sur votre terminal de paiement externe.

### Paiement mobile money

1. Sélectionnez **Mobile money**.
2. Validez le montant dans l'application après confirmation sur le téléphone du client.

### Paiement fractionné

Si activé dans la configuration :

1. Dans la fenêtre de paiement, cliquez sur **+ Mode de paiement**.
2. Répartissez le total entre plusieurs modes (ex. une partie espèces, une partie carte).
3. Validez lorsque le total payé couvre le montant de la vente.

### Impression ticket

Après validation :

- Un **ticket** s'affiche à l'écran (nom entreprise, lignes, taxes, total, pied de ticket).
- Cliquez sur **Imprimer** pour lancer l'impression via le navigateur.
- Si **impression automatique** est activée, la boîte de dialogue d'impression s'ouvre automatiquement.

> **Conseil pratique** : configurez une imprimante par défaut sur le poste caisse et testez une vente à 0 € avant l'ouverture au public.

[Capture d'écran ici — Ticket de caisse]

### Mise en attente d'une vente

**Pause client (même poste vendeur) :**

1. Avec un panier en cours, cliquez sur **Pause client** (raccourci **F8**).
2. Le panier est mis en attente localement.
3. Servez un autre client, puis **Reprendre** (**F9**) pour retrouver le panier.

**File d'attente caisse (mode central) :**

- Les ventes envoyées à la caisse apparaissent dans **File d'attente** pour le caissier.

[Capture d'écran ici — Modal reprise de vente]

---

## 7. Retours et remboursements

### Effectuer un retour

**Depuis la caisse** — menu POS **Retours** (`/pos/returns`) :

1. Recherchez la vente d'origine (numéro de ticket ou vente).
2. Sélectionnez les **lignes** et **quantités** à retourner.
3. Indiquez le **motif**.
4. Choisissez si les articles **retournent en stock** (case à cocher par ligne).
5. Choisissez le **mode de remboursement**.
6. Validez — un **reçu de retour** peut être imprimé.

**Depuis le back-office** — menu **Ventes → Retours** : consultation de l'historique des retours et export CSV.

[Capture d'écran ici — Écran de retour POS]

### Annuler une vente

L'annulation d'une vente en cours (avant ou pendant le cycle de vente) est possible selon vos droits (`pos.sale.cancel`).

Les ventes annulées sont consultables dans **Analytics → Ventes annulées** avec le motif et le détail.

### Impact sur le stock

- **Retour avec remise en stock** : les quantités sont réintégrées dans l'entrepôt.
- **Retour sans remise en stock** : le remboursement est effectué mais le stock n'augmente pas (produit endommagé, etc.).
- **Vente validée** : le stock a déjà été diminué à la validation.

---

## 8. Gestion des clients

Menu **Clients**.

### Créer un client

**Depuis le back-office :**

1. Cliquez sur **Nouveau client**.
2. Renseignez nom, prénom, téléphone, email, adresse, etc.
3. Enregistrez.

**Depuis la caisse :**

1. Panneau **Client** à droite du panier.
2. Recherchez le client ou créez une fiche **rapide** (nom, téléphone).

### Historique d'achats

1. Ouvrez la fiche client.
2. Consultez le résumé : chiffre d'affaires, panier moyen, produits les plus achetés.
3. Parcourez la liste des ventes associées.

### Recherche client

- Back-office : barre de recherche sur la liste clients.
- Caisse : recherche par nom ou téléphone dans le panneau client.

**Programme fidélité** (si activé dans Paramètres avancés) : les points s'accumulent sur les ventes et peuvent être utilisés pour une réduction en caisse.

[Capture d'écran ici — Fiche client]

---

## 9. Gestion des fournisseurs

### Créer un fournisseur

Menu **Catalogue → Fournisseurs**.

1. Cliquez sur **Nouveau fournisseur**.
2. Renseignez nom, coordonnées, notes.
3. Enregistrez.
4. Associez le fournisseur aux produits depuis les fiches produits.

### Commandes fournisseurs

Menu **Stock → Commandes fourn.**

1. **Créez** une commande : fournisseur, lignes produits, quantités, prix.
2. Suivez le statut : en attente, partiellement reçue, livrée, annulée.
3. **Annulez** une commande non livrée si nécessaire.

### Réception des marchandises

1. Ouvrez la commande fournisseur.
2. Cliquez sur **Réceptionner** (ou action équivalente).
3. Indiquez les quantités réellement reçues.
4. Validez — une **entrée de stock** est générée automatiquement.

[Capture d'écran ici — Commande fournisseur]

---

## 10. Tableau de bord

Menu **Vue d'ensemble → Tableau de bord**.

Le tableau de bord est orienté **stock et alertes** :

| Indicateur | Signification |
|------------|----------------|
| **Produits actifs** | Nombre de produits en catalogue actif |
| **Quantité totale** | Somme des quantités en stock |
| **Valeur du stock** | Valorisation selon la méthode configurée |
| **Ruptures** | Produits à zéro |
| **Stock faible** | Produits sous le seuil |

Vous y trouvez aussi :

- les **alertes ouvertes** ;
- le stock **par entrepôt** ;
- les produits les plus mouvementés ;
- les derniers **mouvements**, **entrées** et **sorties**.

> **Pour le chiffre d'affaires et les ventes du jour** : utilisez le module **Analytics** (section 11).

[Capture d'écran ici — Tableau de bord]

---

## 11. Rapports et analytics

Menu **Analytics → Vue d'ensemble**.

### Chiffre d'affaires et ventes

Indicateurs disponibles selon la période choisie (aujourd'hui, hier, semaine, mois, etc.) :

- chiffre d'affaires ;
- nombre de ventes ;
- panier moyen ;
- articles vendus ;
- remboursements, remises, marge brute estimée ;
- ventes annulées.

### Graphiques

- évolution du CA dans le temps ;
- top produits et catégories ;
- répartition des **modes de paiement** ;
- performance des **caissiers** ;
- statistiques **clients** ;
- alertes stock et alertes métier.

### Rapports de ventes (back-office)

Menu **Ventes → Consultation ventes** : liste filtrable des ventes avec accès au détail et export CSV.

### Rapports stock

- **Stock → Mouvements** : export des mouvements.
- **Stock → Valorisation CMP** : vue valorisation, historique, produits dormants.
- **Stock → Alertes** : suivi des alertes.

### Rapports caissiers

- **Analytics** : section performance caissiers.
- **POS → Rapports de session** : rapports des sessions de caisse fermées.

### Exports CSV / Excel

| Zone | Formats |
|------|---------|
| **Analytics** (boutons Export produits / Export paiements) | CSV |
| **Import/Export** (Administration) | CSV et Excel (XLSX) pour produits, stock, mouvements, entrées, sorties, alertes, inventaires |
| **Ventes et retours** | CSV |

Pour exporter depuis **Import/Export** :

1. Menu **Administration → Import/Export**.
2. Onglet **Export**.
3. Choisissez le type de données et le format.
4. Téléchargez le fichier.

[Capture d'écran ici — Page Analytics]

---

## 12. Sauvegarde et sécurité

### Sauvegarde des données

L'application **ne contient pas** de bouton « Sauvegarder la base de données » pour l'utilisateur final.

En installation serveur (Docker ou serveur dédié), les données sont stockées sur le serveur :

- base de données (ventes, stock, produits, utilisateurs) ;
- fichiers uploadés (images produits, logo entreprise) ;
- fichier de licence.

**La sauvegarde régulière de ces données est de la responsabilité de l'installateur ou de l'administrateur système** (copie du serveur, snapshots, sauvegarde PostgreSQL, etc.).

> **Conseil pratique** : convenez d'une fréquence de sauvegarde (quotidienne minimum) avec votre prestataire informatique.

### Bonnes pratiques

1. **Mots de passe** : un compte par personne, mots de passe robustes, changement à l'embauche/départ.
2. **Droits** : principe du moindre privilège — ne donnez pas les droits admin aux caissiers.
3. **Sessions caisse** : ouvrez et fermez la session chaque jour ; vérifiez l'écart espèces à la clôture.
4. **Licence** : surveillez la date d'expiration (bannière d'avertissement dans l'application).
5. **Navigateur** : utilisez un navigateur à jour ; ne enregistrez pas le mot de passe sur un poste partagé sans précaution.
6. **Déconnexion** : déconnectez-vous en fin de poste sur un appareil partagé.

### Gestion des accès

- Créez les comptes dans **Utilisateurs**.
- Ajustez les **rôles** si un employé change de fonction.
- **Désactivez** (ou supprimez) les comptes des personnes qui quittent l'entreprise.

**Validation manager** : certaines actions sensibles (écart de caisse important, remboursement au-dessus d'un seuil) peuvent exiger le **mot de passe d'un manager** — paramétrable dans les paramètres avancés.

---

## 13. Questions fréquentes

### Impossible de vendre un produit

**Causes possibles :**

- le produit n'est pas **actif** dans le catalogue ;
- le produit est en **rupture** et la vente sans stock est interdite (Configuration client → Stock) ;
- vous n'avez pas la permission **pos.sale.create** ;
- aucune **session** n'est ouverte sur le poste ;
- en mode caisse centrale, le vendeur n'a pas envoyé la vente au caissier.

**Que faire :**

1. Vérifiez le statut du produit dans le catalogue.
2. Vérifiez le stock disponible.
3. Vérifiez qu'une session est ouverte.
4. Contactez votre responsable si le message mentionne une permission.

### Stock insuffisant

Message affiché en caisse lorsque la quantité demandée dépasse le stock et que l'option **Autoriser vente si stock insuffisant** est désactivée.

**Que faire :**

1. Réduisez la quantité dans le panier.
2. Ou effectuez une **entrée de stock** si la marchandise est arrivée.
3. Ou demandez à l'administrateur d'autoriser temporairement la vente sans stock (déconseillé en routine).

### Ticket non imprimé

**Causes possibles :**

- l'impression automatique n'est pas activée ;
- le navigateur bloque les pop-ups ou l'impression ;
- aucune imprimante par défaut n'est configurée sur l'ordinateur.

**Que faire :**

1. Après la vente, cliquez manuellement sur **Imprimer** dans la fenêtre du ticket.
2. Vérifiez l'imprimante et les pilotes Windows.
3. Activez **Impression automatique après vente** dans Configuration client si souhaité.
4. Réimprimez depuis **Historique ventes** ou le détail d'une vente (si vous avez la permission de réimpression).

### Produit introuvable en caisse

**Causes possibles :**

- produit non actif ou en brouillon ;
- code-barres incorrect ou non saisi sur la fiche ;
- produit avec variante : la variante n'a pas été sélectionnée ;
- filtre catégorie actif masquant le produit.

**Que faire :**

1. Recherchez par **nom** ou **SKU**.
2. Vérifiez le code-barres sur la fiche produit.
3. Réinitialisez le filtre catégorie (toutes catégories).

### Erreur de connexion

**Causes possibles :**

- email ou mot de passe incorrect ;
- compte désactivé ;
- serveur arrêté ou réseau coupé ;
- **licence expirée ou absente**.

**Que faire :**

1. Vérifiez l'email et le mot de passe (attention aux majuscules).
2. Demandez à l'administrateur de réinitialiser votre mot de passe.
3. Vérifiez que le serveur est démarré et que vous êtes sur le bon réseau.
4. Si un message de licence s'affiche, contactez le support pour renouveler le fichier `.lic`.

---

## Raccourcis clavier utiles en caisse

| Touche | Action |
|--------|--------|
| **F4** | Envoyer à la caisse (mode caisse centrale) |
| **F8** | Pause client (mise en attente locale) |
| **F9** | Reprendre une vente en attente |
| **Esc** | Fermer une fenêtre modale (paiement, ticket) |

---

## Support et documentation complémentaire

- **Documentation in-app** : menu **Aide → Documentation** (vue technique des modules).
- **Configuration** : menu **Administration → Configuration client** pour les réglages métier courants.
- **Paramètres avancés** : fidélité, numérotation, scan code-barres, mode caisse centrale.

---

*Document généré pour Gest_POV — Guide utilisateur final. Remettez ce fichier (PDF ou Markdown) à chaque client lors du déploiement.*
