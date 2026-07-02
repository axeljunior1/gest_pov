package com.erp.products.service;

import com.erp.products.config.BootstrapAdminProperties;
import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminBootstrapService {

    static final int MIN_PASSWORD_LENGTH = 12;
    private static final Set<String> PRIVILEGED_ROLE_CODES = Set.of("SUPER_ADMIN", "ADMIN");

    private final BootstrapAdminProperties bootstrapAdminProperties;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void bootstrapIfNeeded() {
        if (userRepository.existsPrivilegedAdmin()) {
            return;
        }

        String email = requireSetting(bootstrapAdminProperties.getEmail(), "APP_BOOTSTRAP_ADMIN_EMAIL");
        String password = requireSetting(bootstrapAdminProperties.getPassword(), "APP_BOOTSTRAP_ADMIN_PASSWORD");
        validateEmail(email);
        validatePassword(password);

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new IllegalStateException(
                    "Un utilisateur existe deja avec l'email bootstrap " + email
                            + " mais aucun compte administrateur (SUPER_ADMIN/ADMIN) actif n'a ete trouve."
                            + " Corrigez la base ou definissez un autre email.");
        }

        Role superAdmin = roleRepository.findByCode("SUPER_ADMIN")
                .orElseThrow(() -> new IllegalStateException("Role SUPER_ADMIN introuvable — seed systeme incomplet"));

        String firstName = StringUtils.hasText(bootstrapAdminProperties.getFirstName())
                ? bootstrapAdminProperties.getFirstName().trim()
                : "Admin";
        String lastName = StringUtils.hasText(bootstrapAdminProperties.getLastName())
                ? bootstrapAdminProperties.getLastName().trim()
                : "ERP";

        userRepository.save(User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(email.trim().toLowerCase())
                .passwordHash(passwordEncoder.encode(password))
                .isActive(true)
                .roles(new HashSet<>(Set.of(superAdmin)))
                .build());

        log.info("Bootstrap admin created for email {}", email.trim().toLowerCase());
    }

    private static String requireSetting(String value, String envName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(
                    "Aucun administrateur actif en base et variable " + envName + " absente ou vide."
                            + " Definissez " + envName + " (et APP_BOOTSTRAP_ADMIN_PASSWORD) dans .env pour le premier demarrage client.");
        }
        return value.trim();
    }

    static void validateEmail(String email) {
        String normalized = email.trim();
        if (!normalized.contains("@") || normalized.indexOf('@') == 0 || !normalized.contains(".")) {
            throw new IllegalStateException("APP_BOOTSTRAP_ADMIN_EMAIL invalide : format email attendu");
        }
    }

    static void validatePassword(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_ADMIN_PASSWORD trop court (minimum " + MIN_PASSWORD_LENGTH + " caracteres)");
        }
    }
}
