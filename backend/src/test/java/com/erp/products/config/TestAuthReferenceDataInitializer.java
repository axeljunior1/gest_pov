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
                new String[]{"stock.read", "Consulter le stock", "MODULE_STOCK"},
                new String[]{"stock.adjust", "Ajuster le stock", "MODULE_STOCK"},
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
                new String[]{"alerts.manage", "Traiter les alertes", "MODULE_ALERTS"}
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
    }

    private void seedAdminUser() {
        Role superAdmin = roleRepository.findByCode("SUPER_ADMIN").orElseThrow();
        userRepository.save(User.builder()
                .firstName("Admin")
                .lastName("ERP")
                .email(ADMIN_EMAIL)
                .passwordHash(passwordEncoder.encode(ADMIN_PASSWORD))
                .isActive(true)
                .roles(new HashSet<>(Set.of(superAdmin)))
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
