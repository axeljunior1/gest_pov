import { Link } from 'react-router-dom'
import { PageHeader, Card, Badge } from '../components/ui'

const modules = [
  {
    id: 1,
    title: 'Module 1 — Gestion des produits',
    status: 'En place',
    tone: 'success',
    route: '/',
    pages: ['Produits', 'Catégories', 'Fournisseurs', 'Unités', 'Attributs'],
    features: [
      'Fiches produits : nom, SKU, description, marque, statut, cycle de vie',
      'Variantes (SKU, stock, prix, code-barres)',
      'Catégories hiérarchiques illimitées',
      'Multi-fournisseurs avec fournisseur principal',
      'Unités SI pré-configurées (20 unités, 15 conversions)',
      'Conditionnements produit avec conversion vers l’unité de base',
      'Codes-barres EAN13, UPC, Code128, QR Code',
      'Prix achat / vente / promotionnel + historique',
      'Images et documents (upload)',
      'Attributs personnalisés dynamiques',
      'Recherche avancée avec filtres',
      'Audit complet des modifications',
      'Suppressions : produits, médias, unités, catégories, fournisseurs, attributs',
    ],
    api: [
      'GET/POST /api/products',
      'GET /api/products/{id}',
      'GET/POST /api/categories',
      'GET/POST /api/suppliers',
      'GET/POST /api/units',
      'GET/POST /api/attributes',
      'POST /api/barcodes/generate',
    ],
  },
  {
    id: 2,
    title: 'Module 2 — Gestion de stock',
    status: 'En place',
    tone: 'success',
    route: '/stock',
    pages: ['Stock', 'Entrées stock', 'Sorties stock', 'Mouvements', 'Inventaires'],
    features: [
      'Entrepôts et emplacements (WH-MAIN / DEFAULT créés au démarrage)',
      'Entrées de stock multi-lignes (brouillon → validation) après achat externe',
      'Sorties de stock multi-lignes (vente, casse, perte, consommation…) avec motif et validation',
      'Historique global des mouvements (immuable, filtres combinés, export CSV)',
      'Inventaires physiques : comptage, écarts, correction stock via mouvement INVENTORY',
      'Conversion conditionnement → unité de base à la saisie',
      'Lots et dates de péremption à la validation',
      'Stock par produit, variante, entrepôt, emplacement et lot',
      'Stock disponible = stock physique − stock réservé',
      'Mouvements : entrée, sortie, ajustement, transfert, réservation, inventaire…',
      'Ledger centralisé avec verrou pessimiste (pas de stock négatif par défaut)',
      'Réservations et transferts inter-entrepôts',
      'Synchronisation du stock produit depuis les StockItem',
      'Lots avec dates de péremption et fabrication',
    ],
    api: [
      'GET/POST /api/warehouses',
      'GET /api/stock/items',
      'GET /api/stock/available',
      'POST /api/stock/receipt | issue | adjust',
      'GET /api/stock/movements (+ /{id}, /export)',
      'GET/POST /api/stock/reservations',
      'GET/POST /api/stock/transfers',
      'GET/POST /api/stock/inventories',
      'POST /api/stock/inventories/{id}/start | validate | cancel',
      'GET/POST /api/stock/entries',
      'POST /api/stock/entries/{id}/validate | cancel',
      'GET/POST /api/stock/exits',
      'POST /api/stock/exits/{id}/validate | cancel',
    ],
  },
  {
    id: 3,
    title: 'Module 3 — Alertes & notifications',
    status: 'En place',
    tone: 'success',
    route: '/alerts',
    pages: ['Alertes'],
    features: [
      '8 types d’alertes : LOW_STOCK, OUT_OF_STOCK, OVERSTOCK, EXPIRY_SOON, EXPIRED, DORMANT_PRODUCT, SUPPLIER_DELAY, INVENTORY_DISCREPANCY',
      'Déclenchement automatique après chaque mouvement de stock (seuils min/max)',
      'Contrôles planifiés quotidiens (péremption, produits dormants, retards fournisseur)',
      'Pas de doublon OPEN pour le même produit / entrepôt / emplacement / lot / type',
      'Résolution automatique quand la condition n’est plus vraie',
      'Configuration globale, par produit et par entrepôt (seuils, jours péremption, dormance)',
      'Préférences de notification utilisateur (canaux : IN_APP, EMAIL, SMS, PUSH, SLACK, WHATSAPP)',
      'Historique complet des alertes (OPEN, ACKNOWLEDGED, RESOLVED, IGNORED)',
      'Notifications IN_APP opérationnelles ; autres canaux en mode stub (logs)',
      'Permissions dédiées : alerts.read (consultation) et alerts.manage (traitement)',
    ],
    api: [
      'GET /api/alerts',
      'POST /api/alerts/{id}/acknowledge | resolve | ignore',
      'GET /api/notifications',
      'GET /api/notifications/unread-count',
      'POST /api/notifications/{id}/read',
    ],
  },
  {
    id: 4,
    title: 'Module 4 — Utilisateurs, rôles & authentification',
    status: 'En place',
    tone: 'success',
    route: '/login',
    pages: ['Connexion', 'Utilisateurs', 'Rôles'],
    features: [
      'Authentification JWT (Bearer token, expiration 24 h)',
      'Compte admin initial : admin@erp.local / ErpAdmin2026!',
      '6 rôles système : SUPER_ADMIN, ADMIN, MANAGER, OPERATOR, VIEWER, CASHIER',
      'Permissions granulaires (produits, stock, mouvements, inventaires, entrées, sorties, alertes, dashboard, import/export, paramètres, utilisateurs, rôles, POS, clients, fidélité)',
      'Protection @PreAuthorize sur toutes les routes métier',
      'Acteur d’audit et mouvements : email JWT côté serveur (champ client ignoré si authentifié)',
      'Permissions rechargées depuis la base à chaque requête (effet immédiat sans reconnexion)',
      'Routes frontend protégées par permission (Utilisateurs, Rôles, Alertes, Sorties stock, Mouvements) + message 403 explicite',
      'Pages Utilisateurs : création, modification, activation/désactivation, assignation de rôles',
      'Page Rôles : consultation et édition des permissions par rôle (sauf SUPER_ADMIN)',
    ],
    api: [
      'POST /api/auth/login',
      'GET /api/auth/me',
      'GET/POST/PUT/DELETE /api/users',
      'GET /api/roles',
      'PUT /api/roles/{id}/permissions',
    ],
  },
  {
    id: 5,
    title: 'Module 5 — Tableau de bord KPI',
    status: 'En place',
    tone: 'success',
    route: '/dashboard',
    pages: ['Tableau de bord'],
    features: [
      'Synthèse stock : produits actifs, valeur estimée, alertes ouvertes',
      'Mouvements récents, entrées et sorties validées',
      'Top produits les plus mouvementés',
      'Répartition par entrepôt',
      'Devise affichée selon les paramètres généraux (app.currency)',
      'Permission dédiée : dashboard.read',
    ],
    api: [
      'GET /api/dashboard/summary',
      'GET /api/dashboard/alerts',
      'GET /api/dashboard/recent-movements',
      'GET /api/dashboard/recent-entries',
      'GET /api/dashboard/recent-exits',
      'GET /api/dashboard/top-moved-products',
      'GET /api/dashboard/warehouses',
    ],
  },
  {
    id: 6,
    title: 'Module 6 — Import / Export',
    status: 'En place',
    tone: 'success',
    route: '/import-export',
    pages: ['Import / Export'],
    features: [
      'Export CSV ou Excel : produits, stock, mouvements, entrées, sorties, alertes, inventaires',
      'Import produits avec prévisualisation et mode doublon (refuser / mettre à jour)',
      'Import conditionnements (liés au SKU produit)',
      'Import stock initial (mouvement INITIAL_STOCK)',
      'Templates téléchargeables par type d’import',
      'Historique des jobs d’import (statut, lignes OK / erreurs)',
      'Permissions : import.read, import.create, export.read',
    ],
    api: [
      'GET /api/export/{type}?format=CSV|XLSX',
      'GET /api/import/templates/{products|packagings|initial-stock}',
      'POST /api/import/products/preview | validate',
      'POST /api/import/packagings/preview | validate',
      'POST /api/import/initial-stock/preview | validate',
      'GET /api/import/history',
    ],
  },
  {
    id: 7,
    title: 'Module 7 — Paramètres généraux',
    status: 'En place',
    tone: 'success',
    route: '/settings',
    pages: ['Paramètres'],
    features: [
      'Paramètres centralisés (table app_settings) : entreprise, logo, devise, langue, fuseau, format date',
      'Stock : autorisation stock négatif, seuil stock faible par défaut',
      'Alertes : délai péremption par défaut (utilisé si non surchargé par produit/entrepôt)',
      'Numérotation : préfixes entrées, sorties, inventaires, mouvements, ventes POS',
      'POS : nom caisse, taux TVA, entrepôt par défaut, mode caisse (SELLER_CASHIER / CENTRAL_CASHIER)',
      'POS : paiement partiel/fractionné, durée max attente paiement, seuils alertes',
      'Fidélité paramétrable : taux de gain, valeur du point, minimum utilisation, plafond remise, expiration, niveaux JSON',
      'Paramètres publics (sans auth) pour nom entreprise et affichage UI',
      'Intégration modules stock, alertes, numérotation documents, caisse',
      'Permissions : settings.read, settings.update, loyalty.settings.update',
    ],
    api: [
      'GET /api/settings/public',
      'GET /api/settings',
      'PUT /api/settings/{key}',
      'PUT /api/settings (bulk)',
      'GET /api/settings/config/numbering',
      'GET /api/settings/config/stock',
      'GET /api/settings/config/alerts',
      'GET /api/settings/config/loyalty',
    ],
  },
  {
    id: 8,
    title: 'Module 8 — Caisse POS',
    status: 'En place',
    tone: 'success',
    route: '/pos',
    pages: ['Caisse POS', 'Paiements en attente (/pos/pending)'],
    features: [
      'Deux modes pilotés par Settings (pos.cash_handling_mode) sans modification de code :',
      '  · SELLER_CASHIER — vendeur = caissier : DRAFT → paiement → VALIDATED, session CASHIER, fond de caisse',
      '  · CENTRAL_CASHIER — vendeur prépare (session SALES) → PENDING_PAYMENT → caissier encaisse → VALIDATED',
      'Stock décrémenté uniquement à VALIDATED (jamais en DRAFT ni PENDING_PAYMENT)',
      'Sessions typées SALES (vendeur) ou CASHIER (caisse) selon le mode et le rôle',
      'Rôles SELLER (préparation) et CASHIER (encaissement) avec permissions dédiées',
      'Écran « Paiements en attente » pour la caisse centrale (encaisser / annuler / détail)',
      'Sessions de caisse (ouverture / clôture) liées à un entrepôt, calcul écart cash',
      'Catalogue produits par catégorie, promotions, recherche nom / SKU / code-barres',
      'Panier : lignes, quantités, remises, ventes en attente (HOLD) et reprise',
      'Paiement multi-modes paramétrable (split / partiel via Settings)',
      'Validation vente → sortie stock (mouvement OUT) + ticket TK-YYYYMMDD-####',
      'Remboursements total ou partiel avec retour stock optionnel',
      'Comptes test : seller@erp.local / Seller2026! · cashier@erp.local / Cashier2026!',
      'Raccourcis : F2 recherche, F3 quantité, F4 paiement ou envoi caisse, F8 attente, F9 reprise',
      'Permissions : pos.sale.prepare, pos.payment.collect, pos.session.*, pos.sale.*',
    ],
    api: [
      'GET /api/pos/context (inclut posConfig)',
      'POST /api/pos/sessions/open | close',
      'GET /api/pos/catalog | /catalog/search',
      'POST /api/pos/sales',
      'POST /api/pos/sales/{id}/submit-payment',
      'GET /api/pos/sales/pending-payment',
      'POST /api/pos/sales/{id}/lines | validate | hold | resume | cancel',
      'POST /api/pos/sales/{id}/refund',
      'GET /api/pos/sales/{id}/ticket',
    ],
  },
  {
    id: 9,
    title: 'Module 9 — Clients & Fidélité',
    status: 'En place',
    tone: 'success',
    route: '/customers',
    pages: ['Clients', 'Caisse POS (zone client)'],
    features: [
      'Fiche client : numéro CLI-YYYYMMDD-####, identité, contact, notes, statut actif',
      'Vente anonyme toujours possible ; association client sur vente brouillon',
      'Recherche POS par téléphone, nom, prénom, email ou numéro client',
      'Création rapide depuis la caisse (nom + téléphone) sans quitter l’écran POS',
      'Fidélité entièrement paramétrable (Settings) — jamais codée en dur',
      'Gain de points à la validation vente uniquement (pas en attente / brouillon / annulée)',
      'Utilisation des points en remise avant paiement (minimum, plafond %, valeur point)',
      'Niveaux fidélité configurables en JSON (BRONZE, SILVER, GOLD, PLATINUM…)',
      'Historique immuable LoyaltyTransaction avec rule_snapshot (paramètres au moment du calcul)',
      'Remboursement : reversal des points gagnés et recrédit des points utilisés',
      'Fiche client : achats, CA, panier moyen, top produits, transactions fidélité',
      'Ajustement manuel points réservé MANAGER / ADMIN (caissier interdit)',
      'Comptes : caissier@erp.local / Caissier2026! (prod) · cashier@erp.local / Cashier2026! (test)',
    ],
    api: [
      'GET/POST/PUT/DELETE /api/customers',
      'GET /api/customers/search | /{id}/history',
      'GET /api/customers/{id}/loyalty/transactions',
      'POST /api/customers/{id}/loyalty/adjust',
      'GET /api/pos/customers/search',
      'POST /api/pos/customers/quick',
      'PUT/DELETE /api/pos/sales/{id}/customer',
      'POST/DELETE /api/pos/sales/{id}/loyalty/redeem',
    ],
  },
]

function FeatureList({ items }) {
  return (
    <ul className="mt-3 space-y-1.5">
      {items.map((item) => (
        <li key={item} className="flex gap-2 text-sm text-gray-600">
          <span className="text-emerald-500 shrink-0 mt-0.5">✓</span>
          <span>{item}</span>
        </li>
      ))}
    </ul>
  )
}

function ApiList({ items }) {
  return (
    <div className="mt-4 flex flex-wrap gap-2">
      {items.map((item) => (
        <code
          key={item}
          className="text-xs bg-gray-100 text-gray-700 px-2 py-1 rounded-md font-mono"
        >
          {item}
        </code>
      ))}
    </div>
  )
}

export default function DocumentationPage() {
  return (
    <div>
      <PageHeader
        title="Documentation"
        subtitle="État d’avancement de l’application ERP — ce qui est en place aujourd’hui"
      />

      <Card className="p-6 mb-6 bg-gray-50 border-gray-200">
        <h3 className="text-sm font-semibold text-gray-900 mb-2">Stack technique</h3>
        <div className="flex flex-wrap gap-2">
          {['Spring Boot 3.2', 'Java 17', 'PostgreSQL', 'React 19', 'Vite', 'Tailwind CSS 4'].map((t) => (
            <Badge key={t} tone="info">{t}</Badge>
          ))}
        </div>
        <p className="text-sm text-gray-600 mt-4">
          Backend sur le port <strong>8080</strong>, frontend sur le port <strong>5173</strong>.
          Base PostgreSQL via Docker (<code className="text-xs bg-white px-1.5 py-0.5 rounded">.\db.ps1</code> ou{' '}
          <code className="text-xs bg-white px-1.5 py-0.5 rounded">.\dev.ps1</code>).
          94+ tests backend automatisés (JUnit + MockMvc).
        </p>
      </Card>

      <div className="space-y-6">
        {modules.map((mod) => (
          <Card key={mod.id} className="p-6">
            <div className="flex flex-wrap items-start justify-between gap-3 mb-1">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <Badge tone={mod.tone}>{mod.status}</Badge>
                  <span className="text-xs text-gray-400">Module {mod.id}</span>
                </div>
                <h3 className="text-lg font-semibold text-gray-900">{mod.title}</h3>
              </div>
              <Link
                to={mod.route}
                className="text-sm text-gray-600 hover:text-gray-900 underline underline-offset-2"
              >
                Ouvrir la page →
              </Link>
            </div>

            <p className="text-sm text-gray-500">
              Pages frontend : {mod.pages.join(', ')}
            </p>

            <FeatureList items={mod.features} />
            <ApiList items={mod.api} />
          </Card>
        ))}
      </div>

      <Card className="p-6 mt-6 bg-emerald-50 border-emerald-100">
        <h3 className="text-sm font-semibold text-emerald-900 mb-2">Sécurité & permissions (résumé)</h3>
        <ul className="text-sm text-emerald-900 space-y-1.5">
          <li>• Connexion JWT — compte par défaut : <code className="text-xs bg-white px-1 rounded">admin@erp.local</code> / <code className="text-xs bg-white px-1 rounded">ErpAdmin2026!</code></li>
          <li>• Le backend recharge les permissions depuis PostgreSQL à chaque requête authentifiée (modification de rôle effective immédiatement).</li>
          <li>• L’historique d’audit et les mouvements de stock utilisent l’email du token, jamais une valeur envoyée par le client.</li>
          <li>• Alertes : <code className="text-xs bg-white px-1 rounded">alerts.read</code> pour consulter, <code className="text-xs bg-white px-1 rounded">alerts.manage</code> pour acquitter / résoudre / ignorer.</li>
          <li>• Paramètres : <code className="text-xs bg-white px-1 rounded">settings.read</code> pour consulter, <code className="text-xs bg-white px-1 rounded">settings.update</code> pour modifier.</li>
          <li>• Import/Export : <code className="text-xs bg-white px-1 rounded">import.read</code>, <code className="text-xs bg-white px-1 rounded">import.create</code>, <code className="text-xs bg-white px-1 rounded">export.read</code>.</li>
          <li>• POS : <code className="text-xs bg-white px-1 rounded">pos.sale.prepare</code>, <code className="text-xs bg-white px-1 rounded">pos.payment.collect</code>, <code className="text-xs bg-white px-1 rounded">pos.session.open</code>… — rôles SELLER (préparation) et CASHIER (encaissement).</li>
          <li>• Clients : <code className="text-xs bg-white px-1 rounded">customer.read</code>, <code className="text-xs bg-white px-1 rounded">customer.create</code> (caissier), <code className="text-xs bg-white px-1 rounded">customer.update</code> (manager).</li>
          <li>• Fidélité : <code className="text-xs bg-white px-1 rounded">loyalty.read</code>, <code className="text-xs bg-white px-1 rounded">loyalty.redeem</code> (caissier), <code className="text-xs bg-white px-1 rounded">loyalty.manage</code> (ajustement points, manager).</li>
          <li>• Navigation regroupée : Catalogue, Stock, Clients, Administration avec sous-onglets horizontaux.</li>
          <li>• Pages sensibles protégées côté frontend : /users, /roles, /alerts, /settings, /customers — accès refusé affiché si permission manquante.</li>
          <li>• Erreur HTTP 403 : message « Accès refusé — permission insuffisante ».</li>
        </ul>
      </Card>

      <Card className="p-6 mt-6 border-dashed">
        <h3 className="text-sm font-semibold text-gray-900 mb-2">Règles métier fidélité (résumé)</h3>
        <ul className="text-sm text-gray-600 space-y-1.5">
          <li>• Paramètres par défaut : 1 € dépensé = 1 point · 1 point = 0,05 € · minimum 100 points pour utiliser · remise max 50 % du total</li>
          <li>• Points gagnés uniquement sur vente validée avec client identifié ; fidélité désactivable via <code className="text-xs bg-gray-100 px-1 rounded">loyalty.enabled</code></li>
          <li>• Points utilisés : réservés sur le panier, débités définitivement au paiement (validation)</li>
          <li>• Chaque transaction stocke un <code className="text-xs bg-gray-100 px-1 rounded">rule_snapshot</code> — l’historique reste correct si les paramètres changent</li>
          <li>• Remboursement : retrait des points gagnés + recrédit des points utilisés (transaction REFUND_REVERSAL)</li>
          <li>• Niveaux recalculés automatiquement après chaque gain (config JSON dans Paramètres → Fidélité)</li>
        </ul>
      </Card>

      <Card className="p-6 mt-6 border-dashed">
        <h3 className="text-sm font-semibold text-gray-900 mb-2">Règles métier alertes (résumé)</h3>
        <ul className="text-sm text-gray-600 space-y-1.5">
          <li>• LOW_STOCK : stock disponible ≤ seuil minimum (défaut 10)</li>
          <li>• OUT_OF_STOCK : stock disponible = 0</li>
          <li>• OVERSTOCK : stock disponible ≥ seuil maximum (défaut 1000)</li>
          <li>• EXPIRY_SOON / EXPIRED : basé sur la date de péremption du lot (alerte à J−30 par défaut)</li>
          <li>• DORMANT_PRODUCT : aucun mouvement depuis 90 jours (défaut)</li>
          <li>• SUPPLIER_DELAY : commande fournisseur en retard sur la date prévue</li>
          <li>• INVENTORY_DISCREPANCY : écart détecté à la validation d’un inventaire</li>
        </ul>
      </Card>

      <Card className="p-6 mt-6 bg-amber-50 border-amber-100">
        <h3 className="text-sm font-semibold text-amber-900 mb-2">Stabilité du serveur (dépannage)</h3>
        <ul className="text-sm text-amber-800 space-y-1.5">
          <li>• Si l’API ne répond plus (erreurs 502 dans le navigateur), le backend est probablement arrêté ou en cours de redémarrage.</li>
          <li>• Cause principale identifiée : Spring DevTools redémarrait automatiquement le backend à chaque compilation, laissant le port 8080 indisponible plusieurs secondes (parfois crash complet).</li>
          <li>• Correctif appliqué : redémarrage automatique DevTools désactivé. Relancer manuellement avec <code className="text-xs bg-white px-1 rounded">.\dev.ps1</code> après modification du code Java.</li>
          <li>• Au démarrage à froid, Spring Boot met ~15–30 s à être prêt (schéma JPA + initialisation PostgreSQL).</li>
        </ul>
      </Card>

      <Card className="p-6 mt-6 bg-amber-50 border-amber-100">
        <h3 className="text-sm font-semibold text-amber-900 mb-2">Hors périmètre actuel</h3>
        <p className="text-sm text-amber-800">
          Envoi réel EMAIL/SMS/PUSH/Slack/WhatsApp, interface avancée de configuration des seuils d’alerte par produit,
          gestion des commandes fournisseur (entité minimale pour les retards),
          expiration automatique planifiée des points fidélité (champ configuré, job non planifié)
          — non implémentés à ce stade.
        </p>
      </Card>
    </div>
  )
}
