package com.erp.products.config;

import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import com.erp.products.service.AdminBootstrapService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = AuthReferenceDataInitializer.class))
@Import({AuthReferenceDataInitializerDevBehaviorTest.Config.class})
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:authDevBehaviorTest;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver"
})
class AuthReferenceDataInitializerDevBehaviorTest {

  @Autowired
  UserRepository userRepository;

  @Autowired
  AuthReferenceDataInitializer authReferenceDataInitializer;

  @Test
  void shouldCreateKnownSeedAdminWhenDefaultUsersEnabled() throws Exception {
    userRepository.deleteAll();
    authReferenceDataInitializer.run(null);

    assertThat(userRepository.findByEmailIgnoreCase("admin@erp.local")).isPresent();
    assertThat(userRepository.findByEmailIgnoreCase("caissier@erp.local")).isPresent();
  }

  static class Config {
    @Bean
    PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
    }

    @Bean
    SeedProperties seedProperties() {
      SeedProperties properties = new SeedProperties();
      properties.setDefaultUsersEnabled(true);
      return properties;
    }

    @Bean
    BootstrapAdminProperties bootstrapAdminProperties() {
      return new BootstrapAdminProperties();
    }

    @Bean
    AdminBootstrapService adminBootstrapService(
        BootstrapAdminProperties bootstrapAdminProperties,
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder) {
      return new AdminBootstrapService(bootstrapAdminProperties, userRepository, roleRepository, passwordEncoder);
    }

    @Bean
    AuthReferenceDataInitializer authReferenceDataInitializer(
        com.erp.products.repository.PermissionRepository permissionRepository,
        RoleRepository roleRepository,
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        SeedProperties seedProperties,
        AdminBootstrapService adminBootstrapService) {
      return new AuthReferenceDataInitializer(
          permissionRepository, roleRepository, userRepository, passwordEncoder, seedProperties, adminBootstrapService);
    }
  }
}
