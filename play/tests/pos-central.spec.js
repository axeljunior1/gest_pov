import { test, expect } from '@playwright/test'
import { loginAsCashier, loginAsSeller } from '../helpers/auth.js'
import { purgeDemoData } from '../helpers/dev-reset.js'
import {
  addProductToCart,
  cartAside,
  collectFirstPendingPayment,
  collectPendingPayment,
  currentSaleNumber,
  expectSaleNotInPendingQueue,
  openSellerSession,
  pauseClientCart,
  resumeLatestPausedSale,
  sendCartToCashier,
} from '../helpers/pos.js'

/** Produit seed demo sans choix de conditionnement obligatoire. */
const POS_DEMO_PRODUCT = 'Cafe moulu 250g'

test.describe.configure({ mode: 'serial', timeout: 90_000 })

test.describe('POS — caisse centrale', () => {
  test.beforeAll(async ({ request }) => {
    await purgeDemoData(request)
  })

  test('vendeur envoie le panier, caissier encaisse', async ({ browser }) => {
    const sellerContext = await browser.newContext()
    const cashierContext = await browser.newContext()
    const sellerPage = await sellerContext.newPage()
    const cashierPage = await cashierContext.newPage()

    await loginAsSeller(sellerPage)
    await openSellerSession(sellerPage)
    await addProductToCart(sellerPage, POS_DEMO_PRODUCT)
    const saleNumber = await currentSaleNumber(sellerPage)
    await expect(sellerPage.getByRole('button', { name: /Envoyer à la caisse/ })).toBeVisible()
    await expect(sellerPage.getByRole('button', { name: /^Paiement \(F4\)$/ })).toHaveCount(0)
    await sendCartToCashier(sellerPage)

    await loginAsCashier(cashierPage)
    await collectPendingPayment(cashierPage, saleNumber)
    await expect(cashierPage.getByText(/encaissée/i)).toBeVisible()

    await sellerContext.close()
    await cashierContext.close()
  })

  test('pause client (F8) ne part pas en caisse', async ({ browser }) => {
    const sellerContext = await browser.newContext()
    const cashierContext = await browser.newContext()
    const sellerPage = await sellerContext.newPage()
    const cashierPage = await cashierContext.newPage()

    await loginAsSeller(sellerPage)
    await openSellerSession(sellerPage)
    await addProductToCart(sellerPage, POS_DEMO_PRODUCT)
    const saleNumber = await currentSaleNumber(sellerPage)
    await pauseClientCart(sellerPage)

    await loginAsCashier(cashierPage)
    await expectSaleNotInPendingQueue(cashierPage, saleNumber)

    await resumeLatestPausedSale(sellerPage)

    await sellerContext.close()
    await cashierContext.close()
  })
})
