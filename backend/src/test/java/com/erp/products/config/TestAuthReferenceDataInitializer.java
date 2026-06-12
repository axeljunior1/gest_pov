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
                new String[]{"import.read", "Consulter les imports", "MODULE_IMPORT"},
                new String[]{"import.create", "Executer des imports", "MODULE_IMPORT"},
                new String[]{"export.read", "Exporter les donnees", "MODULE_EXPORT"},
                new String[]{"settings.read", "Consulter les parametres", "MODULE_SETTINGS"},
                new String[]{"settings.update", "Modifier les parametres", "MODULE_SETTINGS"}
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
                "stock.read", "stock.adjust", "stock_entry.read", "stock_exit.read", "products.read",
                "dashboard.read", "import.read", "import.create", "export.read"));
        saveRole("Consultation", "VIEWER", filter(all, "inventory.read", "stock.read", "products.read"));
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
