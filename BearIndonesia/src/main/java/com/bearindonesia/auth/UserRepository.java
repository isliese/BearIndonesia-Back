package com.bearindonesia.auth;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<UserInfo> findByEmailAndPassword(String email, String password) {
        String sql = """
            SELECT id, username, name, role
            FROM users_info
            WHERE username = ?
              AND password_hash = crypt(?, password_hash)
            """;
        return jdbcTemplate.query(sql, userRowMapper(), email, password)
                .stream()
                .findFirst();
    }

    public UserInfo createUser(String email, String name, String password) {
        String sql = """
            INSERT INTO users_info (username, name, password_hash)
            VALUES (?, ?, crypt(?, gen_salt('bf')))
            RETURNING id, username, name, role
            """;
        return jdbcTemplate.queryForObject(sql, userRowMapper(), email, name, password);
    }

    public void updateLastLogin(Long userId) {
        jdbcTemplate.update("UPDATE users_info SET last_login_at = NOW() WHERE id = ?", userId);
    }

    public boolean updatePassword(Long userId, String currentPassword, String newPassword) {
        String sql = """
            UPDATE users_info
            SET password_hash = crypt(?, gen_salt('bf'))
            WHERE id = ?
              AND password_hash = crypt(?, password_hash)
            """;
        int updated = jdbcTemplate.update(sql, newPassword, userId, currentPassword);
        return updated > 0;
    }

    public List<AdminUserResponse> listUsers() {
        String sql = """
            SELECT id, username, name, role, created_at, last_login_at
            FROM users_info
            ORDER BY id ASC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new AdminUserResponse(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("name"),
                UserRole.fromDb(rs.getString("role")),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("last_login_at", OffsetDateTime.class)
        ));
    }

    public boolean updateRole(Long userId, UserRole role) {
        int updated = jdbcTemplate.update(
                "UPDATE users_info SET role = ? WHERE id = ?",
                (role == null ? UserRole.USER : role).name(),
                userId
        );
        return updated > 0;
    }

    private RowMapper<UserInfo> userRowMapper() {
        return (rs, rowNum) -> new UserInfo(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("name"),
                UserRole.fromDb(rs.getString("role"))
        );
    }
}
