import { describe, expect, it } from 'vitest'
import {
  getErrorMessage,
  getFieldErrors,
  getErrorDetails,
  validateClientConfig,
  validateLogoFile,
  SESSION_EXPIRED_MESSAGE,
} from './errors'

function axiosError(status, data = {}, code) {
  return {
    response: { status, data },
    message: `Request failed with status code ${status}`,
    code,
  }
}

describe('getErrorMessage', () => {
  it('traduit une erreur 401 hors login en session expirée', () => {
    expect(getErrorMessage(axiosError(401))).toBe(SESSION_EXPIRED_MESSAGE)
  })

  it('traduit une erreur login 401', () => {
    expect(getErrorMessage(axiosError(401), { module: 'login' }))
      .toBe('Email ou mot de passe incorrect.')
  })

  it('ne renvoie pas de message technique axios brut', () => {
    expect(getErrorMessage(axiosError(500))).toBe(
      'Une erreur technique est survenue. Réessayez dans quelques instants.',
    )
  })

  it('mappe le stock insuffisant POS', () => {
    const err = axiosError(400, { message: 'Stock insuffisant pour Café (disponible: 2)' })
    expect(getErrorMessage(err, { module: 'pos' })).toContain('Stock insuffisant')
  })

  it('mappe moyen de paiement non autorisé', () => {
    const err = axiosError(400, { message: 'Moyen de paiement non autorise: CARD' })
    expect(getErrorMessage(err, { module: 'pos' })).toContain('Moyen de paiement non autorisé')
  })

  it('mappe vente déjà payée', () => {
    expect(getErrorMessage(axiosError(400, { message: 'Cette vente est deja payee' }), { module: 'pos' }))
      .toBe('Cette vente est déjà payée.')
  })

  it('gère les erreurs réseau', () => {
    expect(getErrorMessage({ code: 'ERR_NETWORK', message: 'Network Error' }))
      .toContain('Impossible de joindre le serveur')
  })

  it('agrège les erreurs de validation champ', () => {
    const err = axiosError(400, { errors: { name: 'must not be blank', email: 'invalid' } })
    expect(getErrorMessage(err)).toContain('Vérifiez les champs suivants')
  })
})

describe('getFieldErrors', () => {
  it('retourne null sans erreurs champ', () => {
    expect(getFieldErrors(axiosError(400, { message: 'Erreur' }))).toBeNull()
  })

  it('retourne les erreurs champ', () => {
    const fields = getFieldErrors(axiosError(400, { errors: { 'company.name': 'must not be blank' } }))
    expect(fields['company.name']).toBeTruthy()
  })
})

describe('validateClientConfig', () => {
  it('exige le nom entreprise', () => {
    const errors = validateClientConfig({ company: { name: '  ' }, pos: { paymentMethods: [{ enabled: true }] } })
    expect(errors['company.name']).toBeTruthy()
  })

  it('valide le taux de taxe', () => {
    const errors = validateClientConfig({
      company: { name: 'Test' },
      tax: { defaultRate: 150 },
      pos: { paymentMethods: [{ enabled: true }] },
    })
    expect(errors['tax.defaultRate']).toBeTruthy()
  })
})

describe('validateLogoFile', () => {
  it('refuse un fichier non image', () => {
    const file = { type: 'application/pdf', size: 1000 }
    expect(validateLogoFile(file)).toContain('image')
  })

  it('refuse un fichier trop lourd', () => {
    const file = { type: 'image/png', size: 6 * 1024 * 1024 }
    expect(validateLogoFile(file)).toContain('volumineux')
  })
})

describe('getErrorDetails', () => {
  it('expose message et statut', () => {
    const details = getErrorDetails(axiosError(403))
    expect(details.status).toBe(403)
    expect(details.message).toContain('autorisation')
  })
})
