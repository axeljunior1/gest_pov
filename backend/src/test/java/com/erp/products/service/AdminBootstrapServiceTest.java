package com.erp.products.service;

import com.erp.products.config.BootstrapAdminProperties;
import com.erp.products.domain.entity.Role;
import com.erp.products.domain.entity.User;
import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({AdminBootstrapService.class, BootstrapAdminProperties.class, AdminBootstrapServiceTest.Config.class})
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:adminBootstrapTest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class AdminBootstrapServiceTest {

  private static final String BOOTSTRAP_EMAIL = "bootstrap-admin@client.example";
  private static final String BOOTSTRAP_PASSWORD = "Str0ng-P@ssw0rd!";

  @Autowired
  AdminBootstrapService adminBootstrapService;

  @Autowired
  BootstrapAdminProperties bootstrapAdminProperties;

  @Autowired
  UserRepository userRepository;

  @Autowired
  RoleRepository roleRepository;

  @Autowired
  PasswordEncoder passwordEncoder;

  @BeforeEach
  void setUpRoles() {
    bootstrapAdminProperties.setEmail(BOOTSTRAP_EMAIL);
    bootstrapAdminProperties.setPassword(BOOTSTRAP_PASSWORD);
    bootstrapAdminProperties.setFirstName("Client");
    bootstrapAdminProperties.setLastName("Admin");

    if (roleRepository.findByCode("SUPER_ADMIN").isEmpty()) {
      roleRepository.save(Role.builder()
          .name("Super administrateur")
          .code("SUPER_ADMIN")
          .description("test")
          .isSystem(true)
          .permissions(new HashSet<>())
          .build());
    }
  }

  @Test
  void shouldCreateBootstrapAdminWhenDatabaseHasNoPrivilegedAdmin() {
    adminBootstrapService.bootstrapIfNeeded();

    User user = userRepository.findByEmailIgnoreCase(BOOTSTRAP_EMAIL).orElseThrow();
    assertThat(user.getRoles()).extracting(Role::getCode).contains("SUPER_ADMIN");
    assertThat(user.getPasswordHash()).isNotEqualTo(BOOTSTRAP_PASSWORD);
    assertThat(passwordEncoder.matches(BOOTSTRAP_PASSWORD, user.getPasswordHash())).isTrue();
  }

  @Test
  void shouldNotRecreateAdminWhenPrivilegedAdminAlreadyExists() {
    Role superAdmin = roleRepository.findByCode("SUPER_ADMIN").orElseThrow();
    userRepository.save(User.builder()
        .firstName("Existing")
        .lastName("Admin")
        .email("existing-admin@client.example")
        .passwordHash(passwordEncoder.encode("Other-P@ssw0rd12"))
        .isActive(true)
        .roles(new HashSet<>(Set.of(superAdmin)))
        .build());

    adminBootstrapService.bootstrapIfNeeded();

    assertThat(userRepository.findByEmailIgnoreCase(BOOTSTRAP_EMAIL)).isEmpty();
    assertThat(userRepository.existsPrivilegedAdmin()).isTrue();
  }

  @Test
  void shouldFailWhenBootstrapVariablesMissingOnEmptyDatabase() {
    bootstrapAdminProperties.setEmail("");
    bootstrapAdminProperties.setPassword("");

    assertThatThrownBy(() -> adminBootstrapService.bootstrapIfNeeded())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_BOOTSTRAP_ADMIN_EMAIL");
  }

  @Test
  void shouldFailWhenBootstrapPasswordTooShort() {
    bootstrapAdminProperties.setPassword("short");

    assertThatThrownBy(() -> adminBootstrapService.bootstrapIfNeeded())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("APP_BOOTSTRAP_ADMIN_PASSWORD");
  }

  static class Config {
    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }
  }
}
