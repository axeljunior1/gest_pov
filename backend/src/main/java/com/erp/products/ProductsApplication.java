package com.erp.products;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Locale;

@SpringBootApplication
@EnableScheduling
public class ProductsApplication {

    public static void main(String[] args) {
        loadDotenv();
        SpringApplication.run(ProductsApplication.class, args);
    }

    /**
     * Charge backend/.env pour les lancements locaux hors Docker (mvn spring-boot:run).
     * Docker Compose lit son propre .env nativement ; ici on le fait manuellement, sans
     * jamais ecraser une variable d'environnement ou -D deja definie (donc aucun effet
     * quand ces valeurs viennent reellement du conteneur/CI).
     *
     * Chaque entree est posee sous deux formes : la cle brute (SCREAMING_SNAKE_CASE),
     * necessaire pour les placeholders litteraux ${SPRING_DATASOURCE_URL:...} des YAML,
     * et sa forme canonique en points (spring.datasource.url). Cette seconde forme est
     * indispensable pour les reglages lus directement par le framework au bootstrap —
     * spring.profiles.active en particulier : Spring ne convertit SCREAMING_SNAKE_CASE
     * en dot.case que pour une vraie variable d'environnement OS, jamais pour une
     * System property posee manuellement.
     */
    private static void loadDotenv() {
        Dotenv dotenv = Dotenv.configure()
                .directory(".")
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();
        dotenv.entries().forEach(entry -> {
            String key = entry.getKey();
            String value = entry.getValue();
            if (System.getProperty(key) == null && System.getenv(key) == null) {
                System.setProperty(key, value);
            }
            String dotted = key.toLowerCase(Locale.ROOT).replace('_', '.');
            if (!dotted.equals(key) && System.getProperty(dotted) == null && System.getenv(key) == null) {
                System.setProperty(dotted, value);
            }
        });
    }
}
