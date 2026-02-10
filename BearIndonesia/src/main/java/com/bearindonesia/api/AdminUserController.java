package com.bearindonesia.api;

import com.bearindonesia.auth.AdminUserResponse;
import com.bearindonesia.auth.SecurityUtils;
import com.bearindonesia.auth.UpdateUserRoleRequest;
import com.bearindonesia.auth.UserRepository;
import com.bearindonesia.auth.UserRole;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/admin", "/api/admin"})
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/users")
    public List<AdminUserResponse> listUsers() {
        SecurityUtils.requireAdmin();
        return userRepository.listUsers();
    }

    @PatchMapping("/users/{id}/role")
    public ResponseEntity<Void> updateRole(
            @PathVariable Long id,
            @RequestBody UpdateUserRoleRequest req
    ) {
        SecurityUtils.requireAdmin();
        if (req == null || req.role() == null || req.role().isBlank()) {
            throw new IllegalArgumentException("role is required");
        }
        UserRole role = UserRole.fromDb(req.role());
        boolean updated = userRepository.updateRole(id, role);
        if (!updated) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.noContent().build();
    }
}
