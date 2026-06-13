package com.erp.products.config;

import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifie la migration d'une base existante sans role CASHIER ni compte caissier.
 */
@DataJpaTest
@ActiveProfiles("test")
@Import({TestAuthReferenceDataInitializer.class, AuthReferenceDataInitializerTest.Config.class})
class AuthReferenceDataInitializerTest {

    @Autowired
    RoleRepository roleRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    AuthReferenceDataInitializer prodInitializer;

    @Test
    void shouldCreateCashierRoleAndUserOnExistingDatabase() throws Exception {
        // Simule une base prod deja peuplee (admin) mais sans role/compte caissier prod.
        userRepository.findByEmailIgnoreCase(TestAuthReferenceDataInitializer.CASHIER_EMAIL)
                .ifPresent(userRepository::delete);
        userRepository.findByEmailIgnoreCase("caissier@erp.local").ifPresent(userRepository::delete);
        roleRepository.findByCode("CASHIER").ifPresent(roleRepository::delete);

        assertThat(roleRepository.findByCode("CASHIER")).isEmpty();

        prodInitializer.run(null);

        assertThat(roleRepository.findByCode("CASHIER")).isPresent();
        assertThat(userRepository.findByEmailIgnoreCase("caissier@erp.local")).isPresent();
    }

    static class Config {
        @org.springframework.context.annotation.Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }

        @org.springframework.context.annotation.Bean
        AuthReferenceDataInitializer authReferenceDataInitializer(
                com.erp.products.repository.PermissionRepository permissionRepository,
                RoleRepository roleRepository,
                UserRepository userRepository,
                PasswordEncoder passwordEncoder) {
            return new AuthReferenceDataInitializer(permissionRepository, roleRepository, userRepository, passwordEncoder);
        }
    }
}
