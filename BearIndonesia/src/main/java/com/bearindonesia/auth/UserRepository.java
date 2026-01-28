package com.bearindonesia.auth;

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
            SELECT id, username, name
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
            RETURNING id, username, name
            """;
        return jdbcTemplate.queryForObject(sql, userRowMapper(), email, name, password);
    }

    public void updateLastLogin(Long userId) {
        jdbcTemplate.update("UPDATE users_info SET last_login_at = NOW() WHERE id = ?", userId);
    }

    private RowMapper<UserInfo> userRowMapper() {
        return (rs, rowNum) -> new UserInfo(
                rs.getLong("id"),
                rs.getString("username"),
                rs.getString("name")
        );
    }
}
