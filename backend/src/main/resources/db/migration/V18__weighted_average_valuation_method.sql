INSERT INTO app_reference_values (category, code, label, sort_order, active)
SELECT 'STOCK_VALUATION_METHOD', 'WEIGHTED_AVERAGE', 'Coût moyen pondéré (CMP)', 3, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM app_reference_values
    WHERE category = 'STOCK_VALUATION_METHOD' AND code = 'WEIGHTED_AVERAGE'
);
