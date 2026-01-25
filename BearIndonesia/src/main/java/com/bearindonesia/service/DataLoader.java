package com.bearindonesia.service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import com.bearindonesia.domain.Article;
import com.bearindonesia.repository.ArticleRepository;

@Component
public class DataLoader implements CommandLineRunner {

    private final ArticleRepository articleRepository;
    private final JdbcTemplate jdbcTemplate;

    public DataLoader(ArticleRepository articleRepository, JdbcTemplate jdbcTemplate) {
        this.articleRepository = articleRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        try {
            loadFromProcessedNews();
        } catch (Exception e) {
            System.err.println("Data load failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void loadFromProcessedNews() {
        try {
            int count = 0;
            for (Article processed : jdbcTemplate.query("SELECT * FROM processed_news", new ProcessedNewsRowMapper())) {
                if (processed.getLink() == null || processed.getLink().isBlank()) {
                    continue;
                }

                Article article = articleRepository.findByLink(processed.getLink()).orElseGet(Article::new);
                if (article.getId() == null && processed.getId() != null) {
                    article.setId(processed.getId());
                }
                if (article.getId() == null) {
                    article.setLink(processed.getLink());
                }

                article.setTitle(processed.getTitle());
                article.setKorTitle(processed.getKorTitle());
                article.setEngTitle(processed.getEngTitle());
                article.setContent(processed.getContent());
                article.setLink(processed.getLink());
                article.setDate(processed.getDate());
                article.setCategory(processed.getCategory());
                article.setEngCategory(processed.getEngCategory());
                article.setSource(processed.getSource());
                article.setKorSummary(processed.getKorSummary());
                article.setEngSummary(processed.getEngSummary());
                article.setTranslated(processed.getTranslated());
                article.setImportance(processed.getImportance());
                article.setImportanceRationale(processed.getImportanceRationale());
                article.setTagsJson(processed.getTagsJson());

                articleRepository.save(article);
                count++;
            }

            System.out.println("processed_news loaded: " + count + " rows");
        } catch (Exception e) {
            System.err.println("processed_news load failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static class ProcessedNewsRowMapper implements RowMapper<Article> {
        private Set<String> columns;

        @Override
        public Article mapRow(ResultSet rs, int rowNum) throws SQLException {
            if (columns == null) {
                columns = resolveColumns(rs);
            }

            Article article = new Article();
            article.setId(getLong(rs, columns, "id"));
            article.setTitle(getString(rs, columns, "title"));
            article.setKorTitle(getString(rs, columns, "kor_title", "korTitle"));
            article.setEngTitle(getString(rs, columns, "eng_title", "engTitle"));
            article.setContent(getString(rs, columns, "content"));
            article.setLink(getString(rs, columns, "link"));
            article.setDate(getLocalDate(rs, columns, "date"));
            article.setCategory(getString(rs, columns, "category"));
            article.setEngCategory(getString(rs, columns, "eng_category", "engCategory"));
            article.setSource(getString(rs, columns, "source"));
            article.setKorSummary(getString(rs, columns, "kor_summary", "korSummary"));
            article.setEngSummary(getString(rs, columns, "eng_summary", "engSummary"));
            article.setTranslated(getString(rs, columns, "translated"));
            article.setImportance(getInteger(rs, columns, "importance"));
            article.setImportanceRationale(getString(rs, columns, "importance_rationale", "importanceRationale"));
            article.setTagsJson(getString(rs, columns, "tags_json", "tagsJson", "tags"));
            return article;
        }

        private static Set<String> resolveColumns(ResultSet rs) throws SQLException {
            ResultSetMetaData meta = rs.getMetaData();
            Set<String> names = new HashSet<>();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                names.add(meta.getColumnLabel(i).toLowerCase());
            }
            return names;
        }

        private static String getString(ResultSet rs, Set<String> columns, String... names) throws SQLException {
            for (String name : names) {
                if (columns.contains(name.toLowerCase())) {
                    return rs.getString(name);
                }
            }
            return null;
        }

        private static Integer getInteger(ResultSet rs, Set<String> columns, String... names) throws SQLException {
            for (String name : names) {
                if (columns.contains(name.toLowerCase())) {
                    int value = rs.getInt(name);
                    return rs.wasNull() ? null : value;
                }
            }
            return null;
        }

        private static Long getLong(ResultSet rs, Set<String> columns, String... names) throws SQLException {
            for (String name : names) {
                if (columns.contains(name.toLowerCase())) {
                    long value = rs.getLong(name);
                    return rs.wasNull() ? null : value;
                }
            }
            return null;
        }

        private static LocalDate getLocalDate(ResultSet rs, Set<String> columns, String... names)
                throws SQLException {
            for (String name : names) {
                if (columns.contains(name.toLowerCase())) {
                    Object value = rs.getObject(name);
                    if (value == null) {
                        return null;
                    }
                    if (value instanceof LocalDate) {
                        return (LocalDate) value;
                    }
                    if (value instanceof java.sql.Date) {
                        return ((java.sql.Date) value).toLocalDate();
                    }
                }
            }
            return null;
        }
    }
}
