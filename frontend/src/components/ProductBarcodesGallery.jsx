import { useEffect, useMemo, useState } from 'react'
import { barcodesApi } from '../api'

function collectBarcodeEntries(product) {
  if (!product) return []

  if (product.hasVariants && product.variantes?.length) {
    return product.variantes
      .filter((v) => v.codeBarre?.trim())
      .map((v) => ({
        key: String(v.id),
        label: v.label || [v.couleur, v.taille].filter(Boolean).join(' · ') || v.sku || 'Variante',
        code: v.codeBarre.trim(),
        type: v.barcodeType || 'EAN13',
        imageBase64: v.barcodeImageBase64 || null,
      }))
  }

  if (product.codeBarre?.trim()) {
    return [{
      key: 'product',
      label: product.nom || 'Produit',
      code: product.codeBarre.trim(),
      type: 'EAN13',
      imageBase64: product.barcodeImageBase64 || null,
    }]
  }

  return []
}

export default function ProductBarcodesGallery({ product }) {
  const entries = useMemo(() => collectBarcodeEntries(product), [product])
  const [images, setImages] = useState({})

  useEffect(() => {
    if (!entries.length) {
      setImages({})
      return undefined
    }

    let cancelled = false

    const load = async () => {
      const next = {}
      await Promise.all(entries.map(async (entry) => {
        if (entry.imageBase64) {
          next[entry.key] = entry.imageBase64
          return
        }
        try {
          const result = await barcodesApi.generate({ content: entry.code, type: entry.type })
          next[entry.key] = result.imageBase64
        } catch {
          next[entry.key] = null
        }
      }))
      if (!cancelled) setImages(next)
    }

    load()
    return () => { cancelled = true }
  }, [entries])

  if (!entries.length) return null

  return (
    <div className="pt-5 border-t border-gray-100">
      <h3 className="text-sm font-medium text-gray-700 mb-1">Codes-barres</h3>
      <p className="text-xs text-gray-400 mb-4">
        {product.hasVariants
          ? 'Un code-barres par variante — prêt pour impression ou étiquetage.'
          : 'Code-barres du produit — prêt pour impression ou étiquetage.'}
      </p>
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
        {entries.map((entry) => (
          <div
            key={entry.key}
            className="rounded-xl border border-gray-200 bg-white p-4 flex flex-col items-center text-center"
          >
            <p className="text-sm font-medium text-gray-800 truncate w-full">{entry.label}</p>
            <p className="text-xs font-mono text-gray-500 mt-1 mb-3">{entry.code}</p>
            {images[entry.key] ? (
              <img
                src={`data:image/png;base64,${images[entry.key]}`}
                alt={`Code-barres ${entry.code}`}
                className="max-h-28 w-full object-contain"
              />
            ) : images[entry.key] === null ? (
              <p className="text-xs text-gray-400 py-6">Impossible de générer l&apos;image</p>
            ) : (
              <p className="text-xs text-gray-400 py-6">Chargement…</p>
            )}
          </div>
        ))}
      </div>
    </div>
  )
}
