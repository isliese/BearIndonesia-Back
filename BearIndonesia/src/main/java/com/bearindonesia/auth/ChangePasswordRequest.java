package com.bearindonesia.auth;

public record ChangePasswordRequest(String currentPassword, String newPassword, String confirmPassword) {}
