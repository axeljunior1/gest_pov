/** Bandeau d'étape du flux POS (mode caisse centrale ou vendeur encaisseur). */
export default function PosFlowBanner({ mode, saleStatus, hasLines, stockIssues }) {
  if (mode === 'CENTRAL_CASHIER') {
    if (!hasLines) {
      return (
        <div className="px-4 py-2 bg-slate-900/80 border-b border-slate-800 text-slate-400 text-xs text-center">
          Étape 1 — Ajoutez des articles au panier · Étape 2 — Envoyer à la caisse (F4) · Étape 3 — Encaissement au poste caisse
        </div>
      )
    }
    if (stockIssues) {
      return (
        <div className="px-4 py-2 bg-red-950/70 border-b border-red-800 text-red-200 text-xs text-center" role="alert">
          Étape 2 bloquée — corrigez le stock insuffisant avant d&apos;envoyer à la caisse
        </div>
      )
    }
    if (saleStatus === 'DRAFT') {
      return (
        <div className="px-4 py-2 bg-amber-950/40 border-b border-amber-900/50 text-amber-100 text-xs text-center">
          Étape 2 — Panier prêt : <strong>Envoyer à la caisse</strong> (F4). La pause client (F8) reste sur ce poste.
        </div>
      )
    }
    if (saleStatus === 'PENDING_PAYMENT') {
      return (
        <div className="px-4 py-2 bg-emerald-950/40 border-b border-emerald-900/50 text-emerald-100 text-xs text-center">
          Étape 3 — Vente en caisse : le caissier doit encaisser sur le poste <strong>Encaissement</strong>
        </div>
      )
    }
    return null
  }

  if (mode === 'SELLER_COLLECTS_PAYMENT' && hasLines) {
    return (
      <div className="px-4 py-2 bg-emerald-950/30 border-b border-emerald-900/40 text-emerald-100 text-xs text-center">
        Étape 2 — Validez le <strong>paiement</strong> (F4) sur ce poste
      </div>
    )
  }

  return null
}
