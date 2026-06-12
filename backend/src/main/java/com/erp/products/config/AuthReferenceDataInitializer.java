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

        if (userRepository.count() == 0) {
            log.info("Creation compte administrateur {}...", ADMIN_EMAIL);
            seedAdminUser();
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
                "products.", "stock.", "stock_entry.", "stock_exit.", "stock_movement.", "inventory.", "alerts.", "dashboard.", "import.", "export.", "settings.",
                "users.read", "users.create", "users.update",
                "roles.read", "roles.update"));

        saveRole("Manager", "MANAGER",
                "Gestion stock et entrees", true, filter(all,
                "products.read", "products.update",
                "stock.read", "stock.adjust",
                "stock_movement.read", "stock_movement.export",
                "inventory.read", "inventory.create", "inventory.update", "inventory.validate", "inventory.cancel",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_entry.validate", "stock_entry.cancel",
                "stock_exit.read", "stock_exit.create", "stock_exit.update",
                "stock_exit.validate", "stock_exit.cancel",
                "alerts.read", "alerts.manage", "dashboard.read", "import.read", "import.create", "export.read"));

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
                "products.", "stock.", "stock_entry.", "stock_exit.", "stock_movement.", "inventory.", "alerts.", "dashboard.", "import.", "export.", "settings.",
                "users.read", "users.create", "users.update",
                "roles.read", "roles.update"));
        grantRolePermissions("MANAGER", filter(all,
                "products.read", "products.update",
                "stock.read", "stock.adjust",
                "stock_movement.read", "stock_movement.export",
                "inventory.read", "inventory.create", "inventory.update", "inventory.validate", "inventory.cancel",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_entry.validate", "stock_entry.cancel",
                "stock_exit.read", "stock_exit.create", "stock_exit.update",
                "stock_exit.validate", "stock_exit.cancel",
                "alerts.read", "alerts.manage", "dashboard.read", "import.read", "import.create", "export.read"));
        grantRolePermissions("OPERATOR", filter(all,
                "products.read", "stock.read", "stock.adjust",
                "stock_movement.read",
                "inventory.read",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_exit.read", "stock_exit.create", "stock_exit.update",
                "alerts.read", "dashboard.read", "import.read", "export.read"));
        grantRolePermissions("VIEWER", filter(all,
                "products.read", "stock.read", "stock_entry.read", "stock_exit.read",
                "stock_movement.read", "inventory.read", "alerts.read", "dashboard.read", "import.read", "export.read"));
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
                p("products.delete", "Supprimer des produits", "MODULE_PRODUCTS"),
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
                p("import.read", "Consulter les imports et templates", "MODULE_IMPORT"),
                p("import.create", "Executer des imports", "MODULE_IMPORT"),
                p("export.read", "Exporter les donnees", "MODULE_EXPORT"),
                p("settings.read", "Consulter les parametres", "MODULE_SETTINGS"),
                p("settings.update", "Modifier les parametres", "MODULE_SETTINGS"),
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
