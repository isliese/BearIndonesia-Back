package com.bearindonesia.service;

import com.bearindonesia.dto.ArticleDto;
import com.bearindonesia.dto.KeywordDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;

@Service
public class ArticleService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArticleService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ArticleDto> searchProcessedArticles(String query, String sortBy, String filterType) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        String normalizedSort = sortBy == null ? "relevance" : sortBy.toLowerCase();
        String normalizedFilter = filterType == null ? "all" : filterType.toLowerCase();
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM processed_news p
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE p.is_pharma_related IS TRUE
              AND (
                  LOWER(COALESCE(p.kor_title, r.title)) LIKE LOWER(CONCAT('%', ?, '%'))
               OR LOWER(COALESCE(p.kor_summary, p.kor_content, r.content)) LIKE LOWER(CONCAT('%', ?, '%'))
              )
              AND (
                  ? = 'all'
               OR LOWER(COALESCE(p.tags::text, '')) LIKE LOWER(CONCAT('%', ?, '%'))
              )
            ORDER BY
                CASE WHEN ? = 'date' THEN r.published_date END DESC NULLS LAST,
                CASE WHEN ? = 'importance' THEN p.importance END DESC NULLS LAST,
                p.importance DESC NULLS LAST,
                r.published_date DESC NULLS LAST,
                p.id DESC
            """;
        return jdbcTemplate.query(
            sql,
            (rs, rowNum) -> toDto(rs),
            query,
            query,
            normalizedFilter,
            normalizedFilter,
            normalizedSort,
            normalizedSort
        );
    }

    public List<ArticleDto> listProcessedArticles() {
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM processed_news p
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE p.is_pharma_related IS TRUE
            ORDER BY
                p.importance DESC NULLS LAST,
                r.published_date DESC NULLS LAST,
                p.id DESC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> toDto(rs));
    }

    public List<ArticleDto> listProcessedArticlesByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM processed_news p
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE p.is_pharma_related IS TRUE
              AND r.published_date BETWEEN ? AND ?
            ORDER BY
                r.published_date DESC NULLS LAST,
                p.id DESC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> toDto(rs), start, end);
    }

    public byte[] exportProcessedArticlesExcel(int year, int month) {
        List<ArticleDto> rows = listProcessedArticlesByMonth(year, month);
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(String.format("%04d-%02d", year, month));

            Row header = sheet.createRow(0);
            String[] columns = new String[] {
                "날짜",
                "언론사",
                "카테고리",
                "키워드",
                "헤드라인_국문",
                "요약_국문",
                "헤드라인_영문",
                "요약_영문",
                "본문_국문",
                "본문_인도네시아어",
                "링크",
                "중요도"
            };
            for (int i = 0; i < columns.length; i++) {
                header.createCell(i).setCellValue(columns[i]);
            }

            int rowIdx = 1;
            for (ArticleDto a : rows) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.date != null ? a.date.toString() : "");
                row.createCell(1).setCellValue(a.source != null ? a.source : "");
                row.createCell(2).setCellValue(a.category != null ? a.category : "");
                row.createCell(3).setCellValue(a.tags != null
                    ? a.tags.stream().map(t -> t.name).reduce("", (acc, name) -> acc.isEmpty() ? name : acc + ", " + name)
                    : "");
                row.createCell(4).setCellValue(a.korTitle != null ? a.korTitle : "");
                row.createCell(5).setCellValue(a.korSummary != null ? a.korSummary : "");
                row.createCell(6).setCellValue(a.engTitle != null ? a.engTitle : "");
                row.createCell(7).setCellValue(a.engSummary != null ? a.engSummary : "");
                row.createCell(8).setCellValue(a.korContent != null ? a.korContent : "");
                row.createCell(9).setCellValue(a.content != null ? a.content : "");
                row.createCell(10).setCellValue(a.link != null ? a.link : "");
                if (a.importance != null) {
                    row.createCell(11).setCellValue(a.importance.doubleValue());
                } else {
                    row.createCell(11).setCellValue("");
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("엑셀 생성에 실패했습니다.", e);
        }
    }

    public List<ArticleDto> listScrappedArticles(Long userId) {
        String sql = """
            SELECT
                p.id,
                p.raw_news_id,
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                r.img,
                p.kor_title,
                p.kor_summary,
                p.id_summary,
                p.semantic_confidence,
                p.tag_mismatch,
                p.category_mismatch,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.insight,
                p.tags
            FROM user_scrap s
            JOIN processed_news p ON p.raw_news_id = s.raw_news_id
            JOIN raw_news r ON r.id = p.raw_news_id
            WHERE s.user_id = ?
              AND p.is_pharma_related IS TRUE
            ORDER BY s.created_at DESC, p.id DESC
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> toDto(rs), userId);
    }

    private ArticleDto toDto(ResultSet rs) throws SQLException {
        ArticleDto dto = new ArticleDto();
        Object idObj = rs.getObject("id");
        dto.id = idObj == null ? null : ((Number) idObj).longValue();
        Object rawIdObj = rs.getObject("raw_news_id");
        dto.rawNewsId = rawIdObj == null ? null : ((Number) rawIdObj).longValue();
        dto.title = rs.getString("title");
        dto.korTitle = rs.getString("kor_title");
        dto.engTitle = null;
        dto.link = rs.getString("link");
        dto.content = rs.getString("content");
        dto.date = rs.getDate("published_date") != null
            ? rs.getDate("published_date").toLocalDate()
            : null;
        dto.source = rs.getString("source");
        dto.img = rs.getString("img");
        dto.category = rs.getString("category");
        dto.engCategory = rs.getString("eng_category");
        dto.korSummary = rs.getString("kor_summary");
        dto.engSummary = null;
        dto.idSummary = rs.getString("id_summary");
        Object confObj = rs.getObject("semantic_confidence");
        dto.semanticConfidence = confObj == null ? null : ((Number) confObj).floatValue();
        dto.tagMismatch = (Boolean) rs.getObject("tag_mismatch");
        dto.categoryMismatch = (Boolean) rs.getObject("category_mismatch");
        dto.korContent = rs.getString("kor_content");
        dto.importance = rs.getObject("importance") == null ? null : rs.getInt("importance");
        dto.insight = rs.getString("insight");

        String tagsJson = rs.getString("tags");
        dto.tags = parseTags(tagsJson);
        return dto;
    }

    private List<KeywordDto> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            List<?> raw = objectMapper.readValue(tagsJson, List.class);
            List<KeywordDto> out = new ArrayList<>();
            for (Object item : raw) {
                if (item instanceof String s && !s.isBlank()) {
                    KeywordDto k = new KeywordDto();
                    k.name = s;
                    out.add(k);
                } else if (item instanceof java.util.Map<?, ?> m) {
                    Object name = m.get("name");
                    if (name instanceof String s && !s.isBlank()) {
                        KeywordDto k = new KeywordDto();
                        k.name = s;
                        out.add(k);
                    }
                }
            }
            return out;
        } catch (Exception e) {
            return List.of();
        }
    }

}
