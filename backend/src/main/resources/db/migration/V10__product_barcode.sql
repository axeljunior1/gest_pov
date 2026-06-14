ALTER TABLE products ADD COLUMN IF NOT EXISTS code_barre VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uk_products_code_barre ON products (code_barre);
