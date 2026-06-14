import { differenceSeverityStyle, formatPosDateTime, formatPosMoney } from '../../utils/posMoney'

function SummaryRow({ label, value, highlight }) {
  return (
    <div className={`flex justify-between py-1.5 ${highlight ? 'font-semibold' : ''}`}>
      <span className="text-slate-500">{label}</span>
      <span className={highlight ? 'text-slate-900' : 'text-slate-800'}>{value}</span>
    </div>
  )
}

export function PosCloseReportContent({ report, currency, companyName }) {
  if (!report) return null
  const style = differenceSeverityStyle(report.differenceSeverity)

  return (
    <div className="pos-print-root pos-print-close-report pos-light-panel text-sm text-slate-900">
      <div className="text-center border-b border-dashed border-gray-300 pb-4 mb-4">
        {companyName && <p className="text-lg font-bold">{companyName}</p>}
        <p className="text-base font-semibold mt-1">Rapport de clôture de caisse</p>
        <p className="font-mono text-xs text-gray-500 mt-1">Session {report.sessionNumber}</p>
      </div>

      <div className={`rounded-lg border p-4 text-center mb-4 ${style.bg}`}>
        <p className={`text-xs uppercase tracking-wide ${style.text}`}>{style.label}</p>
        <p className="text-2xl font-bold mt-2 text-slate-900">
          {formatPosMoney(report.expectedCashAmount, currency)}
        </p>
        <p className="text-xs text-gray-500 mt-1">Cash attendu</p>
      </div>

      <div className="space-y-0.5">
        <SummaryRow label="Caissier" value={report.cashierName || '—'} />
        <SummaryRow label="Ouverture" value={formatPosDateTime(report.openedAt)} />
        <SummaryRow label="Fermeture" value={formatPosDateTime(report.closedAt)} />
        <SummaryRow label="Fond initial" value={formatPosMoney(report.openingCashAmount, currency)} />
        <SummaryRow label="Nombre de ventes" value={report.saleCount ?? 0} />
        <SummaryRow label="Montant total ventes" value={formatPosMoney(report.totalRevenue, currency)} />
        <SummaryRow label="Ventes espèces" value={formatPosMoney(report.cashRevenue, currency)} />
        <SummaryRow label="Ventes carte" value={formatPosMoney(report.cardRevenue, currency)} />
        <SummaryRow label="Mobile money" value={formatPosMoney(report.mobileMoneyRevenue, currency)} />
        <SummaryRow label="Virements" value={formatPosMoney(report.bankTransferRevenue, currency)} />
        <SummaryRow label="Remboursements cash" value={formatPosMoney(report.cashRefundTotal, currency)} />
        <SummaryRow label="Cash déclaré" value={formatPosMoney(report.declaredCashAmount, currency)} highlight />
        <SummaryRow label="Écart" value={formatPosMoney(report.cashDifference, currency)} highlight />
        {report.differenceReasonLabel && (
          <SummaryRow label="Motif écart" value={report.differenceReasonLabel} />
        )}
      </div>

      {report.differenceComment && (
        <p className="text-xs text-gray-600 bg-gray-50 rounded-lg p-3 mt-3 border border-gray-200">
          {report.differenceComment}
        </p>
      )}
      {report.managerValidatedBy && (
        <p className="text-xs text-gray-500 mt-3">
          Validé par {report.managerValidatedBy}
          {report.managerValidatedAt && ` · ${formatPosDateTime(report.managerValidatedAt)}`}
        </p>
      )}
      {report.closedBy && (
        <p className="text-xs text-gray-400 mt-4 pt-3 border-t border-dashed">
          Clôturé par {report.closedBy}
        </p>
      )}
    </div>
  )
}

export function PosCloseReportModal({ report, currency, companyName, onClose }) {
  if (!report) return null

  return (
    <div className="pos-print-overlay fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-4">
      <div className="w-full max-w-lg max-h-[90vh] overflow-auto rounded-2xl bg-slate-900 border border-slate-700">
        <div className="p-6 border-b border-slate-800 flex items-center justify-between gap-3">
          <div>
            <h2 className="text-xl font-semibold text-white">Rapport de clôture</h2>
            <p className="text-sm text-slate-400 mt-0.5">Session {report.sessionNumber}</p>
          </div>
        </div>
        <div className="p-6">
          <PosCloseReportContent report={report} currency={currency} companyName={companyName} />
        </div>
        <div className="pos-print-actions p-6 border-t border-slate-800 flex gap-3 justify-end">
          <button
            type="button"
            onClick={() => window.print()}
            className="px-5 py-2.5 bg-slate-100 hover:bg-white text-slate-900 rounded-lg text-sm font-medium"
          >
            Imprimer / PDF
          </button>
          <button
            type="button"
            onClick={onClose}
            className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 rounded-lg text-sm font-medium"
          >
            Terminer
          </button>
        </div>
      </div>
    </div>
  )
}
