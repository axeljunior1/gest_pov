-- Renomme value -> attr_value si l'ancienne V7 avait déjà été appliquée avec "value"
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'variant_attribute_values'
          AND column_name = 'value'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'variant_attribute_values'
          AND column_name = 'attr_value'
    ) THEN
        ALTER TABLE variant_attribute_values RENAME COLUMN value TO attr_value;
    END IF;
END $$;
