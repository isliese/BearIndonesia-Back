package com.bearindonesia.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req) {
        AuthResponse resp = authService.register(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest req) {
        AuthResponse resp = authService.login(req);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        AuthUser user = SecurityUtils.requireUser();
        return ResponseEntity.ok(new UserResponse(user.id(), user.email(), user.name(), user.role()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        AuthUser user = SecurityUtils.requireUser();
        authService.changePassword(user.id(), req);
        return ResponseEntity.noContent().build();
    }
}
