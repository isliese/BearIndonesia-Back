package com.bearindonesia.auth;

public record UserInfo(Long id, String email, String name, UserRole role) {}
