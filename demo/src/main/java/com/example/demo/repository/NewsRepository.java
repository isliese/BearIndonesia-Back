// src/main/java/com/example/demo/repository/NewsRepository.java
package com.example.demo.repository;

import com.example.demo.entity.News;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface NewsRepository extends JpaRepository<News, Long> {

    // 단건 중복 체크/업서트 등에 사용
    Optional<News> findByLink(String link);

    // 관련성순: 중요도 → 날짜 → id
    @Query("""
        SELECT n FROM News n
        WHERE
            LOWER(COALESCE(n.korTitle, n.title)) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(COALESCE(n.korSummary, COALESCE(n.engSummary, n.translated))) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY
            n.importance DESC NULLS LAST,
            n.date DESC NULLS LAST,
            n.id DESC
    """)
    List<News> searchByRelevance(@Param("q") String q);

    // 정렬키 기반: date / importance (그 외는 id DESC로 폴백)
    @Query("""
        SELECT n FROM News n
        WHERE
            LOWER(COALESCE(n.korTitle, n.title)) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(COALESCE(n.korSummary, COALESCE(n.engSummary, n.translated))) LIKE LOWER(CONCAT('%', :q, '%'))
        ORDER BY
            CASE WHEN :sortBy = 'date' THEN n.date END DESC,
            CASE WHEN :sortBy = 'importance' THEN n.importance END DESC,
            n.id DESC
    """)
    List<News> searchByKeyword(@Param("q") String q, @Param("sortBy") String sortBy);

    // 필터: tagsJson 문자열에서 포함 여부로 간단 필터 (all이면 패스)
    @Query("""
        SELECT n FROM News n
        WHERE
           (LOWER(COALESCE(n.korTitle, n.title)) LIKE LOWER(CONCAT('%', :q, '%'))
         OR LOWER(COALESCE(n.korSummary, COALESCE(n.engSummary, n.translated))) LIKE LOWER(CONCAT('%', :q, '%')))
         AND (:filterType = 'all'
              OR LOWER(COALESCE(n.tagsJson, '')) LIKE LOWER(CONCAT('%', :filterType, '%')))
        ORDER BY
            n.importance DESC NULLS LAST,
            n.date DESC NULLS LAST,
            n.id DESC
    """)
    List<News> searchWithFilter(@Param("q") String q, @Param("filterType") String filterType);
}
