package com.bearindonesia.auth;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthService(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    public AuthResponse register(RegisterRequest req) {
        validateRegister(req);
        UserInfo user;
        try {
            user = userRepository.createUser(req.email().trim(), req.name().trim(), req.password());
        } catch (DuplicateKeyException e) {
            throw new UnauthorizedException("이미 사용 중인 이메일입니다.");
        }
        String token = jwtService.createToken(user.id(), user.email(), user.name());
        return new AuthResponse(token, new UserResponse(user.id(), user.email(), user.name()));
    }

    public AuthResponse login(LoginRequest req) {
        validateLogin(req);
        UserInfo user = userRepository
                .findByEmailAndPassword(req.email().trim(), req.password())
                .orElseThrow(() -> new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다."));
        userRepository.updateLastLogin(user.id());
        String token = jwtService.createToken(user.id(), user.email(), user.name());
        return new AuthResponse(token, new UserResponse(user.id(), user.email(), user.name()));
    }

    private void validateRegister(RegisterRequest req) {
        if (req == null || req.email() == null || req.name() == null || req.password() == null) {
            throw new UnauthorizedException("필수 값이 누락되었습니다.");
        }
        if (req.email().isBlank() || req.name().isBlank() || req.password().isBlank()) {
            throw new UnauthorizedException("필수 값이 비어 있습니다.");
        }
    }

    private void validateLogin(LoginRequest req) {
        if (req == null || req.email() == null || req.password() == null) {
            throw new UnauthorizedException("필수 값이 누락되었습니다.");
        }
        if (req.email().isBlank() || req.password().isBlank()) {
            throw new UnauthorizedException("필수 값이 비어 있습니다.");
        }
    }
}
