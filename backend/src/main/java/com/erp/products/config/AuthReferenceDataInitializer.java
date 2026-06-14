package com.erp.products.config;

import com.erp.products.domain.entity.Permission;
import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import com.erp.products.repository.PermissionRepository;
import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Component
@Profile("!test")
@RequiredArgsConstructor
public class AuthReferenceDataInitializer implements ApplicationRunner {

    private static final String ADMIN_EMAIL = "admin@erp.local";
    private static final String ADMIN_PASSWORD = "ErpAdmin2026!";
    private static final String CASHIER_EMAIL = "caissier@erp.local";
    private static final String CASHIER_PASSWORD = "Caissier2026!";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (permissionRepository.count() == 0) {
            log.info("Initialisation des permissions systeme...");
            seedPermissions();
        } else {
            ensureMissingPermissions();
        }

        if (roleRepository.count() == 0) {
            log.info("Initialisation des roles systeme...");
            seedRoles();
        } else {
            syncExistingRolePermissions();
        }

        ensureCashierRole();

        if (userRepository.count() == 0) {
            log.info("Creation compte administrateur {}...", ADMIN_EMAIL);
            seedAdminUser();
            seedCashierUser();
        } else {
            ensureCashierUser();
        }
    }

    private void seedPermissions() {
        allPermissionDefs().forEach(d -> permissionRepository.save(Permission.builder()
                .code(d.code)
                .name(d.name)
                .description(d.description)
                .module(d.module)
                .build()));
    }

    private void ensureMissingPermissions() {
        Map<String, PermissionDef> defs = new LinkedHashMap<>();
        allPermissionDefs().forEach(d -> defs.put(d.code, d));
        int added = 0;
        for (PermissionDef def : defs.values()) {
            if (permissionRepository.findByCode(def.code).isEmpty()) {
                permissionRepository.save(Permission.builder()
                        .code(def.code)
                        .name(def.name)
                        .description(def.description)
                        .module(def.module)
                        .build());
                added++;
            }
        }
        if (added > 0) {
            log.info("Ajout de {} permission(s) manquante(s)", added);
        }
    }

    private void seedRoles() {
        Map<String, Permission> all = loadAllPermissions();

        saveRole("Super administrateur", "SUPER_ADMIN",
                "Acces complet au systeme", true, all.values());

        saveRole("Administrateur", "ADMIN",
                "Administration generale", true, filter(all,
                "products.", "product_variant.", "product_packaging.", "stock.", "stock_entry.", "stock_exit.", "stock_movement.", "inventory.", "alerts.", "dashboard.", "analytics.", "import.", "export.", "settings.", "pos.", "customer.", "loyalty.",
                "users.read", "users.create", "users.update",
                "roles.read", "roles.update"));

        saveRole("Manager", "MANAGER",
                "Gestion stock et entrees", true, filter(all,
                "products.read", "products.update", "products.validate",
                "product_variant.read", "product_variant.create", "product_variant.update",
                "product_variant.manage_stock", "product_variant.manage_packaging",
                "stock.read", "stock.adjust",
                "stock_movement.read", "stock_movement.export",
                "inventory.read", "inventory.create", "inventory.update", "inventory.validate", "inventory.cancel",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_entry.validate", "stock_entry.cancel",
                "stock_exit.read", "stock_exit.create", "stock_exit.update",
                "stock_exit.validate", "stock_exit.cancel",
                "alerts.read", "alerts.manage", "dashboard.read", "analytics.read", "analytics.sales.read", "analytics.stock.read", "analytics.cashier.read", "analytics.export", "sales.cancellations.read", "sales.cancellations.detail", "sales.cancellations.audit", "import.read", "import.create", "export.read",
                "pos.session.open", "pos.session.close", "pos.sale.read", "pos.sale.prepare", "pos.sale.create",
                "pos.payment.collect", "pos.sale.validate",
                "pos.sale.cancel", "pos.sale.discount", "pos.sale.refund", "pos.ticket.reprint", "pos.report.read",
                "pos.return.read", "pos.return.create", "pos.return.validate",
                "pos.refund.create", "pos.refund.validate",
                "pos.session.validate_cash_difference",
                "customer.read", "customer.create", "customer.update", "loyalty.read", "loyalty.redeem", "loyalty.manage"));

        saveRole("Caissier", "CASHIER",
                "Encaissement et session caisse", true, filter(all,
                "pos.session.open", "pos.session.close", "pos.session.read",
                "pos.payment.collect", "pos.payment.validate",
                "pos.sale.read", "pos.ticket.print", "pos.ticket.reprint", "pos.report.read",
                "pos.return.read", "pos.return.create", "pos.return.validate",
                "pos.refund.create", "pos.refund.validate",
                "customer.read", "loyalty.read"));

        saveRole("Vendeur", "SELLER",
                "Preparation ventes sans encaissement", true, filter(all,
                "pos.sale.send_to_payment", "pos.sale.read", "pos.sale.read_own",
                "pos.sale.create", "pos.sale.update_draft", "pos.sale.discount",
                "customer.read", "customer.create", "loyalty.read", "loyalty.redeem"));

        saveRole("Operateur", "OPERATOR",
                "Operations quotidiennes", true, filter(all,
                "products.read", "stock.read", "stock.adjust",
                "stock_movement.read",
                "inventory.read",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_exit.read", "stock_exit.create", "stock_exit.update",
                "alerts.read", "dashboard.read", "import.read", "export.read"));

        saveRole("Consultation", "VIEWER",
                "Lecture seule", true, filter(all,
                "products.read", "stock.read", "stock_entry.read", "stock_exit.read",
                "stock_movement.read", "inventory.read", "alerts.read", "dashboard.read", "import.read", "export.read"));
    }

    private void syncExistingRolePermissions() {
        Map<String, Permission> all = loadAllPermissions();
        grantRolePermissions("SUPER_ADMIN", all.values());
        grantRolePermissions("ADMIN", filter(all,
                "products.", "product_variant.", "product_packaging.", "stock.", "stock_entry.", "stock_exit.", "stock_movement.", "inventory.", "alerts.", "dashboard.", "analytics.", "import.", "export.", "settings.", "pos.", "customer.", "loyalty.",
                "users.read", "users.create", "users.update",
                "roles.read", "roles.update"));
        grantRolePermissions("MANAGER", filter(all,
                "products.read", "products.update", "products.validate",
                "product_variant.read", "product_variant.create", "product_variant.update",
                "product_variant.manage_stock", "product_variant.manage_packaging",
                "stock.read", "stock.adjust",
                "stock_movement.read", "stock_movement.export",
                "inventory.read", "inventory.create", "inventory.update", "inventory.validate", "inventory.cancel",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_entry.validate", "stock_entry.cancel",
                "stock_exit.read", "stock_exit.create", "stock_exit.update",
                "stock_exit.validate", "stock_exit.cancel",
                "alerts.read", "alerts.manage", "dashboard.read", "analytics.read", "analytics.sales.read", "analytics.stock.read", "analytics.cashier.read", "analytics.export", "sales.cancellations.read", "sales.cancellations.detail", "sales.cancellations.audit", "import.read", "import.create", "export.read",
                "pos.session.open", "pos.session.close", "pos.sale.read", "pos.sale.prepare", "pos.sale.create",
                "pos.payment.collect", "pos.sale.validate",
                "pos.sale.cancel", "pos.sale.discount", "pos.sale.refund", "pos.ticket.reprint", "pos.report.read",
                "pos.return.read", "pos.return.create", "pos.return.validate",
                "pos.refund.create", "pos.refund.validate",
                "pos.session.validate_cash_difference",
                "customer.read", "customer.create", "customer.update", "loyalty.read", "loyalty.redeem", "loyalty.manage"));
        ensureCashierRolePermissions();
        ensureSellerRolePermissions();
        grantRolePermissions("OPERATOR", filter(all,
                "products.read", "stock.read", "stock.adjust",
                "stock_movement.read",
                "inventory.read",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_exit.read", "stock_exit.create", "stock_exit.update",
                "alerts.read", "dashboard.read", "import.read", "export.read"));
        grantRolePermissions("VIEWER", filter(all,
                "products.read", "stock.read", "stock_entry.read", "stock_exit.read",
                "stock_movement.read", "inventory.read", "alerts.read", "dashboard.read", "import.read", "export.read",
                "customer.read", "loyalty.read", "pos.sale.read", "pos.report.read", "analytics.sales.read"));
    }

    private void grantRolePermissions(String roleCode, Collection<Permission> expected) {
        Role role = roleRepository.findByCode(roleCode).orElse(null);
        if (role == null) {
            return;
        }
        role = roleRepository.findByIdWithPermissions(role.getId()).orElse(role);
        Set<Permission> merged = new HashSet<>(role.getPermissions());
        int before = merged.size();
        merged.addAll(expected);
        if (merged.size() > before) {
            role.setPermissions(merged);
            roleRepository.save(role);
            log.info("Permissions synchronisees pour le role {}", roleCode);
        }
    }

    private void replaceRolePermissions(String roleCode, Collection<Permission> expected) {
        Role role = roleRepository.findByCode(roleCode).orElse(null);
        if (role == null) {
            return;
        }
        role.setPermissions(new HashSet<>(expected));
        roleRepository.save(role);
        log.info("Permissions remplacees pour le role {}", roleCode);
    }

    private Map<String, Permission> loadAllPermissions() {
        Map<String, Permission> all = new HashMap<>();
        permissionRepository.findAll().forEach(p -> all.put(p.getCode(), p));
        return all;
    }

    private void seedAdminUser() {
        Role superAdmin = roleRepository.findByCode("SUPER_ADMIN")
                .orElseThrow();
        userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("ERP")
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(superAdmin)))
                .build());
        log.info("Compte admin cree — email: {} mot de passe: {}", ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    private void ensureCashierRole() {
        Map<String, Permission> all = loadAllPermissions();
        Collection<Permission> cashierPerms = filter(all,
                "pos.session.open", "pos.session.close", "pos.session.read",
                "pos.payment.collect", "pos.payment.validate",
                "pos.sale.read", "pos.ticket.print", "pos.ticket.reprint", "pos.report.read",
                "customer.read", "loyalty.read", "analytics.sales.read");

        roleRepository.findByCode("CASHIER").ifPresentOrElse(
                role -> {
                    role.setPermissions(new HashSet<>(cashierPerms));
                    roleRepository.save(role);
                },
                () -> {
                    saveRole("Caissier", "CASHIER", "Vente en caisse uniquement", true, cashierPerms);
                    log.info("Role CASHIER cree");
                }
        );
    }

    private void ensureCashierRolePermissions() {
        ensureCashierRole();
    }

    private void ensureSellerRolePermissions() {
        Map<String, Permission> all = loadAllPermissions();
        Collection<Permission> sellerPerms = filter(all,
                "pos.sale.send_to_payment", "pos.sale.read", "pos.sale.read_own",
                "pos.sale.create", "pos.sale.update_draft", "pos.sale.discount",
                "customer.read", "customer.create", "loyalty.read", "loyalty.redeem");

        roleRepository.findByCode("SELLER").ifPresentOrElse(
                role -> {
                    role.setPermissions(new HashSet<>(sellerPerms));
                    roleRepository.save(role);
                },
                () -> {
                    saveRole("Vendeur", "SELLER", "Preparation ventes sans encaissement", true, sellerPerms);
                    log.info("Role SELLER cree");
                }
        );
    }

    private void seedCashierUser() {
        Role cashier = roleRepository.findByCode("CASHIER")
                .orElseThrow(() -> new IllegalStateException("Role CASHIER introuvable apres ensureCashierRole"));
        userRepository.save(User.builder()
                .firstName("Caissier")
                .lastName("Vendeur")
                .email(CASHIER_EMAIL)
                .passwordHash(passwordEncoder.encode(CASHIER_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(cashier)))
                .build());
        log.info("Compte caissier cree — email: {} mot de passe: {}", CASHIER_EMAIL, CASHIER_PASSWORD);
    }

    private void ensureCashierUser() {
        if (userRepository.findByEmailIgnoreCase(CASHIER_EMAIL).isEmpty()) {
            log.info("Creation compte caissier {}...", CASHIER_EMAIL);
            seedCashierUser();
        }
    }

    private Role saveRole(String name, String code, String desc, boolean system, Collection<Permission> perms) {
        Role role = Role.builder()
                .name(name)
                .code(code)
                .description(desc)
                .isSystem(system)
                .permissions(new HashSet<>(perms))
                .build();
        return roleRepository.save(role);
    }

    private Collection<Permission> filter(Map<String, Permission> all, String... codesOrPrefixes) {
        Set<Permission> result = new LinkedHashSet<>();
        for (String pattern : codesOrPrefixes) {
            if (pattern.endsWith(".")) {
                all.keySet().stream()
                        .filter(k -> k.startsWith(pattern))
                        .map(all::get)
                        .forEach(result::add);
            } else if (all.containsKey(pattern)) {
                result.add(all.get(pattern));
            }
        }
        return result;
    }

    private List<PermissionDef> allPermissionDefs() {
        return List.of(
                p("products.read", "Lire les produits", "MODULE_PRODUCTS"),
                p("products.create", "Creer des produits", "MODULE_PRODUCTS"),
                p("products.update", "Modifier des produits", "MODULE_PRODUCTS"),
                p("products.validate", "Valider le cycle de vie produit", "MODULE_PRODUCTS"),
                p("products.delete", "Supprimer des produits", "MODULE_PRODUCTS"),
                p("product_variant.read", "Lire les variantes produit", "MODULE_PRODUCTS"),
                p("product_variant.create", "Creer des variantes produit", "MODULE_PRODUCTS"),
                p("product_variant.update", "Modifier des variantes produit", "MODULE_PRODUCTS"),
                p("product_variant.delete", "Supprimer des variantes produit", "MODULE_PRODUCTS"),
                p("product_variant.manage_stock", "Gerer le stock des variantes", "MODULE_PRODUCTS"),
                p("product_variant.manage_packaging", "Gerer les conditionnements variante", "MODULE_PRODUCTS"),
                p("product_packaging.update_price", "Modifier le prix de vente des conditionnements", "MODULE_PRODUCTS"),
                p("stock.read", "Consulter le stock", "MODULE_STOCK"),
                p("stock.adjust", "Ajuster le stock", "MODULE_STOCK"),
                p("stock_movement.read", "Consulter l historique des mouvements", "MODULE_STOCK"),
                p("stock_movement.export", "Exporter les mouvements de stock", "MODULE_STOCK"),
                p("inventory.read", "Consulter les inventaires", "MODULE_STOCK"),
                p("inventory.create", "Creer des inventaires", "MODULE_STOCK"),
                p("inventory.update", "Modifier des inventaires", "MODULE_STOCK"),
                p("inventory.validate", "Valider des inventaires", "MODULE_STOCK"),
                p("inventory.cancel", "Annuler des inventaires", "MODULE_STOCK"),
                p("stock_entry.read", "Lire les entrees stock", "MODULE_STOCK"),
                p("stock_entry.create", "Creer des entrees stock", "MODULE_STOCK"),
                p("stock_entry.update", "Modifier des entrees brouillon", "MODULE_STOCK"),
                p("stock_entry.validate", "Valider des entrees stock", "MODULE_STOCK"),
                p("stock_entry.cancel", "Annuler des entrees stock", "MODULE_STOCK"),
                p("stock_exit.read", "Lire les sorties stock", "MODULE_STOCK"),
                p("stock_exit.create", "Creer des sorties stock", "MODULE_STOCK"),
                p("stock_exit.update", "Modifier des sorties brouillon", "MODULE_STOCK"),
                p("stock_exit.validate", "Valider des sorties stock", "MODULE_STOCK"),
                p("stock_exit.cancel", "Annuler des sorties stock", "MODULE_STOCK"),
                p("alerts.read", "Consulter les alertes", "MODULE_ALERTS"),
                p("alerts.manage", "Traiter les alertes", "MODULE_ALERTS"),
                p("dashboard.read", "Consulter le tableau de bord", "MODULE_DASHBOARD"),
                p("analytics.read", "Consulter analytics complet", "MODULE_ANALYTICS"),
                p("analytics.sales.read", "Consulter analytics ventes", "MODULE_ANALYTICS"),
                p("analytics.stock.read", "Consulter analytics stock", "MODULE_ANALYTICS"),
                p("analytics.cashier.read", "Consulter performance caissiers", "MODULE_ANALYTICS"),
                p("analytics.export", "Exporter analytics", "MODULE_ANALYTICS"),
                p("sales.cancellations.read", "Consulter les ventes annulees", "MODULE_ANALYTICS"),
                p("sales.cancellations.detail", "Consulter le detail des ventes annulees", "MODULE_ANALYTICS"),
                p("sales.cancellations.audit", "Audit complet des annulations de vente", "MODULE_ANALYTICS"),
                p("import.read", "Consulter les imports et templates", "MODULE_IMPORT"),
                p("import.create", "Executer des imports", "MODULE_IMPORT"),
                p("export.read", "Exporter les donnees", "MODULE_EXPORT"),
                p("settings.read", "Consulter les parametres", "MODULE_SETTINGS"),
                p("settings.update", "Modifier les parametres", "MODULE_SETTINGS"),
                p("pos.session.open", "Ouvrir une session caisse", "MODULE_POS"),
                p("pos.session.close", "Fermer une session caisse", "MODULE_POS"),
                p("pos.session.read", "Consulter les sessions caisse", "MODULE_POS"),
                p("pos.session.validate_cash_difference", "Valider un ecart de caisse (manager)", "MODULE_POS"),
                p("pos.sale.read", "Consulter les ventes POS", "MODULE_POS"),
                p("pos.sale.read_own", "Consulter ses ventes POS", "MODULE_POS"),
                p("pos.sale.prepare", "Preparer des ventes POS (legacy)", "MODULE_POS"),
                p("pos.sale.send_to_payment", "Envoyer une vente a l encaissement", "MODULE_POS"),
                p("pos.sale.create", "Creer des ventes POS", "MODULE_POS"),
                p("pos.sale.update_draft", "Modifier un panier brouillon", "MODULE_POS"),
                p("pos.payment.collect", "Encaisser un paiement POS", "MODULE_POS"),
                p("pos.payment.validate", "Valider un paiement POS", "MODULE_POS"),
                p("pos.sale.validate", "Valider des ventes POS", "MODULE_POS"),
                p("pos.sale.cancel", "Annuler des ventes POS", "MODULE_POS"),
                p("pos.sale.discount", "Appliquer des remises POS", "MODULE_POS"),
                p("pos.sale.refund", "Rembourser des ventes POS", "MODULE_POS"),
                p("pos.return.read", "Consulter les retours POS", "MODULE_POS"),
                p("pos.return.create", "Creer un retour POS", "MODULE_POS"),
                p("pos.return.validate", "Valider un retour POS", "MODULE_POS"),
                p("pos.refund.create", "Creer un remboursement POS", "MODULE_POS"),
                p("pos.refund.validate", "Valider un remboursement POS", "MODULE_POS"),
                p("pos.ticket.print", "Imprimer un ticket POS", "MODULE_POS"),
                p("pos.ticket.reprint", "Reimprimer des tickets", "MODULE_POS"),
                p("pos.report.read", "Consulter les rapports caisse", "MODULE_POS"),
                p("customer.read", "Consulter les clients", "MODULE_CUSTOMERS"),
                p("customer.create", "Creer des clients", "MODULE_CUSTOMERS"),
                p("customer.update", "Modifier des clients", "MODULE_CUSTOMERS"),
                p("customer.delete", "Supprimer des clients", "MODULE_CUSTOMERS"),
                p("loyalty.read", "Consulter la fidelite", "MODULE_LOYALTY"),
                p("loyalty.redeem", "Utiliser points fidelite", "MODULE_LOYALTY"),
                p("loyalty.manage", "Gerer points fidelite", "MODULE_LOYALTY"),
                p("loyalty.settings.update", "Parametrer la fidelite", "MODULE_LOYALTY"),
                p("users.read", "Lire les utilisateurs", "MODULE_USERS"),
                p("users.create", "Creer des utilisateurs", "MODULE_USERS"),
                p("users.update", "Modifier des utilisateurs", "MODULE_USERS"),
                p("users.delete", "Supprimer des utilisateurs", "MODULE_USERS"),
                p("roles.read", "Lire les roles", "MODULE_ROLES"),
                p("roles.create", "Creer des roles", "MODULE_ROLES"),
                p("roles.update", "Modifier les roles", "MODULE_ROLES"),
                p("roles.delete", "Supprimer les roles", "MODULE_ROLES")
        );
    }

    private PermissionDef p(String code, String name, String module) {
        return new PermissionDef(code, name, name, module);
    }

    private record PermissionDef(String code, String name, String description, String module) {}
}
