package com.example.bearindonesia.repository;

import com.example.bearindonesia.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByLink(String link);

    @Query("""
        SELECT a FROM Article a
        WHERE
            LOWER(COALESCE(a.korTitle, a.title)) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(COALESCE(a.korSummary, COALESCE(a.engSummary, a.translated))) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY
            a.importance DESC NULLS LAST,
            a.date DESC NULLS LAST,
            a.id DESC
    """)
    List<Article> searchByRelevance(@Param("q") String q);

    @Query("""
        SELECT a FROM Article a
        WHERE
            LOWER(COALESCE(a.korTitle, a.title)) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(COALESCE(a.korSummary, COALESCE(a.engSummary, a.translated))) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY
            CASE WHEN :sortBy = 'date' THEN a.date END DESC,
            CASE WHEN :sortBy = 'importance' THEN a.importance END DESC,
            a.id DESC
    """)
    List<Article> searchByKeyword(@Param("q") String q, @Param("sortBy") String sortBy);

    @Query("""
        SELECT a FROM Article a
        WHERE
           (LOWER(COALESCE(a.korTitle, a.title)) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(COALESCE(a.korSummary, COALESCE(a.engSummary, a.translated))) LIKE LOWER(CONCAT('%', :q, '%')))
         AND (:filterType = 'all'
              OR LOWER(COALESCE(a.tagsJson, '')) LIKE LOWER(CONCAT('%', :filterType, '%')))
        ORDER BY
            a.importance DESC NULLS LAST,
            a.date DESC NULLS LAST,
            a.id DESC
    """)
    List<Article> searchWithFilter(@Param("q") String q, @Param("filterType") String filterType);
}
