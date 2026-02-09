package com.bearindonesia.auth;

public enum UserRole {
    USER,
    ADMIN;

    public static UserRole fromDb(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        try {
            return UserRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return USER;
        }
    }
}

