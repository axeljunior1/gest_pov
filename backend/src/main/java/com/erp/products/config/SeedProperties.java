package com.erp.products.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Contrôle l'initialisation des données au démarrage.
 * <ul>
 *   <li>{@code system-enabled} — rôles, permissions, unités, entrepôt, paramètres techniques</li>
 *   <li>{@code demo-enabled} — autorise la génération manuelle de données de démonstration</li>
 *   <li>{@code demo-auto} — génère automatiquement le jeu démo au démarrage (désactivé en prod)</li>
 * </ul>
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.seed")
public class SeedProperties {

    /** Données système (rôles, settings, unités, entrepôt…) */
    private boolean systemEnabled = true;

    /** Autorise generate/cleanup demo via API admin */
    private boolean demoEnabled = true;

    /** Jeu démo automatique au démarrage — false en production */
    private boolean demoAuto = false;
}
