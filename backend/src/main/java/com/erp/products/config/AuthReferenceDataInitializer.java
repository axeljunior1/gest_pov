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
        }
        if (roleRepository.count() == 0) {
            log.info("Initialisation des roles systeme...");
            seedRoles();
        }
        if (userRepository.count() == 0) {
            log.info("Creation compte administrateur {}...", ADMIN_EMAIL);
            seedAdminUser();
        }
    }

    private void seedPermissions() {
        List<PermissionDef> defs = List.of(
                p("products.read", "Lire les produits", "MODULE_PRODUCTS"),
                p("products.create", "Creer des produits", "MODULE_PRODUCTS"),
                p("products.update", "Modifier des produits", "MODULE_PRODUCTS"),
                p("products.delete", "Supprimer des produits", "MODULE_PRODUCTS"),
                p("stock.read", "Consulter le stock", "MODULE_STOCK"),
                p("stock.adjust", "Ajuster le stock", "MODULE_STOCK"),
                p("stock_entry.read", "Lire les entrees stock", "MODULE_STOCK"),
                p("stock_entry.create", "Creer des entrees stock", "MODULE_STOCK"),
                p("stock_entry.update", "Modifier des entrees brouillon", "MODULE_STOCK"),
                p("stock_entry.validate", "Valider des entrees stock", "MODULE_STOCK"),
                p("stock_entry.cancel", "Annuler des entrees stock", "MODULE_STOCK"),
                p("users.read", "Lire les utilisateurs", "MODULE_USERS"),
                p("users.create", "Creer des utilisateurs", "MODULE_USERS"),
                p("users.update", "Modifier des utilisateurs", "MODULE_USERS"),
                p("users.delete", "Supprimer des utilisateurs", "MODULE_USERS"),
                p("roles.read", "Lire les roles", "MODULE_ROLES"),
                p("roles.create", "Creer des roles", "MODULE_ROLES"),
                p("roles.update", "Modifier les roles", "MODULE_ROLES"),
                p("roles.delete", "Supprimer les roles", "MODULE_ROLES")
        );
        defs.forEach(d -> permissionRepository.save(Permission.builder()
                .code(d.code)
                .name(d.name)
                .description(d.description)
                .module(d.module)
                .build()));
    }

    private void seedRoles() {
        Map<String, Permission> all = new HashMap<>();
        permissionRepository.findAll().forEach(p -> all.put(p.getCode(), p));

        Role superAdmin = saveRole("Super administrateur", "SUPER_ADMIN",
                "Acces complet au systeme", true, all.values());

        Role admin = saveRole("Administrateur", "ADMIN",
                "Administration generale", true, filter(all,
                "products.", "stock.", "stock_entry.", "users.read", "users.create", "users.update",
                "roles.read", "roles.update"));

        Role manager = saveRole("Manager", "MANAGER",
                "Gestion stock et entrees", true, filter(all,
                "products.read", "products.update",
                "stock.read", "stock.adjust",
                "stock_entry.read", "stock_entry.create", "stock_entry.update",
                "stock_entry.validate", "stock_entry.cancel"));

        Role operator = saveRole("Operateur", "OPERATOR",
                "Operations quotidiennes", true, filter(all,
                "products.read", "stock.read", "stock.adjust",
                "stock_entry.read", "stock_entry.create", "stock_entry.update"));

        Role viewer = saveRole("Consultation", "VIEWER",
                "Lecture seule", true, filter(all,
                "products.read", "stock.read", "stock_entry.read"));

        roleRepository.saveAll(List.of(superAdmin, admin, manager, operator, viewer));
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
            } else {
                if (all.containsKey(pattern)) {
                    result.add(all.get(pattern));
                }
            }
        }
        return result;
    }

    private PermissionDef p(String code, String name, String module) {
        return new PermissionDef(code, name, name, module);
    }

    private record PermissionDef(String code, String name, String description, String module) {}
}
