-- Devise CDF (Franc congolais) + neutralisation devise par defaut pour nouvelles installations

INSERT INTO app_reference_values (category, code, label, sort_order, active)
SELECT 'CURRENCY', 'CDF', 'Franc congolais (CDF)', 11, TRUE
WHERE NOT EXISTS (
    SELECT 1 FROM app_reference_values WHERE category = 'CURRENCY' AND code = 'CDF'
);
