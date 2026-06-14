-- Traçabilité complète des annulations de vente

ALTER TABLE sales ADD COLUMN IF NOT EXISTS created_by_id BIGINT REFERENCES users(id);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS updated_by_id BIGINT REFERENCES users(id);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS cancelled_by_id BIGINT REFERENCES users(id);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS cancellation_reason VARCHAR(50);
ALTER TABLE sales ADD COLUMN IF NOT EXISTS cancellation_comment VARCHAR(2000);

CREATE INDEX IF NOT EXISTS idx_sales_cancelled_at ON sales(cancelled_at);
CREATE INDEX IF NOT EXISTS idx_sales_cancellation_reason ON sales(cancellation_reason);

CREATE TABLE IF NOT EXISTS sale_events (
    id              BIGSERIAL PRIMARY KEY,
    sale_id         BIGINT NOT NULL REFERENCES sales(id) ON DELETE CASCADE,
    event_type      VARCHAR(50) NOT NULL,
    description     VARCHAR(500),
    details         VARCHAR(2000),
    actor_id        BIGINT REFERENCES users(id),
    actor_name      VARCHAR(255),
    occurred_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_sale_events_sale ON sale_events(sale_id, occurred_at);
