package com.bearindonesia.scrap;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class UserScrapRepository {

    private final JdbcTemplate jdbcTemplate;

    public UserScrapRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void addScrap(Long userId, Long rawNewsId, String comment) {
        String sql = """
            INSERT INTO user_scrap (user_id, raw_news_id, comment)
            VALUES (?, ?, ?)
            ON CONFLICT (user_id, raw_news_id) DO UPDATE
            SET comment = CASE
                WHEN EXCLUDED.comment IS NULL THEN user_scrap.comment
                ELSE EXCLUDED.comment
            END
            """;
        jdbcTemplate.update(sql, userId, rawNewsId, comment);
    }

    public void removeScrap(Long userId, Long rawNewsId) {
        jdbcTemplate.update(
                "DELETE FROM user_scrap WHERE user_id = ? AND raw_news_id = ?",
                userId,
                rawNewsId
        );
    }

    public List<Long> listScrapRawNewsIds(Long userId) {
        return jdbcTemplate.query(
                "SELECT raw_news_id FROM user_scrap WHERE user_id = ? ORDER BY created_at DESC",
                (rs, rowNum) -> rs.getLong("raw_news_id"),
                userId
        );
    }
}
