package com.bearindonesia.auth;

public record UserResponse(Long id, String email, String name, UserRole role) {}
