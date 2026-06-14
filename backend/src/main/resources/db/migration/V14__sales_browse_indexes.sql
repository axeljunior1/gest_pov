CREATE INDEX IF NOT EXISTS idx_sales_paid_at ON sales (paid_at DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_sales_validated_at ON sales (validated_at DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_sale_refunds_created_at ON sale_refunds (created_at DESC);
