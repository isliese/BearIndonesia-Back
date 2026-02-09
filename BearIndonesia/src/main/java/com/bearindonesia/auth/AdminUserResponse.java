package com.bearindonesia.auth;

import java.time.OffsetDateTime;

public record AdminUserResponse(
        Long id,
        String email,
        String name,
        UserRole role,
        OffsetDateTime createdAt,
        OffsetDateTime lastLoginAt
) {}

