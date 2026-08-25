package com.erp.products.service;

import com.erp.products.domain.entity.User;
import com.erp.products.exception.BusinessException;
import com.erp.products.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Authentification par badge (barcode/QR) + code PIN — utilisee pour la connexion rapide et pour
 * les popups de validation manager (remboursement, ecart de caisse, ajustement de stock) en
 * alternative a email+mot de passe. Verrouille le compte apres plusieurs echecs (le PIN est court,
 * donc a faible entropie face au brute-force).
 */
@Service
@RequiredArgsConstructor
public class BadgePinAuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final Duration LOCKOUT_DURATION = Duration.ofMinutes(15);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User authenticate(String badgeCode, String pin) {
        if (badgeCode == null || badgeCode.isBlank() || pin == null || pin.isBlank()) {
            throw new BusinessException("Badge et code PIN requis");
        }
        User user = userRepository.findByBadgeCodeWithRolesAndPermissions(badgeCode.trim())
                .orElseThrow(() -> new BusinessException("Badge inconnu"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new BusinessException("Compte desactive");
        }
        if (user.getPinLockedUntil() != null && user.getPinLockedUntil().isAfter(Instant.now())) {
            long minutes = Duration.between(Instant.now(), user.getPinLockedUntil()).toMinutes() + 1;
            throw new BusinessException("Compte temporairement verrouille apres plusieurs echecs de PIN "
                    + "(reessayez dans " + minutes + " min)");
        }
        if (user.getPinHash() == null || !passwordEncoder.matches(pin, user.getPinHash())) {
            registerFailure(user);
            throw new BusinessException("Code PIN incorrect");
        }

        if (user.getPinFailedAttempts() != null && user.getPinFailedAttempts() > 0) {
            user.setPinFailedAttempts(0);
            user.setPinLockedUntil(null);
            userRepository.save(user);
        }
        return user;
    }

    private void registerFailure(User user) {
        int attempts = (user.getPinFailedAttempts() == null ? 0 : user.getPinFailedAttempts()) + 1;
        user.setPinFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setPinLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
        }
        userRepository.save(user);
    }
}
