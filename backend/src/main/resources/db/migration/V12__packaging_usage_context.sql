ALTER TABLE product_packagings
    ADD COLUMN usable_for_sale BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE product_packagings
    ADD COLUMN usable_for_purchase BOOLEAN NOT NULL DEFAULT TRUE;

-- Conditionnements historiquement marqués achat uniquement (principal sans vente)
UPDATE product_packagings
SET usable_for_sale = FALSE
WHERE default_achat = TRUE
  AND default_vente = FALSE
  AND principal = TRUE;
