package com.erp.products.config;

import com.erp.products.repository.PermissionRepository;
import com.erp.products.repository.RoleRepository;
import com.erp.products.repository.UserRepository;
import com.erp.products.service.AdminBootstrapService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthReferenceDataInitializerProdBehaviorTest {

  @Mock
  PermissionRepository permissionRepository;

  @Mock
  RoleRepository roleRepository;

  @Mock
  UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @Test
  void shouldBootstrapAdminWithoutCreatingKnownSeedUsersWhenDefaultUsersDisabled() throws Exception {
    SeedProperties seedProperties = new SeedProperties();
    seedProperties.setDefaultUsersEnabled(false);

    AtomicBoolean bootstrapInvoked = new AtomicBoolean(false);
    AdminBootstrapService adminBootstrapService = new AdminBootstrapService(
        new BootstrapAdminProperties(), userRepository, roleRepository, passwordEncoder) {
      @Override
      public void bootstrapIfNeeded() {
        bootstrapInvoked.set(true);
      }
    };

    AuthReferenceDataInitializer initializer = new AuthReferenceDataInitializer(
        permissionRepository, roleRepository, userRepository, passwordEncoder, seedProperties, adminBootstrapService);

    when(permissionRepository.count()).thenReturn(1L);
    when(roleRepository.count()).thenReturn(1L);
    when(roleRepository.findByCode(anyString())).thenReturn(Optional.empty());
    when(roleRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

    initializer.run(null);

    assertThat(bootstrapInvoked).isTrue();
    verify(userRepository, never()).save(any());
  }
}
