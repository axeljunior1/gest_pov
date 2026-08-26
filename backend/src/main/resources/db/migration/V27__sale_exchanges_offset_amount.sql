ALTER TABLE sale_exchanges ADD COLUMN offset_amount NUMERIC(19,4) NOT NULL DEFAULT 0;
ALTER TABLE sale_exchanges ALTER COLUMN offset_amount DROP DEFAULT;
