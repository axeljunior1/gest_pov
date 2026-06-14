import { formatPosMoney } from '../../utils/posMoney'

export function PosTicketModal({ ticket, onClose }) {
  if (!ticket) return null
  return (
    <div className="pos-print-overlay fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="pos-print-root pos-print-ticket pos-light-panel rounded-xl w-full max-w-sm p-6 font-mono text-sm">
        <p className="font-bold text-center">{ticket.companyName}</p>
        <p className="text-center text-xs text-gray-500">{ticket.registerName}</p>
        <p className="text-center text-xs mb-1">Ticket {ticket.ticketNumber}</p>
        {ticket.saleDate && (
          <p className="text-center text-xs text-gray-500 mb-3">
            {new Date(ticket.saleDate).toLocaleString('fr-FR')}
          </p>
        )}
        {ticket.cashierName && (
          <p className="text-center text-xs text-gray-500 mb-3">Caissier : {ticket.cashierName}</p>
        )}
        <hr className="my-2 border-dashed" />
        {ticket.lines?.map((l, i) => (
          <div key={i} className="flex justify-between gap-2 py-0.5">
            <span>{l.productNom} x{Number(l.quantity)}</span>
            <span>{Number(l.lineTotal).toFixed(2)}</span>
          </div>
        ))}
        <hr className="my-2 border-dashed" />
        <div className="flex justify-between font-bold">
          <span>TOTAL</span>
          <span>{Number(ticket.total).toFixed(2)} {ticket.currency}</span>
        </div>
        {ticket.changeAmount > 0 && (
          <p className="text-xs mt-1">Monnaie : {Number(ticket.changeAmount).toFixed(2)}</p>
        )}
        <div className="pos-print-actions flex gap-2 mt-4">
          <button type="button" onClick={() => window.print()} className="flex-1 py-2 bg-gray-900 text-white rounded-lg text-xs">
            Imprimer
          </button>
          <button type="button" onClick={onClose} className="flex-1 py-2 bg-gray-200 rounded-lg text-xs">
            Fermer
          </button>
        </div>
      </div>
    </div>
  )
}

export function PosInvoiceModal({ invoice, onClose }) {
  if (!invoice) return null
  return (
    <div className="pos-print-overlay fixed inset-0 bg-black/70 flex items-center justify-center z-50 p-4">
      <div className="pos-print-root pos-print-invoice pos-light-panel rounded-xl w-full max-w-lg p-8 text-sm">
        <div className="flex justify-between items-start gap-4 mb-6">
          <div>
            <p className="text-lg font-bold">{invoice.companyName}</p>
            <p className="text-xs text-gray-500">{invoice.registerName}</p>
          </div>
          <div className="text-right">
            <p className="text-lg font-bold uppercase tracking-wide">Facture</p>
            <p className="font-mono text-sm">{invoice.invoiceNumber}</p>
            {invoice.saleDate && (
              <p className="text-xs text-gray-500 mt-1">
                {new Date(invoice.saleDate).toLocaleDateString('fr-FR')}
              </p>
            )}
          </div>
        </div>

        {invoice.customerName ? (
          <div className="mb-6 p-3 bg-gray-50 rounded-lg border border-gray-200">
            <p className="text-xs uppercase text-gray-500 mb-1">Client</p>
            <p className="font-semibold">{invoice.customerName}</p>
            {invoice.customerNumber && (
              <p className="text-xs text-gray-500">N° {invoice.customerNumber}</p>
            )}
            {invoice.customerAddress && <p className="text-sm text-gray-600">{invoice.customerAddress}</p>}
            {invoice.customerCity && <p className="text-sm text-gray-600">{invoice.customerCity}</p>}
            {invoice.customerPhone && <p className="text-sm text-gray-600">{invoice.customerPhone}</p>}
            {invoice.customerEmail && <p className="text-sm text-gray-600">{invoice.customerEmail}</p>}
          </div>
        ) : (
          <p className="text-sm text-gray-500 mb-6 italic border border-dashed border-gray-300 rounded-lg p-3">
            Vente comptoir — client non renseigné
          </p>
        )}

        <table className="w-full text-sm mb-4 pos-print-table">
          <thead>
            <tr className="border-b text-left text-gray-500 text-xs">
              <th className="py-2">Désignation</th>
              <th className="py-2 text-right">Qté</th>
              <th className="py-2 text-right">P.U.</th>
              <th className="py-2 text-right">Total</th>
            </tr>
          </thead>
          <tbody>
            {invoice.lines?.map((l, i) => (
              <tr key={i} className="border-b border-gray-100">
                <td className="py-2">{l.productNom}</td>
                <td className="py-2 text-right">{Number(l.quantity)}</td>
                <td className="py-2 text-right">{formatPosMoney(l.unitPrice, invoice.currency)}</td>
                <td className="py-2 text-right">{formatPosMoney(l.lineTotal, invoice.currency)}</td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="space-y-1 text-sm border-t pt-3">
          <div className="flex justify-between"><span>Sous-total</span><span>{formatPosMoney(invoice.subtotal, invoice.currency)}</span></div>
          {Number(invoice.discountTotal) > 0 && (
            <div className="flex justify-between text-gray-600">
              <span>Remises</span><span>-{formatPosMoney(invoice.discountTotal, invoice.currency)}</span>
            </div>
          )}
          {Number(invoice.loyaltyDiscountAmount) > 0 && (
            <div className="flex justify-between text-gray-600">
              <span>Fidélité</span><span>-{formatPosMoney(invoice.loyaltyDiscountAmount, invoice.currency)}</span>
            </div>
          )}
          {Number(invoice.taxTotal) > 0 && (
            <div className="flex justify-between"><span>TVA</span><span>{formatPosMoney(invoice.taxTotal, invoice.currency)}</span></div>
          )}
          <div className="flex justify-between font-bold text-base pt-2">
            <span>Total TTC</span><span>{formatPosMoney(invoice.total, invoice.currency)}</span>
          </div>
        </div>

        <p className="text-xs text-gray-500 mt-4">
          Vendeur : {invoice.sellerName} · Caissier : {invoice.cashierName}
        </p>

        <div className="pos-print-actions flex gap-2 mt-6">
          <button type="button" onClick={() => window.print()} className="flex-1 py-2 bg-gray-900 text-white rounded-lg text-xs">
            Imprimer facture
          </button>
          <button type="button" onClick={onClose} className="flex-1 py-2 bg-gray-200 rounded-lg text-xs">
            Fermer
          </button>
        </div>
      </div>
    </div>
  )
}
