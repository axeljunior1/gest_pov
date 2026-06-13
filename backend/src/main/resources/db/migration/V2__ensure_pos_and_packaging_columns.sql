-- Securise les bases existantes baselinees sans rejouer V1 (idempotent).

ALTER TABLE pos_sessions
    ADD COLUMN IF NOT EXISTS session_type varchar(20) NOT NULL DEFAULT 'CASHIER';

ALTER TABLE sales ADD COLUMN IF NOT EXISTS seller_id bigint;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS paid_at timestamp with time zone;
ALTER TABLE sales ADD COLUMN IF NOT EXISTS submitted_at timestamp with time zone;

UPDATE sales SET seller_id = cashier_id
WHERE seller_id IS NULL AND cashier_id IS NOT NULL;

UPDATE sales SET paid_at = validated_at
WHERE paid_at IS NULL AND validated_at IS NOT NULL;

ALTER TABLE payments ADD COLUMN IF NOT EXISTS cashier_id bigint;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS pos_session_id bigint;

UPDATE payments p
SET cashier_id = s.cashier_id,
    pos_session_id = COALESCE(s.payment_session_id, s.pos_session_id)
FROM sales s
WHERE p.sale_id = s.id
  AND (p.cashier_id IS NULL OR p.pos_session_id IS NULL);

ALTER TABLE product_packagings ADD COLUMN IF NOT EXISTS prix_vente numeric(19,4);
ALTER TABLE product_packagings ADD COLUMN IF NOT EXISTS prix_achat numeric(19,4);
ALTER TABLE product_packagings ADD COLUMN IF NOT EXISTS default_vente boolean NOT NULL DEFAULT false;
ALTER TABLE product_packagings ADD COLUMN IF NOT EXISTS default_achat boolean NOT NULL DEFAULT false;
ALTER TABLE product_packagings ADD COLUMN IF NOT EXISTS actif boolean NOT NULL DEFAULT true;

UPDATE product_packagings pp
SET prix_vente = COALESCE(p.prix_vente, 0) * pp.quantite_base
FROM products p
WHERE pp.product_id = p.id
  AND (pp.prix_vente IS NULL OR pp.prix_vente = 0);

UPDATE product_packagings SET default_achat = true WHERE principal = true AND default_achat = false;
UPDATE product_packagings SET actif = true WHERE actif IS NULL;

ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS packaging_name_snapshot varchar(255);
ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS packaging_quantity_snapshot numeric(19,6);
ALTER TABLE sale_lines ADD COLUMN IF NOT EXISTS unit_price_snapshot numeric(19,4);

UPDATE sale_lines sl
SET packaging_name_snapshot = pp.nom,
    packaging_quantity_snapshot = pp.quantite_base,
    unit_price_snapshot = sl.unit_price
FROM product_packagings pp
WHERE sl.packaging_id = pp.id
  AND sl.packaging_name_snapshot IS NULL;

UPDATE sale_lines SET unit_price_snapshot = unit_price WHERE unit_price_snapshot IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = 'public' AND table_name = 'sales' AND constraint_name = 'sales_status_check'
    ) THEN
        ALTER TABLE sales DROP CONSTRAINT sales_status_check;
    END IF;
    ALTER TABLE sales ADD CONSTRAINT sales_status_check CHECK (status IN (
        'DRAFT', 'HOLD', 'PENDING_PAYMENT', 'PAID', 'VALIDATED',
        'CANCELLED', 'REFUNDED', 'PARTIALLY_REFUNDED'
    ));
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
