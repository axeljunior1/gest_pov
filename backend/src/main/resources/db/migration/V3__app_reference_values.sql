-- Listes de valeurs applicatives (devise, langue, fuseau, format date, flux POS)

CREATE TABLE IF NOT EXISTS app_reference_values (
    id bigserial PRIMARY KEY,
    category varchar(50) NOT NULL,
    code varchar(50) NOT NULL,
    label varchar(120) NOT NULL,
    sort_order integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT uk_app_ref_category_code UNIQUE (category, code)
);

INSERT INTO app_reference_values (category, code, label, sort_order, active) VALUES
('CURRENCY', 'EUR', 'Euro (EUR)', 1, true),
('CURRENCY', 'USD', 'Dollar US (USD)', 2, true),
('CURRENCY', 'GBP', 'Livre sterling (GBP)', 3, true),
('CURRENCY', 'CHF', 'Franc suisse (CHF)', 4, true),
('CURRENCY', 'XOF', 'Franc CFA UEMOA (XOF)', 5, true),
('CURRENCY', 'XAF', 'Franc CFA CEMAC (XAF)', 6, true),
('CURRENCY', 'MAD', 'Dirham marocain (MAD)', 7, true),
('CURRENCY', 'TND', 'Dinar tunisien (TND)', 8, true),
('CURRENCY', 'DZD', 'Dinar algérien (DZD)', 9, true),
('CURRENCY', 'CAD', 'Dollar canadien (CAD)', 10, true),
('LANGUAGE', 'fr', 'Français', 1, true),
('LANGUAGE', 'en', 'English', 2, true),
('LANGUAGE', 'es', 'Español', 3, true),
('LANGUAGE', 'de', 'Deutsch', 4, true),
('LANGUAGE', 'pt', 'Português', 5, true),
('LANGUAGE', 'ar', 'العربية', 6, true),
('TIMEZONE', 'Europe/Paris', 'Europe/Paris (France)', 1, true),
('TIMEZONE', 'Europe/London', 'Europe/London (UK)', 2, true),
('TIMEZONE', 'Africa/Abidjan', 'Africa/Abidjan (UEMOA)', 3, true),
('TIMEZONE', 'Africa/Dakar', 'Africa/Dakar (Sénégal)', 4, true),
('TIMEZONE', 'Africa/Casablanca', 'Africa/Casablanca (Maroc)', 5, true),
('TIMEZONE', 'Africa/Lagos', 'Africa/Lagos (Nigeria)', 6, true),
('TIMEZONE', 'Africa/Douala', 'Africa/Douala (Cameroun)', 7, true),
('TIMEZONE', 'UTC', 'UTC', 8, true),
('DATE_FORMAT', 'dd/MM/yyyy', 'jj/mm/aaaa (dd/MM/yyyy)', 1, true),
('DATE_FORMAT', 'MM/dd/yyyy', 'mm/jj/aaaa (MM/dd/yyyy)', 2, true),
('DATE_FORMAT', 'yyyy-MM-dd', 'aaaa-mm-jj (yyyy-MM-dd)', 3, true),
('DATE_FORMAT', 'dd-MM-yyyy', 'jj-mm-aaaa (dd-MM-yyyy)', 4, true),
('POS_SALES_FLOW_MODE', 'SELLER_COLLECTS_PAYMENT', 'Vendeur encaisseur', 1, true),
('POS_SALES_FLOW_MODE', 'CENTRAL_CASHIER', 'Caisse centrale', 2, true)
ON CONFLICT (category, code) DO NOTHING;
