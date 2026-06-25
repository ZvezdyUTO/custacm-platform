package com.custacm.platform.auth.web;

import com.custacm.platform.auth.core.CurrentUser;
import com.custacm.platform.auth.core.CurrentUserExtractor;
import com.custacm.platform.auth.interfaceapi.CurrentUserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> currentUser(@AuthenticationPrincipal Jwt jwt) {
        CurrentUser user = CurrentUserExtractor.from(jwt);
        return ResponseEntity.ok(new CurrentUserResponse(
                user.studentIdentity(),
                user.role()
        ));
    }
}
