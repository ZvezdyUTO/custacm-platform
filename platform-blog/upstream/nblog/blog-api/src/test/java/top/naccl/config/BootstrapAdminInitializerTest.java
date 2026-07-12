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
        BootstrapAdminInitializer initializer = new BootstrapAdminInitializer(mapper, "safe-password");
        when(mapper.findByUsername("root")).thenReturn(null);

        initializer.run(null);

        verify(mapper).insert(argThat(user -> "root".equals(user.getUsername())
                && "ROLE_admin".equals(user.getRole())
                && "".equals(user.getAvatar())
                && HashUtils.matchBC("safe-password", user.getPassword())));

        User existing = new User();
        existing.setUsername("root");
        when(mapper.findByUsername("root")).thenReturn(existing);
        initializer.run(null);
        verify(mapper, never()).updateAdminFields(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }
}
