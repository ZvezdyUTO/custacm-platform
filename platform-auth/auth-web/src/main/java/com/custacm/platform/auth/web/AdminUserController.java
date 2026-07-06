package com.custacm.platform.auth.web;

import com.custacm.platform.auth.app.exception.AuthErrorCode;
import com.custacm.platform.auth.app.exception.AuthServiceException;
import com.custacm.platform.auth.app.service.AdminUserService;
import com.custacm.platform.auth.core.CurrentUser;
import com.custacm.platform.auth.core.CurrentUserExtractor;
import com.custacm.platform.auth.domain.model.UserRole;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @PostMapping("/admin/users")
    public ResponseEntity<UserOperationResponse> createUser(@RequestBody CreateUserRequest request) {
        if (request == null) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "request body must not be empty");
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponseMapper.toOperationResponse(adminUserService.createUser(
                request.studentIdentity(),
                request.password(),
                UserRole.fromValue(request.role())
        )));
    }

    @PostMapping("/admin/users:batch-create")
    public ResponseEntity<List<UserOperationResponse>> batchCreateUsers(@RequestBody BatchCreateUsersRequest request) {
        List<AdminUserService.CreateUserCommand> commands = request == null || request.users() == null
                ? null
                : request.users().stream()
                        .map(user -> user == null ? null : new AdminUserService.CreateUserCommand(
                                user.studentIdentity(),
                                user.password(),
                                user.role()
                        ))
                        .toList();
        List<UserOperationResponse> responses = adminUserService.createUsers(commands).stream()
                .map(UserResponseMapper::toOperationResponse)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(responses);
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserResponse>> listUsers() {
        return ResponseEntity.ok(adminUserService.listUsers().stream()
                .map(UserResponseMapper::toResponse)
                .toList());
    }

    @PatchMapping("/admin/users/{studentIdentity}")
    public ResponseEntity<UserOperationResponse> updateUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("studentIdentity") String studentIdentity,
            @RequestBody UpdateUserRequest request
    ) {
        if (request == null) {
            throw new AuthServiceException(AuthErrorCode.AUTH_INVALID_REQUEST, "request body must not be empty");
        }
        CurrentUser currentUser = CurrentUserExtractor.from(jwt);
        return ResponseEntity.ok(UserResponseMapper.toOperationResponse(adminUserService.updateUser(
                currentUser.studentIdentity(),
                studentIdentity,
                new AdminUserService.UpdateUserCommand(optionalAccountRole(request.role()), request.newPassword())
        )));
    }

    private static UserRole optionalAccountRole(String role) {
        if (role == null || role.isBlank() || "guest".equalsIgnoreCase(role.trim())) {
            return null;
        }
        return UserRole.fromValue(role);
    }

    @DeleteMapping("/admin/users/{studentIdentity}")
    public ResponseEntity<UserOperationResponse> deleteUser(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable("studentIdentity") String studentIdentity
    ) {
        CurrentUser currentUser = CurrentUserExtractor.from(jwt);
        return ResponseEntity.ok(UserResponseMapper.toOperationResponse(
                adminUserService.deleteUser(currentUser.studentIdentity(), studentIdentity)
        ));
    }
}
