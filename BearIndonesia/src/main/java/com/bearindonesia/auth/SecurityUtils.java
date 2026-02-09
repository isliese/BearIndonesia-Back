package com.bearindonesia.auth;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static AuthUser requireUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
            throw new UnauthorizedException("인증이 필요합니다.");
        }
        return user;
    }

    public static AuthUser requireAdmin() {
        AuthUser user = requireUser();
        if (user.role() != UserRole.ADMIN) {
            throw new ForbiddenException("권한이 없습니다.");
        }
        return user;
    }
}
