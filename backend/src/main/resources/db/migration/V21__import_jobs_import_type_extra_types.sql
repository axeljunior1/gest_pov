-- La contrainte d'origine (V1) ne listait que PRODUCTS/PACKAGINGS/INITIAL_STOCK.
-- L'enum Java ImportType a depuis ete etendu (BRANDS, CATEGORIES, SUPPLIERS, UNITS, WAREHOUSES)
-- sans que la contrainte CHECK soit mise a jour, ce qui faisait echouer ces imports en base.
ALTER TABLE import_jobs DROP CONSTRAINT import_jobs_import_type_check;

ALTER TABLE import_jobs ADD CONSTRAINT import_jobs_import_type_check
    CHECK (((import_type)::text = ANY ((ARRAY[
        'PRODUCTS'::character varying,
        'PACKAGINGS'::character varying,
        'INITIAL_STOCK'::character varying,
        'BRANDS'::character varying,
        'CATEGORIES'::character varying,
        'SUPPLIERS'::character varying,
        'UNITS'::character varying,
        'WAREHOUSES'::character varying
    ])::text[])));
