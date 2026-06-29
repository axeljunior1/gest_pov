import { expect } from '@playwright/test'
import { defaultAdmin } from './auth.js'

const API_BASE = process.env.PLAYWRIGHT_API_URL || 'http://localhost:8080'
export const DEV_RESET_TOKEN = process.env.PLAYWRIGHT_RESET_TOKEN || 'dev-reset-token-change-me'

async function adminAuthHeaders(request) {
  const loginRes = await request.post(`${API_BASE}/api/auth/login`, {
    data: { email: defaultAdmin.email, password: defaultAdmin.password },
  })
  expect(loginRes.ok()).toBeTruthy()
  const { token } = await loginRes.json()
  return { Authorization: `Bearer ${token}` }
}

/** Purge les données métier puis recharge le jeu de démo (profil dev requis). */
export async function purgeDemoData(request) {
  const headers = {
    ...(await adminAuthHeaders(request)),
    'X-Reset-Token': DEV_RESET_TOKEN,
  }
  const resetRes = await request.post(`${API_BASE}/api/admin/reset-demo`, { headers })
  expect(resetRes.ok()).toBeTruthy()
  const seedRes = await request.post(`${API_BASE}/api/admin/seed-demo`, { headers })
  expect(seedRes.ok()).toBeTruthy()
}
