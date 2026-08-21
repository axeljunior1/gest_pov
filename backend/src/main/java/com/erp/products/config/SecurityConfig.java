package com.erp.products.config;

import com.erp.products.security.JwtAuthenticationFilter;
import com.erp.products.license.LicenseEnforcementFilter;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final LicenseEnforcementFilter licenseEnforcementFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Chaîne réelle (Web, Docker, Desktop, dev).
     * Matchers Ant : les MvcRequestMatcher ignorent une URL s'il n'y a pas encore de handler MVC
     * (403 vide sur GET /api/discovery).
     */
    @Bean
    @Profile("!test")
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return buildApplicationSecurity(http);
    }

    /**
     * Même règles que la prod, pour tester discovery/login sans {@code permitAll} du profil test.
     */
    @Bean
    @Profile("test")
    @ConditionalOnProperty(name = "app.security.use-prod-chain", havingValue = "true")
    public SecurityFilterChain productionSecurityFilterChainInTest(HttpSecurity http) throws Exception {
        return buildApplicationSecurity(http);
    }

    @Bean
    @Profile("test")
    @ConditionalOnProperty(name = "app.security.use-prod-chain", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(licenseEnforcementFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private SecurityFilterChain buildApplicationSecurity(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Sinon un 404 MVC est renvoyé comme 401 via /error (clients déconnectés à tort).
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(antMatcher("/error")).permitAll()
                        .requestMatchers(antMatcher(HttpMethod.OPTIONS, "/**")).permitAll()
                        .requestMatchers(antMatcher("/api/license/**")).permitAll()
                        .requestMatchers(antMatcher("/api/discovery"), antMatcher("/api/discovery/**")).permitAll()
                        .requestMatchers(antMatcher("/api/auth/login")).permitAll()
                        .requestMatchers(antMatcher("/api/settings/public")).permitAll()
                        .requestMatchers(antMatcher("/uploads/**")).permitAll()
                        .requestMatchers(
                                antMatcher("/actuator/health/liveness"),
                                antMatcher("/actuator/health/readiness")).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write("{\"message\":\"Authentification requise\"}");
                        })
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(403);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                            response.getWriter().write(
                                    "{\"message\":\"Vous n'avez pas l'autorisation d'effectuer cette opération.\"}");
                        }))
                .addFilterBefore(licenseEnforcementFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
