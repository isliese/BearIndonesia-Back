package com.bearindonesia.auth;

public record AuthResponse(String token, UserResponse user) {}
