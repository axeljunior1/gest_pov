-- Methodes de valorisation stock manquantes (V3 n'incluait pas cette categorie, V18 n'a ajoute que WEIGHTED_AVERAGE)

INSERT INTO app_reference_values (category, code, label, sort_order, active)
SELECT 'STOCK_VALUATION_METHOD', 'PURCHASE_COST', 'Prix d''achat (PA)', 1, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM app_reference_values
    WHERE category = 'STOCK_VALUATION_METHOD' AND code = 'PURCHASE_COST'
);

INSERT INTO app_reference_values (category, code, label, sort_order, active)
SELECT 'STOCK_VALUATION_METHOD', 'SALE_PRICE', 'Prix de vente (PV)', 2, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM app_reference_values
    WHERE category = 'STOCK_VALUATION_METHOD' AND code = 'SALE_PRICE'
);
