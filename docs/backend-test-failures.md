# Corrections tests backend en échec — Gest_POV

**Date :** 2026-06-17  
**Contexte :** suite complète `mvn test` — 211 tests, 6 failures, 4 errors (~19 min)

---

## Synthèse

| Classe | Avant | Après (tests ciblés) |
|--------|-------|----------------------|
| `LicenseServiceTest` | 4 failures + 1 error | **7/7 OK** (~68 s) |
| `AdminDevControllerTest` | 3 errors | **3/3 OK** (~132 s) |
| `SettingsControllerTest` | 1 failure | **10/10 OK** (~130 s) |
| `AnalyticsControllerTest` | 1 failure | **8/8 OK** (~152 s) |
| **Groupe des 4 classes** | — | **OK** (~177 s) |

Suite complète **non relancée** (sur demande).

---

## 1. `AdminDevControllerTest` (3 errors)

### Cause
Profils actifs `test` + `dev` : `application-dev.yml` force une datasource **PostgreSQL** (`jdbc:postgresql://127.0.0.1:5432/...`) alors que le profil `test` utilise le driver **H2** → `Driver org.h2.Driver claims to not accept jdbcUrl`.

Annotations dupliquées (`@SpringBootTest`, `@AutoConfigureMockMvc`, `@Autowired MockMvc`) en plus de `AbstractIntegrationTest`.

### Correction
- Suppression des annotations redondantes (héritage `AbstractIntegrationTest`).
- Ajout `@TestPropertySource` pour forcer H2 en mémoire (`admdevtest`) tout en gardant `@ActiveProfiles({"test", "dev"})` pour activer `AdminDevController`.
- Commentaire expliquant l’intention.

### Fichier
`backend/src/test/java/com/erp/products/controller/AdminDevControllerTest.java`

### Commande
```bash
cd backend && mvn -Dtest=AdminDevControllerTest test
```

---

## 2. `LicenseServiceTest` (4 failures + 1 error)

### Cause
Le test signe avec `test_private_key.pem` mais `LicenseService` vérifiait avec `classpath:keys/public_key.pem` (clé **production**) → toutes les signatures rejetées (`INVALID_SIGNATURE`).

### Correction
- `properties.setPublicKeyResource("classpath:keys/test_public_key.pem")` dans `@BeforeEach` et `shouldEnforceMaxUsers`.
- Aucune modification de `LicenseSignatureVerifier` ni de la config prod.

### Fichier
`backend/src/test/java/com/erp/products/license/LicenseServiceTest.java`

### Commande
```bash
cd backend && mvn -Dtest=LicenseServiceTest test
```

---

## 3. `SettingsControllerTest` + `AnalyticsControllerTest` (1 failure chacun)

### Cause
Les tests attendaient `company.name = "ERP Produits"` et `currency = "EUR"`, mais les **defaults prod** ont été vidés (`""`) pour la configuration client (setup initial vide).

Tentative initiale de seed via `TestAppSettingsInitializer` a provoqué une **erreur de contexte** : `setSetting(APP_CURRENCY, "EUR")` exécuté **avant** le seed des `app_reference_values` → `Valeur invalide pour CURRENCY: EUR`.

### Correction
- `TestAppSettingsInitializer` : après `ensureDefaultsSeeded()`, seed test-only `company.name` + `app.currency` (EUR).
- `ReferenceValueTestInitializer` : `@Order(1)`.
- `TestAppSettingsInitializer` : `@Order(100)` pour garantir l’ordre des `ApplicationRunner`.

**Pas de changement** des defaults dans `SettingsService` (prod inchangée).

### Fichiers
- `backend/src/test/java/com/erp/products/config/TestAppSettingsInitializer.java`
- `backend/src/main/java/com/erp/products/config/ReferenceValueTestInitializer.java` (profil `test` uniquement — `@Order`)

### Commandes
```bash
cd backend && mvn -Dtest=SettingsControllerTest test
cd backend && mvn -Dtest=AnalyticsControllerTest test
```

---

## Validation groupée

```bash
cd backend && mvn "-Dtest=AdminDevControllerTest,LicenseServiceTest,AnalyticsControllerTest,SettingsControllerTest" test
```

**Résultat :** BUILD SUCCESS (~177 s).

---

## Suite complète (non lancée)

```bash
cd backend && mvn test
```

À lancer **sur validation manager** — durée attendue ~19 min (optimisation `@DirtiesContext` recommandée avant).

---

## Risques restants

| Risque | Détail |
|--------|--------|
| Lenteur suite complète | `@DirtiesContext(AFTER_EACH_TEST_METHOD)` sur `AbstractIntegrationTest` inchangé |
| Ordre runners test | Si nouveaux `ApplicationRunner` profil `test`, vérifier `@Order` |
| Defaults prod vides | Comportement voulu ; seuls les tests reçoivent ERP Produits/EUR via seed test |
| `ReferenceValueTestInitializer` | Modifié uniquement pour `@Order(1)` — pas de logique prod |

---

## Prochaine amélioration recommandée

1. **Optimiser `@DirtiesContext`** → nettoyage H2 entre tests (gain estimé 5–10×).
2. Relancer `mvn test` complet pour confirmer 211/211.
