-- Flags vendable/stockable parent + variantes, snapshots panier

ALTER TABLE products ADD COLUMN IF NOT EXISTS has_variants boolean NOT NULL DEFAULT false;
ALTER TABLE products ADD COLUMN IF NOT EXISTS is_stockable boolean NOT NULL DEFAULT true;

UPDATE products p
SET has_variants = true,
    is_sellable = false,
    is_stockable = false
WHERE EXISTS (SELECT 1 FROM product_variants pv WHERE pv.product_id = p.id);

ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS name character varying(255);
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS is_sellable boolean NOT NULL DEFAULT true;
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS is_stockable boolean NOT NULL DEFAULT true;
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true;
ALTER TABLE product_variants ADD COLUMN IF NOT EXISTS cost_price numeric(19, 4);

UPDATE product_variants pv
SET name = COALESCE(
    NULLIF(TRIM(COALESCE(pv.couleur, '') || CASE WHEN pv.couleur IS NOT NULL AND pv.taille IS NOT NULL THEN ' ' ELSE '' END || COALESCE(pv.taille, '')), ''),
    pv.sku
)
WHERE pv.name IS NULL OR TRIM(pv.name) = '';

ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS product_name_snapshot character varying(255);
ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS variant_name_snapshot character varying(255);

UPDATE sale_lines sl
SET product_name_snapshot = p.nom
FROM products p
WHERE sl.product_id = p.id AND sl.product_name_snapshot IS NULL;

UPDATE sale_lines sl
SET variant_name_snapshot = COALESCE(
    NULLIF(TRIM(COALESCE(pv.couleur, '') || CASE WHEN pv.couleur IS NOT NULL AND pv.taille IS NOT NULL THEN ' ' ELSE '' END || COALESCE(pv.taille, '')), ''),
    pv.sku
)
FROM product_variants pv
WHERE sl.variant_id = pv.id AND sl.variant_name_snapshot IS NULL;
