package com.example.bearindonesia.service;

import com.example.bearindonesia.dto.ArticleDto;
import com.example.bearindonesia.domain.Article;
import com.example.bearindonesia.repository.ArticleRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ArticleService(ArticleRepository articleRepository) {
        this.articleRepository = articleRepository;
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
