-- Purge des donnees metier ERP (conserve referentiel systeme)
-- Conserve : users, roles, permissions, app_settings, units, warehouses, locations, alert_rules

BEGIN;

DELETE FROM notifications;
DELETE FROM alerts;

DELETE FROM sale_refund_lines;
DELETE FROM sale_refunds;
DELETE FROM payments;
DELETE FROM loyalty_transactions;
DELETE FROM sale_lines;
DELETE FROM sales;
DELETE FROM pos_sessions;

DELETE FROM inventory_count_lines;
DELETE FROM inventory_counts;
DELETE FROM stock_reservations;
DELETE FROM stock_movements;
DELETE FROM stock_entry_lines;
DELETE FROM stock_entries;
DELETE FROM stock_exit_lines;
DELETE FROM stock_exits;
DELETE FROM stock_transfer_lines;
DELETE FROM stock_transfers;
DELETE FROM stock_items;
DELETE FROM lots;

DELETE FROM alert_settings;

DELETE FROM product_custom_attribute_values;
DELETE FROM product_documents;
DELETE FROM product_images;
DELETE FROM product_packagings;
DELETE FROM product_suppliers;
DELETE FROM price_history;
DELETE FROM product_variants;
DELETE FROM supplier_purchase_orders;
DELETE FROM products;

-- categories hierarchiques : enfants d'abord
DELETE FROM categories WHERE parent_id IS NOT NULL;
DELETE FROM categories;

DELETE FROM customers;
DELETE FROM suppliers;
DELETE FROM custom_attribute_definitions;
DELETE FROM import_jobs;
DELETE FROM audit_logs;

COMMIT;
