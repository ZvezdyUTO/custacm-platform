package top.naccl.config;

import org.junit.jupiter.api.Test;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.util.HashUtils;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BootstrapAdminInitializerTest {
    @Test
    void createsFirstAdminWithoutOverwritingExistingUser() {
        UserMapper mapper = mock(UserMapper.class);
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(mapper, "root-admin", "safe-password");
        when(mapper.findByUsername("root-admin")).thenReturn(null);

        initializer.run(null);

        verify(mapper).insert(argThat(user -> "ROLE_admin".equals(user.getRole())
                && HashUtils.matchBC("safe-password", user.getPassword())));

        User existing = new User();
        existing.setUsername("root-admin");
        when(mapper.findByUsername("root-admin")).thenReturn(existing);
        initializer.run(null);
        verify(mapper, never()).updateAdminFields(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
