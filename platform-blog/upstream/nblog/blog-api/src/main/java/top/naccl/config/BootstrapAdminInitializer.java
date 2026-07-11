package top.naccl.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import top.naccl.entity.User;
import top.naccl.mapper.UserMapper;
import top.naccl.util.HashUtils;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(BootstrapAdminInitializer.class);

    private final UserMapper userMapper;
    private final String username;
    private final String password;

    public BootstrapAdminInitializer(
            UserMapper userMapper,
            @Value("${blog.bootstrap-admin.username:}") String username,
            @Value("${blog.bootstrap-admin.password:}") String password
    ) {
        this.userMapper = userMapper;
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isEmpty() || password.isEmpty()) {
            log.info("Bootstrap administrator skipped, reason=credentials-not-configured");
            return;
        }
        if (userMapper.findByUsername(username) != null) {
            log.info("Bootstrap administrator skipped, reason=user-already-exists");
            return;
        }
        User admin = new User();
        admin.setUsername(username);
        admin.setPassword(HashUtils.getBC(password));
        admin.setNickname("Administrator");
		admin.setAvatar("");
		admin.setEmail("");
        admin.setRole("ROLE_admin");
        userMapper.insert(admin);
        log.info("Bootstrap administrator created, role=ROLE_admin");
    }
}
