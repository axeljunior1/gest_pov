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
    pages: ['Stock', 'Entrées stock', 'Sorties stock'],
    features: [
      'Entrepôts et emplacements (WH-MAIN / DEFAULT créés au démarrage)',
      'Entrées de stock multi-lignes (brouillon → validation) après achat externe',
      'Sorties de stock multi-lignes (vente, casse, perte, consommation…) avec motif et validation',
      'Conversion conditionnement → unité de base à la saisie',
      'Lots et dates de péremption à la validation',
      'Stock par produit, variante, entrepôt, emplacement et lot',
      'Stock disponible = stock physique − stock réservé',
      'Mouvements : entrée, sortie, ajustement, transfert, réservation, inventaire…',
      'Ledger centralisé avec verrou pessimiste (pas de stock négatif par défaut)',
      'Réservations et transferts inter-entrepôts',
      'Inventaires physiques avec écarts et validation',
      'Synchronisation du stock produit depuis les StockItem',
      'Lots avec dates de péremption et fabrication',
    ],
    api: [
      'GET/POST /api/warehouses',
      'GET /api/stock/items',
      'GET /api/stock/available',
      'POST /api/stock/receipt | issue | adjust',
      'GET /api/stock/movements',
      'GET/POST /api/stock/reservations',
      'GET/POST /api/stock/transfers',
      'GET/POST /api/stock/inventories',
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
      '5 rôles système : SUPER_ADMIN, ADMIN, MANAGER, OPERATOR, VIEWER',
      '26 permissions granulaires (produits, stock, entrées, sorties, alertes, utilisateurs, rôles)',
      'Protection @PreAuthorize sur toutes les routes métier',
      'Acteur d’audit et mouvements : email JWT côté serveur (champ client ignoré si authentifié)',
      'Permissions rechargées depuis la base à chaque requête (effet immédiat sans reconnexion)',
      'Routes frontend protégées par permission (Utilisateurs, Rôles, Alertes, Sorties stock) + message 403 explicite',
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
          31 tests backend automatisés (JUnit + MockMvc).
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
          <li>• Pages sensibles protégées côté frontend : /users, /roles, /alerts — accès refusé affiché si permission manquante.</li>
          <li>• Erreur HTTP 403 : message « Accès refusé — permission insuffisante ».</li>
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
          Envoi réel EMAIL/SMS/PUSH/Slack/WhatsApp, interface de configuration des seuils d’alerte,
          gestion des commandes fournisseur (entité minimale pour les retards), tableaux de bord analytics
          — non implémentés à ce stade.
        </p>
      </Card>
    </div>
  )
}
