-- V13 : Jeu de donnees de demonstration ERP (catalogue, stock, clients, ventes POS)
-- Le chargement metier complet est execute au demarrage par DemoDatasetInitializer
-- (apres creation des utilisateurs systeme et des unites de mesure).

CREATE TABLE IF NOT EXISTS demo_dataset_meta (
    id SMALLINT PRIMARY KEY DEFAULT 1 CHECK (id = 1),
    migration_version VARCHAR(10) NOT NULL,
    seeded_at TIMESTAMPTZ,
    note VARCHAR(500)
);

INSERT INTO demo_dataset_meta (migration_version, note)
VALUES ('13', 'Catalogue demo — DemoDatasetInitializer au premier demarrage post-migration')
ON CONFLICT (id) DO NOTHING;

INSERT INTO warehouses (code, nom, adresse, actif, created_at, updated_at)
SELECT 'WH-MAIN', 'Entrepot principal', 'Site principal', true, now(), now()
WHERE NOT EXISTS (SELECT 1 FROM warehouses WHERE code = 'WH-MAIN');

INSERT INTO locations (warehouse_id, code, nom, actif, created_at, updated_at)
SELECT w.id, 'DEFAULT', 'Zone par defaut', true, now(), now()
FROM warehouses w
WHERE w.code = 'WH-MAIN'
  AND NOT EXISTS (
    SELECT 1 FROM locations l WHERE l.warehouse_id = w.id AND l.code = 'DEFAULT'
  );
