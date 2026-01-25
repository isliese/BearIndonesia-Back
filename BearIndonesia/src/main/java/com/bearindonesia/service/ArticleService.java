package com.bearindonesia.service;

import com.bearindonesia.dto.ArticleDto;
import com.bearindonesia.dto.KeywordDto;
import com.bearindonesia.domain.Article;
import com.bearindonesia.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArticleService(ArticleRepository articleRepository, JdbcTemplate jdbcTemplate) {
        this.articleRepository = articleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Article> searchArticles(String query, String sortBy, String filterType) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        try {
            if ("all".equals(filterType)) {
                if ("relevance".equals(sortBy)) {
                    return articleRepository.searchByRelevance(query);
                } else {
                    return articleRepository.searchByKeyword(query, sortBy);
                }
            } else {
                return articleRepository.searchWithFilter(query, filterType);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return List.of();
        }
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
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                p.kor_title,
                p.kor_summary,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.importance_rationale,
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
                r.title,
                r.link,
                r.content,
                r.published_date,
                r.source,
                p.kor_title,
                p.kor_summary,
                p.kor_content,
                p.category,
                p.eng_category,
                p.importance,
                p.importance_rationale,
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

    private ArticleDto toDto(ResultSet rs) throws SQLException {
        ArticleDto dto = new ArticleDto();
        dto.title = rs.getString("title");
        dto.korTitle = rs.getString("kor_title");
        dto.engTitle = null;
        dto.link = rs.getString("link");
        dto.content = rs.getString("content");
        dto.date = rs.getDate("published_date") != null
            ? rs.getDate("published_date").toLocalDate()
            : null;
        dto.source = rs.getString("source");
        dto.category = rs.getString("category");
        dto.engCategory = rs.getString("eng_category");
        dto.korSummary = rs.getString("kor_summary");
        dto.engSummary = null;
        dto.translated = rs.getString("kor_content");
        dto.importance = rs.getObject("importance") == null ? null : rs.getInt("importance");
        dto.importanceRationale = rs.getString("importance_rationale");

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

    @Transactional
    public Article upsertByLink(ArticleDto dto) {
        Article article = articleRepository.findByLink(dto.link).orElseGet(Article::new);
        if (article.getId() == null) {
            article.setLink(dto.link);
        }

        article.setTitle(dto.title);
        article.setKorTitle(dto.korTitle);
        article.setEngTitle(dto.engTitle);
        article.setContent(dto.content);
        article.setDate(dto.date);
        article.setSource(dto.source);
        article.setCategory(dto.category);
        article.setEngCategory(dto.engCategory);
        article.setKorSummary(dto.korSummary);
        article.setEngSummary(dto.engSummary);
        article.setTranslated(dto.translated);
        article.setImportance(dto.importance);
        article.setImportanceRationale(dto.importanceRationale);

        if (dto.tags != null) {
            try {
                article.setTagsJson(objectMapper.writeValueAsString(dto.tags));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return articleRepository.save(article);
    }

    @Transactional
    public int upsertAll(List<ArticleDto> items) {
        int saved = 0;
        for (ArticleDto dto : items) {
            if (dto == null || dto.link == null || dto.link.isBlank()) continue;
            upsertByLink(dto);
            saved++;
        }
        return saved;
    }
}

