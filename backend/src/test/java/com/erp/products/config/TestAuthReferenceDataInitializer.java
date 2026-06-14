package com.erp.products.config;

import com.erp.products.domain.entity.Permission;
import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import com.erp.products.repository.PermissionRepository;
import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Component
@Profile("test")
@RequiredArgsConstructor
public class TestAuthReferenceDataInitializer implements ApplicationRunner {

    static final String ADMIN_EMAIL = "admin@erp.local";
    static final String ADMIN_PASSWORD = "ErpAdmin2026!";
    public static final String VIEWER_EMAIL = "viewer@erp.local";
    public static final String VIEWER_PASSWORD = "Viewer2026!";
    public static final String MANAGER_EMAIL = "manager@erp.local";
    public static final String MANAGER_PASSWORD = "Manager2026!";
    public static final String CASHIER_EMAIL = "cashier@erp.local";
    public static final String CASHIER_PASSWORD = "Cashier2026!";
    public static final String SELLER_EMAIL = "seller@erp.local";
    public static final String SELLER_PASSWORD = "Seller2026!";

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (permissionRepository.count() == 0) {
            seedPermissions();
        }
        if (roleRepository.count() == 0) {
            seedRoles();
        }
        if (userRepository.count() == 0) {
            seedAdminUser();
        }
    }

    private void seedPermissions() {
        List<String[]> defs = List.of(
                new String[]{"products.read", "Lire les produits", "MODULE_PRODUCTS"},
                new String[]{"products.create", "Creer des produits", "MODULE_PRODUCTS"},
                new String[]{"products.update", "Modifier des produits", "MODULE_PRODUCTS"},
                new String[]{"products.delete", "Supprimer des produits", "MODULE_PRODUCTS"},
                new String[]{"product_variant.read", "Lire les variantes produit", "MODULE_PRODUCTS"},
                new String[]{"product_variant.create", "Creer des variantes produit", "MODULE_PRODUCTS"},
                new String[]{"product_variant.update", "Modifier des variantes produit", "MODULE_PRODUCTS"},
                new String[]{"product_variant.delete", "Supprimer des variantes produit", "MODULE_PRODUCTS"},
                new String[]{"product_variant.manage_stock", "Gerer le stock des variantes", "MODULE_PRODUCTS"},
                new String[]{"product_variant.manage_packaging", "Gerer les conditionnements variante", "MODULE_PRODUCTS"},
                new String[]{"stock.read", "Consulter le stock", "MODULE_STOCK"},
                new String[]{"stock.adjust", "Ajuster le stock", "MODULE_STOCK"},
                new String[]{"stock_movement.read", "Consulter l historique des mouvements", "MODULE_STOCK"},
                new String[]{"stock_movement.export", "Exporter les mouvements de stock", "MODULE_STOCK"},
                new String[]{"inventory.read", "Consulter les inventaires", "MODULE_STOCK"},
                new String[]{"inventory.create", "Creer des inventaires", "MODULE_STOCK"},
                new String[]{"inventory.update", "Modifier des inventaires", "MODULE_STOCK"},
                new String[]{"inventory.validate", "Valider des inventaires", "MODULE_STOCK"},
                new String[]{"inventory.cancel", "Annuler des inventaires", "MODULE_STOCK"},
                new String[]{"stock_entry.read", "Lire les entrees stock", "MODULE_STOCK"},
                new String[]{"stock_entry.create", "Creer des entrees stock", "MODULE_STOCK"},
                new String[]{"stock_entry.update", "Modifier des entrees brouillon", "MODULE_STOCK"},
                new String[]{"stock_entry.validate", "Valider des entrees stock", "MODULE_STOCK"},
                new String[]{"stock_entry.cancel", "Annuler des entrees stock", "MODULE_STOCK"},
                new String[]{"stock_exit.read", "Lire les sorties stock", "MODULE_STOCK"},
                new String[]{"stock_exit.create", "Creer des sorties stock", "MODULE_STOCK"},
                new String[]{"stock_exit.update", "Modifier des sorties brouillon", "MODULE_STOCK"},
                new String[]{"stock_exit.validate", "Valider des sorties stock", "MODULE_STOCK"},
                new String[]{"stock_exit.cancel", "Annuler des sorties stock", "MODULE_STOCK"},
                new String[]{"users.read", "Lire les utilisateurs", "MODULE_USERS"},
                new String[]{"users.create", "Creer des utilisateurs", "MODULE_USERS"},
                new String[]{"users.update", "Modifier des utilisateurs", "MODULE_USERS"},
                new String[]{"users.delete", "Supprimer des utilisateurs", "MODULE_USERS"},
                new String[]{"roles.read", "Lire les roles", "MODULE_ROLES"},
                new String[]{"roles.create", "Creer des roles", "MODULE_ROLES"},
                new String[]{"roles.update", "Modifier les roles", "MODULE_ROLES"},
                new String[]{"roles.delete", "Supprimer les roles", "MODULE_ROLES"},
                new String[]{"alerts.read", "Consulter les alertes", "MODULE_ALERTS"},
                new String[]{"alerts.manage", "Traiter les alertes", "MODULE_ALERTS"},
                new String[]{"dashboard.read", "Consulter le tableau de bord", "MODULE_DASHBOARD"},
                new String[]{"analytics.read", "Consulter analytics complet", "MODULE_ANALYTICS"},
                new String[]{"analytics.sales.read", "Consulter analytics ventes", "MODULE_ANALYTICS"},
                new String[]{"analytics.stock.read", "Consulter analytics stock", "MODULE_ANALYTICS"},
                new String[]{"analytics.cashier.read", "Consulter performance caissiers", "MODULE_ANALYTICS"},
                new String[]{"analytics.export", "Exporter analytics", "MODULE_ANALYTICS"},
                new String[]{"import.read", "Consulter les imports", "MODULE_IMPORT"},
                new String[]{"import.create", "Executer des imports", "MODULE_IMPORT"},
                new String[]{"export.read", "Exporter les donnees", "MODULE_EXPORT"},
                new String[]{"settings.read", "Consulter les parametres", "MODULE_SETTINGS"},
                new String[]{"settings.update", "Modifier les parametres", "MODULE_SETTINGS"},
                new String[]{"pos.session.open", "Ouvrir session caisse", "MODULE_POS"},
                new String[]{"pos.session.close", "Fermer session caisse", "MODULE_POS"},
                new String[]{"pos.session.read", "Consulter sessions caisse", "MODULE_POS"},
                new String[]{"pos.session.validate_cash_difference", "Valider ecart caisse (manager)", "MODULE_POS"},
                new String[]{"pos.sale.read", "Lire ventes POS", "MODULE_POS"},
                new String[]{"pos.sale.read_own", "Lire ses ventes POS", "MODULE_POS"},
                new String[]{"pos.sale.prepare", "Preparer ventes POS", "MODULE_POS"},
                new String[]{"pos.sale.create", "Creer ventes POS", "MODULE_POS"},
                new String[]{"pos.sale.update_draft", "Modifier panier brouillon", "MODULE_POS"},
                new String[]{"pos.sale.send_to_payment", "Envoyer vente a encaissement", "MODULE_POS"},
                new String[]{"pos.payment.collect", "Encaisser ventes POS", "MODULE_POS"},
                new String[]{"pos.payment.validate", "Valider paiement POS", "MODULE_POS"},
                new String[]{"pos.sale.validate", "Valider ventes POS", "MODULE_POS"},
                new String[]{"pos.sale.cancel", "Annuler ventes POS", "MODULE_POS"},
                new String[]{"pos.sale.discount", "Remises POS", "MODULE_POS"},
                new String[]{"pos.sale.refund", "Rembourser ventes POS", "MODULE_POS"},
                new String[]{"pos.return.read", "Consulter retours POS", "MODULE_POS"},
                new String[]{"pos.return.create", "Creer retour POS", "MODULE_POS"},
                new String[]{"pos.return.validate", "Valider retour POS", "MODULE_POS"},
                new String[]{"pos.refund.create", "Creer remboursement POS", "MODULE_POS"},
                new String[]{"pos.refund.validate", "Valider remboursement POS", "MODULE_POS"},
                new String[]{"pos.ticket.print", "Imprimer ticket POS", "MODULE_POS"},
                new String[]{"pos.ticket.reprint", "Reimprimer tickets", "MODULE_POS"},
                new String[]{"pos.report.read", "Rapports caisse", "MODULE_POS"},
                new String[]{"customer.read", "Consulter les clients", "MODULE_CUSTOMERS"},
                new String[]{"customer.create", "Creer des clients", "MODULE_CUSTOMERS"},
                new String[]{"customer.update", "Modifier des clients", "MODULE_CUSTOMERS"},
                new String[]{"customer.delete", "Supprimer des clients", "MODULE_CUSTOMERS"},
                new String[]{"loyalty.read", "Consulter la fidelite", "MODULE_LOYALTY"},
                new String[]{"loyalty.redeem", "Utiliser points fidelite", "MODULE_LOYALTY"},
                new String[]{"loyalty.manage", "Gerer points fidelite", "MODULE_LOYALTY"},
                new String[]{"loyalty.settings.update", "Parametrer la fidelite", "MODULE_LOYALTY"}
        );
        for (String[] d : defs) {
            permissionRepository.save(Permission.builder()
                    .code(d[0])
                    .name(d[1])
                    .description(d[1])
                    .module(d[2])
                    .build());
        }
    }

    private void seedRoles() {
        Map<String, Permission> all = new HashMap<>();
        permissionRepository.findAll().forEach(p -> all.put(p.getCode(), p));
        saveRole("Super administrateur", "SUPER_ADMIN", all.values());
        saveRole("Manager", "MANAGER", filter(all,
                "inventory.read", "inventory.create", "inventory.update", "inventory.validate", "inventory.cancel",
                "stock.read", "stock.adjust", "stock_entry.read", "stock_exit.read", "products.read", "products.update",
                "dashboard.read", "analytics.read", "analytics.sales.read", "analytics.stock.read", "analytics.cashier.read", "analytics.export", "import.read", "import.create", "export.read",
                "pos.session.open", "pos.session.close", "pos.sale.read", "pos.sale.prepare", "pos.sale.create",
                "pos.payment.collect", "pos.sale.validate",
                "pos.sale.cancel", "pos.sale.discount", "pos.sale.refund", "pos.ticket.reprint", "pos.report.read",
                "pos.session.validate_cash_difference",
                "customer.read", "customer.create", "customer.update", "loyalty.read", "loyalty.redeem", "loyalty.manage"));
        saveRole("Caissier", "CASHIER", filter(all,
                "pos.session.open", "pos.session.close", "pos.session.read",
                "pos.payment.collect", "pos.payment.validate",
                "pos.sale.read", "pos.ticket.print", "pos.ticket.reprint", "pos.report.read",
                "pos.return.read", "pos.return.create", "pos.return.validate",
                "pos.refund.create", "pos.refund.validate",
                "customer.read", "loyalty.read", "analytics.sales.read"));
        saveRole("Vendeur", "SELLER", filter(all,
                "pos.sale.send_to_payment", "pos.sale.read", "pos.sale.read_own",
                "pos.sale.create", "pos.sale.update_draft", "pos.sale.discount",
                "customer.read", "customer.create", "loyalty.read", "loyalty.redeem"));
        saveRole("Consultation", "VIEWER", filter(all, "inventory.read", "stock.read", "products.read",
                "customer.read", "loyalty.read", "pos.sale.read", "pos.report.read", "analytics.read"));
    }

    private Collection<Permission> filter(Map<String, Permission> all, String... codes) {
        List<Permission> result = new ArrayList<>();
        for (String code : codes) {
            if (all.containsKey(code)) {
                result.add(all.get(code));
            }
        }
        return result;
    }

    private void seedAdminUser() {
        Role superAdmin = roleRepository.findByCode("SUPER_ADMIN").orElseThrow();
        Role manager = roleRepository.findByCode("MANAGER").orElseThrow();
        Role cashier = roleRepository.findByCode("CASHIER").orElseThrow();
        Role seller = roleRepository.findByCode("SELLER").orElseThrow();
        Role viewer = roleRepository.findByCode("VIEWER").orElseThrow();
        userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("ERP")
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(superAdmin)))
                .build());
        userRepository.save(User.builder()
                .firstName("Manager")
                .lastName("Stock")
                .email(MANAGER_EMAIL)
                .passwordHash(passwordEncoder.encode(MANAGER_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(manager)))
                .build());
        userRepository.save(User.builder()
                .firstName("Caissier")
                .lastName("POS")
                .email(CASHIER_EMAIL)
                .passwordHash(passwordEncoder.encode(CASHIER_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(cashier)))
                .build());
        userRepository.save(User.builder()
                .firstName("Vendeur")
                .lastName("POS")
                .email(SELLER_EMAIL)
                .passwordHash(passwordEncoder.encode(SELLER_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(seller)))
                .build());
        userRepository.save(User.builder()
                .firstName("Viewer")
                .lastName("Read")
                .email(VIEWER_EMAIL)
                .passwordHash(passwordEncoder.encode(VIEWER_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(viewer)))
                .build());
    }

    private void saveRole(String name, String code, Collection<Permission> perms) {
        roleRepository.save(Role.builder()
                .name(name)
                .code(code)
                .description(name)
                .isSystem(true)
                .permissions(new HashSet<>(perms))
                .build());
    }
}
