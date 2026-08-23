-- Nettoyage des ventes brouillon/annulées sans aucune ligne ("ventes fantômes" créées
-- automatiquement à l'ouverture de session côté client desktop avant correction du bug).
-- sale_events est en ON DELETE CASCADE ; sale_lines/payments/sale_refunds/loyalty_transactions
-- sont explicitement vérifiés vides avant suppression pour ne jamais toucher une vraie vente.
DELETE FROM sales s
WHERE s.status IN ('DRAFT', 'CANCELLED')
  AND NOT EXISTS (SELECT 1 FROM sale_lines sl WHERE sl.sale_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM payments p WHERE p.sale_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM sale_refunds sr WHERE sr.sale_id = s.id)
  AND NOT EXISTS (SELECT 1 FROM loyalty_transactions lt WHERE lt.sale_id = s.id);
