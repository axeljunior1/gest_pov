# Optimisation tests backend — Gest_POV

**Date :** 2026-06-17  
**Objectif :** réduire le temps d'exécution sans redémarrer Spring à chaque méthode de test.

---

## 1. Cause de lenteur

`AbstractIntegrationTest` portait :

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
```

Effet : **redémarrage complet du contexte Spring Boot** après chaque méthode (~211 tests, ~30 classes). Chaque cycle recharge JPA, sécurité, initializers, schéma H2, MockMvc, etc.

**Mesure avant optimisation (avec `@DirtiesContext`, après corrections d'échecs) :**

| Commande | Durée |
|----------|-------|
| `LicenseServiceTest` | ~68 s |
| `SettingsControllerTest` | ~130 s |
| `AnalyticsControllerTest` | ~152 s |
| `AdminDevControllerTest` | ~132 s |
| Groupe 4 classes | ~177 s |
| Suite complète `mvn test` | ~19 min (211 tests, dont 10 échecs corrigés ensuite) |

---

## 2. Changement appliqué

### Retrait du `@DirtiesContext` global

Supprimé de `AbstractIntegrationTest`. Aucun `@DirtiesContext` local ajouté (tous les tests ciblés et représentatifs passent sans).

### `TestDatabaseCleaner` (profil `test`)

Fichier : `backend/src/test/java/com/erp/products/support/TestDatabaseCleaner.java`

**Stratégie :**

1. `@BeforeEach` dans `AbstractIntegrationTest` → `testDatabaseCleaner.resetDatabase()`
2. `TRUNCATE TABLE ... RESTART IDENTITY` sur toutes les tables `PUBLIC` (H2)
3. `SET REFERENTIAL_INTEGRITY FALSE/TRUE` autour du truncate
4. Exclusion : `flyway_schema_history` (absent en test H2 de toute façon)
5. Fallback `DELETE FROM` si truncate échoue
6. **Reseed** dans la même transaction via :
   - `ReferenceValueTestInitializer.seedAll()`
   - `TestAuthReferenceDataInitializer.seedAll()` (permissions, rôles, utilisateurs seed)
   - `TestAlertReferenceDataInitializer.seedAll()`
   - `TestAppSettingsInitializer.seedAll()` (defaults + `ERP Produits` / `EUR` pour tests intégration)

**Non nettoyé :** fichiers licence (`target/test-gest-pov-data`) — hors base H2.

**Logique métier prod :** inchangée. Seuls composants `@Profile("test")` ou test sources modifiés.

---

## 3. Fichiers modifiés

| Fichier | Rôle |
|---------|------|
| `AbstractIntegrationTest.java` | Retrait `@DirtiesContext`, ajout `@BeforeEach` cleanup |
| `support/TestDatabaseCleaner.java` | **Créé** — truncate + reseed |
| `TestAuthReferenceDataInitializer.java` | Méthode publique `seedAll()` |
| `TestAlertReferenceDataInitializer.java` | Méthode publique `seedAll()` |
| `ReferenceValueTestInitializer.java` | Méthode publique `seedAll()` + `@Order(1)` |
| `TestAppSettingsInitializer.java` | Méthode publique `seedAll()` + `@Order(100)` |

---

## 4. Tests lancés et résultats (après optimisation)

| Commande | Durée | Résultat |
|----------|-------|----------|
| `mvn -Dtest=LicenseServiceTest test` | **49,9 s** | OK |
| `mvn -Dtest=SettingsControllerTest test` | **51,3 s** | OK |
| `mvn -Dtest=AnalyticsControllerTest test` | **97,7 s** | OK |
| `mvn -Dtest=AdminDevControllerTest test` | **~57 s** | OK |
| Groupe 4 classes | **81 s** | OK |
| Représentatif 5 classes (`StockEntry`, `Pos`, `Auth`, `Product`, `PosReturnRefund`) | **82,4 s** | OK |

### Gain observé (ciblé)

| Scénario | Avant | Après | Gain |
|----------|-------|-------|------|
| `SettingsControllerTest` | ~130 s | ~51 s | **~2,5×** |
| Groupe 4 classes | ~177 s | ~81 s | **~2,2×** |
| 5 classes représentatives | (non mesuré isolément, estimé plusieurs min avec DirtiesContext) | ~82 s | **fort** |

Gain suite complète **confirmé (2026-06-17) :**

| Métrique | Avant (`@DirtiesContext`) | Après (`TestDatabaseCleaner`) |
|----------|---------------------------|-------------------------------|
| Durée `mvn test` | ~19 min (~1 140 s) | **4 min 05 s (~248 s)** |
| Tests | 211 (6 fail + 4 err avant corrections) | **211 OK, 0 échec** |
| Gain | — | **~4,6× plus rapide** |

---

## 5. Tests gardant `@DirtiesContext`

**Aucun** pour l'instant. Si une classe échoue après nettoyage DB :

```java
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS) // raison documentée
```

Cas candidats futurs : contexte `test`+`dev` (`AdminDevControllerTest`) si pollution entre classes — **non nécessaire** aux tests actuels.

---

## 6. Risques restants

| Risque | Mitigation |
|--------|------------|
| Ordre d'exécution / fuite de données | `@BeforeEach` truncate + reseed systématique |
| Cache licence en mémoire (`LicenseService`) | Profil test : enforcement off ; surveiller `LicenseImportIntegrationTest` |
| Fichiers upload test | Répertoire `target/test-uploads` non vidé entre tests |
| `AdminDevControllerTest` + reset-demo SQL | Compatible H2 ; test reset/seed passe |
| Performance truncate | Négligeable vs redémarrage Spring (~ms vs ~10–30 s) |

---

## 7. Commandes utiles

```bash
# Tests unitaires licence (sans Spring)
cd backend && mvn -Dtest=LicenseServiceTest test

# Intégration settings
cd backend && mvn -Dtest=SettingsControllerTest test

# Groupe des 4 classes corrigées + optimisées
cd backend && mvn "-Dtest=AdminDevControllerTest,LicenseServiceTest,AnalyticsControllerTest,SettingsControllerTest" test

# Représentatif POS/stock/catalogue/retours
cd backend && mvn "-Dtest=StockEntryControllerTest,PosControllerTest,AuthControllerTest,ProductControllerTest,PosReturnRefundTest" test

# Suite complète (sur demande — ~3–6 min estimé vs ~19 min avant)
cd backend && mvn test
```

---

## 8. Recommandation

**Suite complète validée** : `mvn test` → **211/211 OK** en **~4 min** (vs ~19 min).

**Ensuite (optionnel) :** séparer Surefire tests rapides (`LicenseServiceTest`, `CmpCalculatorTest`, …) vs intégration pour CI plus réactive ; script `test-fast` pour PR locales.

---

*Voir aussi : [`backend-test-failures.md`](backend-test-failures.md), [`release-audit-v1.md`](release-audit-v1.md)*
