import { useEffect, useMemo, useState } from 'react'
import { posApi } from '../../api'
import { useNotification } from '../../context/NotificationContext'
import { getErrorMessage } from '../../utils/errors'
import { differenceSeverityStyle, formatPosDateTime, formatPosMoney } from '../../utils/posMoney'

function SummaryRow({ label, value, highlight }) {
  return (
    <div className={`flex justify-between py-1.5 ${highlight ? 'font-semibold' : ''}`}>
      <span className="text-slate-400">{label}</span>
      <span className={highlight ? 'text-white' : 'text-slate-200'}>{value}</span>
    </div>
  )
}

export default function CashSessionCloseModal({ session, currency, onClose, onClosed }) {
  const notify = useNotification()
  const [preview, setPreview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [submitting, setSubmitting] = useState(false)
  const [declaredCash, setDeclaredCash] = useState('')
  const [differenceReason, setDifferenceReason] = useState('')
  const [differenceComment, setDifferenceComment] = useState('')
  const [managerEmail, setManagerEmail] = useState('')
  const [managerPassword, setManagerPassword] = useState('')
  const [report, setReport] = useState(null)

  useEffect(() => {
    posApi.sessionClosePreview()
      .then((data) => {
        setPreview(data)
        setDeclaredCash(String(data.expectedCashAmount ?? ''))
      })
      .catch((e) => notify.error(getErrorMessage(e)))
      .finally(() => setLoading(false))
  }, [notify])

  const declaredNum = useMemo(() => {
    const n = Number(declaredCash)
    return Number.isFinite(n) ? n : null
  }, [declaredCash])

  const liveDifference = useMemo(() => {
    if (declaredNum == null || preview?.expectedCashAmount == null) return null
    return declaredNum - Number(preview.expectedCashAmount)
  }, [declaredNum, preview])

  const liveSeverity = useMemo(() => {
    if (liveDifference == null) return null
    if (Math.abs(liveDifference) < 0.0001) return 'BALANCED'
    const threshold = preview?.alertCashDifferenceThreshold ?? 20
    return Math.abs(liveDifference) <= threshold ? 'MINOR' : 'MAJOR'
  }, [liveDifference, preview])

  const needsReason = liveDifference != null && Math.abs(liveDifference) >= 0.0001
  const needsManager = needsReason && preview?.requireManagerValidationForDifference

  const submit = async () => {
    if (declaredNum == null) {
      notify.error('Saisissez le montant réellement présent en caisse')
      return
    }
    if (needsReason && !differenceReason) {
      notify.error('Motif de l\'écart obligatoire')
      return
    }
    if (needsManager && (!managerEmail.trim() || !managerPassword)) {
      notify.error('Validation manager obligatoire')
      return
    }
    setSubmitting(true)
    try {
      const result = await posApi.closeSession({
        closingCashAmount: declaredNum,
        differenceReason: needsReason ? differenceReason : undefined,
        differenceComment: differenceComment || undefined,
        managerEmail: needsManager ? managerEmail.trim() : undefined,
        managerPassword: needsManager ? managerPassword : undefined,
      })
      setReport(result)
      notify.success(result.balanced ? 'Caisse équilibrée — session fermée' : 'Session fermée avec écart enregistré')
      onClosed?.(result)
    } catch (e) {
      notify.error(getErrorMessage(e))
    } finally {
      setSubmitting(false)
    }
  }

  if (report) {
    const style = differenceSeverityStyle(report.differenceSeverity)
    return (
      <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-4">
        <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-lg max-h-[90vh] overflow-auto">
          <div className="p-6 border-b border-slate-800">
            <h2 className="text-xl font-semibold">Rapport de clôture</h2>
            <p className="text-sm text-slate-400 mt-1">Session {report.sessionNumber}</p>
          </div>
          <div className="p-6 space-y-4 text-sm">
            <div className={`rounded-xl border p-4 text-center ${style.bg}`}>
              <p className={`text-xs uppercase tracking-wide ${style.text}`}>{style.label}</p>
              <p className="text-3xl font-bold mt-2">{formatPosMoney(report.expectedCashAmount, currency)}</p>
              <p className="text-xs text-slate-400 mt-1">Cash attendu</p>
            </div>
            <SummaryRow label="Caissier" value={report.cashierName} />
            <SummaryRow label="Ouverture" value={formatPosDateTime(report.openedAt)} />
            <SummaryRow label="Fermeture" value={formatPosDateTime(report.closedAt)} />
            <SummaryRow label="Fond initial" value={formatPosMoney(report.openingCashAmount, currency)} />
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
            {report.differenceComment && (
              <p className="text-xs text-slate-400 bg-slate-800 rounded-lg p-3">{report.differenceComment}</p>
            )}
          </div>
          <div className="p-6 border-t border-slate-800 flex justify-end">
            <button type="button" onClick={onClose}
              className="px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 rounded-lg text-sm font-medium">
              Terminer
            </button>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="fixed inset-0 bg-black/80 flex items-center justify-center z-50 p-4">
      <div className="bg-slate-900 border border-slate-700 rounded-2xl w-full max-w-2xl max-h-[95vh] overflow-auto">
        <div className="p-6 border-b border-slate-800">
          <h2 className="text-xl font-semibold">Clôture de caisse</h2>
          <p className="text-sm text-slate-400 mt-1">
            Session {session?.sessionNumber} · {formatPosDateTime(session?.openedAt)}
          </p>
        </div>

        {loading ? (
          <p className="p-8 text-slate-400 text-sm">Calcul des totaux…</p>
        ) : (
          <div className="p-6 space-y-6">
            <section>
              <h3 className="text-xs font-semibold uppercase tracking-wide text-slate-500 mb-3">Résumé activité</h3>
              <div className="grid grid-cols-2 gap-x-6 gap-y-1 text-sm bg-slate-800/50 rounded-xl p-4">
                <SummaryRow label="Nombre de ventes" value={preview?.saleCount ?? 0} />
                <SummaryRow label="Montant total ventes" value={formatPosMoney(preview?.totalRevenue, currency)} />
                <SummaryRow label="Espèces" value={formatPosMoney(preview?.cashRevenue, currency)} />
                <SummaryRow label="Carte" value={formatPosMoney(preview?.cardRevenue, currency)} />
                <SummaryRow label="Mobile money" value={formatPosMoney(preview?.mobileMoneyRevenue, currency)} />
                <SummaryRow label="Virement" value={formatPosMoney(preview?.bankTransferRevenue, currency)} />
                <SummaryRow label="Remboursements cash" value={formatPosMoney(preview?.cashRefundTotal, currency)} />
              </div>
            </section>

            <section className="grid md:grid-cols-2 gap-4">
              <div className="rounded-xl border border-slate-700 p-4">
                <p className="text-xs text-slate-500">Fond initial</p>
                <p className="text-xl font-semibold mt-1">{formatPosMoney(preview?.openingCashAmount, currency)}</p>
              </div>
              <div className={`rounded-xl border p-4 ${
                liveSeverity === 'BALANCED' ? 'border-emerald-700 bg-emerald-950/40'
                  : liveSeverity === 'MINOR' ? 'border-amber-700 bg-amber-950/40'
                    : liveSeverity === 'MAJOR' ? 'border-red-700 bg-red-950/40'
                      : 'border-emerald-700 bg-emerald-950/40'
              }`}>
                <p className="text-xs text-slate-400">Cash attendu</p>
                <p className="text-3xl font-bold mt-1 text-white">{formatPosMoney(preview?.expectedCashAmount, currency)}</p>
                <p className="text-xs text-slate-500 mt-2">Fond + espèces − remboursements cash</p>
              </div>
            </section>

            <section>
              <label className="block">
                <span className="text-sm font-medium text-white">Cash réellement présent dans la caisse *</span>
                <input
                  type="number"
                  step="0.01"
                  min="0"
                  autoFocus
                  className="mt-2 w-full rounded-xl px-4 py-3 text-lg font-semibold border border-slate-600 bg-slate-800"
                  value={declaredCash}
                  onChange={(e) => setDeclaredCash(e.target.value)}
                />
              </label>
              {liveDifference != null && (
                <p className={`mt-2 text-sm ${
                  liveSeverity === 'BALANCED' ? 'text-emerald-400'
                    : liveSeverity === 'MINOR' ? 'text-amber-400' : 'text-red-400'
                }`}>
                  Écart calculé : {formatPosMoney(liveDifference, currency)}
                  {liveSeverity === 'BALANCED' && ' — caisse équilibrée'}
                </p>
              )}
            </section>

            {needsReason && (
              <section className="space-y-3 rounded-xl border border-amber-800/50 bg-amber-950/20 p-4">
                <p className="text-sm text-amber-200 font-medium">Écart détecté — justification obligatoire</p>
                <label className="block">
                  <span className="text-xs text-slate-400">Motif de l&apos;écart *</span>
                  <select
                    className="mt-1 w-full rounded-lg px-3 py-2 border border-slate-600 bg-slate-800 text-sm"
                    value={differenceReason}
                    onChange={(e) => setDifferenceReason(e.target.value)}
                  >
                    <option value="">Choisir…</option>
                    {(preview?.differenceReasonOptions ?? []).map((opt) => (
                      <option key={opt.code} value={opt.code}>{opt.label}</option>
                    ))}
                  </select>
                </label>
                <label className="block">
                  <span className="text-xs text-slate-400">Commentaire libre</span>
                  <textarea
                    className="mt-1 w-full rounded-lg px-3 py-2 border border-slate-600 bg-slate-800 text-sm min-h-[72px]"
                    value={differenceComment}
                    onChange={(e) => setDifferenceComment(e.target.value)}
                    placeholder="Précisions éventuelles…"
                  />
                </label>
              </section>
            )}

            {needsManager && (
              <section className="space-y-3 rounded-xl border border-red-800/50 bg-red-950/20 p-4">
                <p className="text-sm text-red-200 font-medium">Validation manager obligatoire</p>
                <label className="block">
                  <span className="text-xs text-slate-400">Email manager</span>
                  <input
                    type="email"
                    className="mt-1 w-full rounded-lg px-3 py-2 border border-slate-600 bg-slate-800 text-sm"
                    value={managerEmail}
                    onChange={(e) => setManagerEmail(e.target.value)}
                  />
                </label>
                <label className="block">
                  <span className="text-xs text-slate-400">Mot de passe manager</span>
                  <input
                    type="password"
                    className="mt-1 w-full rounded-lg px-3 py-2 border border-slate-600 bg-slate-800 text-sm"
                    value={managerPassword}
                    onChange={(e) => setManagerPassword(e.target.value)}
                  />
                </label>
              </section>
            )}
          </div>
        )}

        <div className="p-6 border-t border-slate-800 flex gap-3 justify-end">
          <button type="button" onClick={onClose} disabled={submitting}
            className="px-4 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-sm">
            Annuler
          </button>
          <button type="button" onClick={submit} disabled={loading || submitting}
            className="px-5 py-2.5 rounded-lg bg-red-700 hover:bg-red-600 text-sm font-medium disabled:opacity-50">
            {submitting ? 'Clôture…' : 'Valider la clôture'}
          </button>
        </div>
      </div>
    </div>
  )
}
