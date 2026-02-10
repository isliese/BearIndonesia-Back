package com.bearindonesia.sales;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SalesReportRepository {

    private final JdbcTemplate jdbcTemplate;

    public SalesReportRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SalesReportListItem> listReports() {
        String sql = """
            SELECT
                sr.id,
                sr.title,
                sr.original_filename,
                sr.created_at,
                sr.updated_at,
                sr.created_by_user_id,
                u.name AS created_by_name,
                u.username AS created_by_email
            FROM sales_reports sr
            JOIN users_info u ON u.id = sr.created_by_user_id
            ORDER BY sr.created_at DESC, sr.id DESC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new SalesReportListItem(
                rs.getLong("id"),
                rs.getString("title"),
                rs.getString("original_filename"),
                rs.getLong("created_by_user_id"),
                rs.getString("created_by_name"),
                rs.getString("created_by_email"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        ));
    }

    public long create(
            String title,
            String originalFilename,
            byte[] rawExcel,
            String html,
            long createdByUserId
    ) {
        String sql = """
            INSERT INTO sales_reports (title, original_filename, file_blob, html, created_by_user_id)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id
            """;
        Long id = jdbcTemplate.queryForObject(sql, Long.class, title, originalFilename, rawExcel, html, createdByUserId);
        if (id == null) {
            throw new IllegalStateException("Failed to create report");
        }
        return id;
    }

    public Optional<String> findHtml(long reportId) {
        String sql = """
            SELECT html
            FROM sales_reports
            WHERE id = ?
            """;
        List<String> rows = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("html"), reportId);
        return rows.stream().findFirst();
    }
}
