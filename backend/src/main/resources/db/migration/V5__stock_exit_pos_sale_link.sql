-- Lien sortie stock ↔ vente POS (une sortie par vente payée)
ALTER TABLE stock_exits ADD COLUMN sale_id bigint;

ALTER TABLE stock_exits
    ADD CONSTRAINT fk_stock_exits_sale
        FOREIGN KEY (sale_id) REFERENCES sales (id);

CREATE UNIQUE INDEX uk_stock_exits_sale_id ON stock_exits (sale_id) WHERE sale_id IS NOT NULL;
