package com.bearindonesia.service;

import com.bearindonesia.dto.CombinedNewsDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CombinedNewsService {

    // raw_news + processed_news 조인으로 응답할 컬럼 정의
    private static final String BASE_SELECT = """
        SELECT
            rn.id AS rn_id,
            rn.title AS rn_title,
            rn.link AS rn_link,
            rn.img AS rn_img,
            rn.content AS rn_content,
            rn.published_date AS rn_published_date,
            rn.source AS rn_source,
            rn.search_keyword AS rn_search_keyword,
            rn.language AS rn_language,
            rn.crawled_at AS rn_crawled_at,
            rn.raw_payload AS rn_raw_payload,
            pn.raw_news_id AS pn_raw_news_id,
            pn.kor_title AS pn_kor_title,
            pn.kor_summary AS pn_kor_summary,
            pn.kor_content AS pn_kor_content,
            pn.category AS pn_category,
            pn.eng_category AS pn_eng_category,
            pn.tags AS pn_tags,
            pn.importance AS pn_importance,
            pn.insight AS pn_insight,
            pn.is_pharma_related AS pn_is_pharma_related,
            pn.title_filter_reason AS pn_title_filter_reason,
            pn.model AS pn_model,
            pn.prompt_version AS pn_prompt_version,
            pn.tokens_in AS pn_tokens_in,
            pn.tokens_out AS pn_tokens_out,
            pn.status AS pn_status,
            pn.error_message AS pn_error_message,
            pn.processed_at AS pn_processed_at
        FROM raw_news rn
        JOIN processed_news pn ON pn.raw_news_id = rn.id
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CombinedNewsService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CombinedNewsDto> search(String query, String sortBy, String filterType) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        // 검색은 기존 정렬/필터 규칙을 유지하며 조인 결과 반환
        return queryCombined(query, sortBy, filterType, null);
    }

    public List<CombinedNewsDto> listNews(LocalDate date, String sortBy) {
        // 날짜 필터가 있으면 해당 날짜만, 없으면 전체 반환
        return queryCombined(null, sortBy, "all", date);
    }

    private List<CombinedNewsDto> queryCombined(
            String query,
            String sortBy,
            String filterType,
            LocalDate date) {
        StringBuilder sql = new StringBuilder(BASE_SELECT);
        // processed_news 중 제약 관련(true)만 노출
        sql.append(" WHERE pn.is_pharma_related = TRUE");

        MapSqlParameterSource params = new MapSqlParameterSource();

        if (query != null && !query.trim().isEmpty()) {
            // 제목/요약/본문 기준으로 검색
            sql.append("""
                AND (
                    LOWER(COALESCE(pn.kor_title, rn.title)) LIKE LOWER(CONCAT('%', :q, '%'))
                 OR LOWER(COALESCE(pn.kor_summary, pn.kor_content, rn.content)) LIKE LOWER(CONCAT('%', :q, '%'))
                )
            """);
            params.addValue("q", query);
        }

        if (filterType != null && !"all".equalsIgnoreCase(filterType)) {
            // 태그 문자열 포함 여부로 필터
            sql.append("""
                AND LOWER(COALESCE(CAST(pn.tags AS TEXT), '')) LIKE LOWER(CONCAT('%', :filterType, '%'))
            """);
            params.addValue("filterType", filterType);
        }

        if (date != null) {
            // 특정 날짜만 필터
            sql.append(" AND rn.published_date = :date");
            params.addValue("date", date);
        }

        sql.append(resolveOrderBy(sortBy));

        return jdbcTemplate.query(sql.toString(), params, new CombinedNewsRowMapper(objectMapper));
    }

    private static String resolveOrderBy(String sortBy) {
        String resolved = sortBy == null ? "relevance" : sortBy;
        if ("date".equalsIgnoreCase(resolved)) {
            // 날짜 기준 정렬
            return " ORDER BY rn.published_date DESC NULLS LAST, rn.id DESC";
        }
        if ("importance".equalsIgnoreCase(resolved)) {
            // 중요도 기준 정렬
            return " ORDER BY pn.importance DESC NULLS LAST, rn.id DESC";
        }
        // 기본(연관도) 정렬: 중요도 → 날짜 → id
        return " ORDER BY pn.importance DESC NULLS LAST, rn.published_date DESC NULLS LAST, rn.id DESC";
    }

    private static class CombinedNewsRowMapper implements RowMapper<CombinedNewsDto> {
        private final ObjectMapper objectMapper;

        private CombinedNewsRowMapper(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public CombinedNewsDto mapRow(ResultSet rs, int rowNum) throws SQLException {
            // 조인 결과를 DTO에 매핑
            CombinedNewsDto dto = new CombinedNewsDto();
            dto.id = getLong(rs, "rn_id");
            dto.title = rs.getString("rn_title");
            dto.link = rs.getString("rn_link");
            dto.img = rs.getString("rn_img");
            dto.content = rs.getString("rn_content");
            dto.publishedDate = getLocalDate(rs, "rn_published_date");
            dto.source = rs.getString("rn_source");
            dto.searchKeyword = rs.getString("rn_search_keyword");
            dto.language = rs.getString("rn_language");
            dto.crawledAt = getOffsetDateTime(rs, "rn_crawled_at");
            dto.rawPayload = readJson(rs.getString("rn_raw_payload"));

            dto.rawNewsId = getLong(rs, "pn_raw_news_id");
            dto.korTitle = rs.getString("pn_kor_title");
            dto.korSummary = rs.getString("pn_kor_summary");
            dto.korContent = rs.getString("pn_kor_content");
            dto.category = rs.getString("pn_category");
            dto.engCategory = rs.getString("pn_eng_category");
            dto.tags = readJson(rs.getString("pn_tags"));
            dto.importance = getInteger(rs, "pn_importance");
            dto.insight = rs.getString("pn_insight");
            dto.isPharmaRelated = getBoolean(rs, "pn_is_pharma_related");
            dto.titleFilterReason = rs.getString("pn_title_filter_reason");
            dto.model = rs.getString("pn_model");
            dto.promptVersion = rs.getString("pn_prompt_version");
            dto.tokensIn = getInteger(rs, "pn_tokens_in");
            dto.tokensOut = getInteger(rs, "pn_tokens_out");
            dto.status = rs.getString("pn_status");
            dto.errorMessage = rs.getString("pn_error_message");
            dto.processedAt = getOffsetDateTime(rs, "pn_processed_at");
            return dto;
        }

        private JsonNode readJson(String raw) {
            if (raw == null || raw.isBlank()) {
                return null;
            }
            try {
                return objectMapper.readTree(raw);
            } catch (Exception e) {
                // JSON 파싱 실패 시 null 처리
                return null;
            }
        }

        private static Long getLong(ResultSet rs, String column) throws SQLException {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        }

        private static Integer getInteger(ResultSet rs, String column) throws SQLException {
            int value = rs.getInt(column);
            return rs.wasNull() ? null : value;
        }

        private static Boolean getBoolean(ResultSet rs, String column) throws SQLException {
            boolean value = rs.getBoolean(column);
            return rs.wasNull() ? null : value;
        }

        private static LocalDate getLocalDate(ResultSet rs, String column) throws SQLException {
            Object value = rs.getObject(column);
            if (value == null) {
                return null;
            }
            if (value instanceof LocalDate) {
                return (LocalDate) value;
            }
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate();
            }
            return null;
        }

        private static OffsetDateTime getOffsetDateTime(ResultSet rs, String column) throws SQLException {
            Object value = rs.getObject(column);
            if (value == null) {
                return null;
            }
            if (value instanceof OffsetDateTime) {
                return (OffsetDateTime) value;
            }
            if (value instanceof java.sql.Timestamp) {
                // 타임스탬프는 UTC 기준으로 변환
                return ((java.sql.Timestamp) value).toInstant().atOffset(ZoneOffset.UTC);
            }
            return null;
        }
    }
}
