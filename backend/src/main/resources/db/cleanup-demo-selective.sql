-- Suppression sélective des données de démonstration uniquement.
-- Marqueurs : SKU DEMO-*, clients CUST-DEMO-* / @demo.local, fournisseurs @demo.local, catégories « demo ».

BEGIN;

CREATE TEMP TABLE tmp_demo_products ON COMMIT DROP AS
    SELECT id FROM products WHERE sku LIKE 'DEMO-%';

CREATE TEMP TABLE tmp_demo_customers ON COMMIT DROP AS
    SELECT id FROM customers
    WHERE customer_number LIKE 'CUST-DEMO-%'
       OR LOWER(email) LIKE '%@demo.local';

CREATE TEMP TABLE tmp_demo_sales ON COMMIT DROP AS
    SELECT DISTINCT s.id
    FROM sales s
    LEFT JOIN sale_lines sl ON sl.sale_id = s.id
    LEFT JOIN tmp_demo_products dp ON sl.product_id = dp.id
    LEFT JOIN tmp_demo_customers dc ON s.customer_id = dc.id
    WHERE dp.id IS NOT NULL OR dc.id IS NOT NULL;

CREATE TEMP TABLE tmp_demo_sessions ON COMMIT DROP AS
    SELECT DISTINCT ps.id
    FROM pos_sessions ps
    WHERE ps.id IN (SELECT pos_session_id FROM sales WHERE id IN (SELECT id FROM tmp_demo_sales) AND pos_session_id IS NOT NULL);

-- Retours liés aux ventes demo
DELETE FROM refund_payments
WHERE refund_id IN (SELECT sr.id FROM sale_refunds sr WHERE sr.sale_id IN (SELECT id FROM tmp_demo_sales));

DELETE FROM sale_refund_lines
WHERE refund_id IN (SELECT sr.id FROM sale_refunds sr WHERE sr.sale_id IN (SELECT id FROM tmp_demo_sales));

DELETE FROM sale_refunds WHERE sale_id IN (SELECT id FROM tmp_demo_sales);

DELETE FROM payments WHERE sale_id IN (SELECT id FROM tmp_demo_sales);
DELETE FROM loyalty_transactions WHERE sale_id IN (SELECT id FROM tmp_demo_sales);
DELETE FROM sale_lines WHERE sale_id IN (SELECT id FROM tmp_demo_sales);
DELETE FROM stock_movements
WHERE product_id IN (SELECT id FROM tmp_demo_products)
   OR reference_type = 'DEMO_SEED'
   OR utilisateur = 'demo-seed';

DELETE FROM sales WHERE id IN (SELECT id FROM tmp_demo_sales);
DELETE FROM pos_sessions WHERE id IN (SELECT id FROM tmp_demo_sessions);

DELETE FROM stock_reservations WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM stock_items WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM lots WHERE product_id IN (SELECT id FROM tmp_demo_products);

DELETE FROM product_custom_attribute_values WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM product_documents WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM product_images WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM product_packagings WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM product_suppliers WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM price_history WHERE product_id IN (SELECT id FROM tmp_demo_products);

DELETE FROM product_variant_attribute_values
WHERE variant_id IN (SELECT id FROM product_variants WHERE product_id IN (SELECT id FROM tmp_demo_products));

DELETE FROM product_variants WHERE product_id IN (SELECT id FROM tmp_demo_products);
DELETE FROM products WHERE id IN (SELECT id FROM tmp_demo_products);

DELETE FROM customers WHERE id IN (SELECT id FROM tmp_demo_customers);

DELETE FROM suppliers
WHERE LOWER(email) LIKE '%@demo.local'
   OR nom LIKE '% Demo %'
   OR nom LIKE '% demo'
   OR nom LIKE '%Demo SA%'
   OR nom LIKE '%Demo SARL%';

-- Catégories demo (feuilles puis parents)
DELETE FROM categories WHERE nom LIKE '% demo' AND id NOT IN (
    SELECT DISTINCT parent_id FROM categories WHERE parent_id IS NOT NULL
);
DELETE FROM categories WHERE nom LIKE '% demo';

UPDATE demo_dataset_meta SET seeded_at = NULL WHERE id = 1;

COMMIT;
