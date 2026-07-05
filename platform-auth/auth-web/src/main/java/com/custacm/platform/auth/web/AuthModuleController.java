package com.custacm.platform.auth.web;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthModuleController {
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "service", "auth-web"
        );
    }

    @GetMapping("/module-info")
    public Map<String, Object> moduleInfo() {
        return Map.of(
                "module", "platform-auth",
                "service", "auth-web",
                "features", new String[]{"local-login", "rsa-jwt", "user-management", "current-user"}
        );
    }
}
