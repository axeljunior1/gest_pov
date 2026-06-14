import ProductsPage from '../pages/ProductsPage'

/** Liste produits — route `/` (déjà protégée par products.read). */
export default function HomeRedirect() {
  return <ProductsPage />
}
