package com.example.demo.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.example.demo.dto.NewsDto;
import com.example.demo.entity.News;
import com.example.demo.repository.NewsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;

@Service
public class NewsService {

    private final NewsRepository newsRepository;
    private final ObjectMapper objectMapper = new ObjectMapper(); // ✅ JSON 변환기

    public NewsService(NewsRepository newsRepository) {
        this.newsRepository = newsRepository;
    }

    @Transactional
    public News upsertByLink(NewsDto dto) {
        News news = newsRepository.findByLink(dto.link).orElseGet(News::new);
        if (news.getId() == null) {
            news.setLink(dto.link); // unique key
        }

        // 매핑
        news.setTitle(dto.title);
        news.setKorTitle(dto.korTitle);
        news.setEngTitle(dto.engTitle);
        news.setContent(dto.content);
        news.setDate(dto.date);   // ✅ dto.date는 이미 LocalDate
        news.setSource(dto.source);
        news.setCategory(dto.category);
        news.setEngCategory(dto.engCategory);
        news.setKorSummary(dto.korSummary);
        news.setEngSummary(dto.engSummary);
        news.setTranslated(dto.translated);
        news.setImportance(dto.importance);
        news.setImportanceRationale(dto.importanceRationale);

        // ✅ tags → JSON 문자열로 저장
        if (dto.tags != null) {
            try {
                news.setTagsJson(objectMapper.writeValueAsString(dto.tags));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return newsRepository.save(news);
    }

    // ✅ 여러 개 한 번에 저장하는 메서드 (IngestController에서 호출)
    @Transactional
    public int upsertAll(List<NewsDto> items) {
        int saved = 0;
        for (NewsDto dto : items) {
            if (dto == null || dto.link == null || dto.link.isBlank()) continue;
            upsertByLink(dto);
            saved++;
        }
        return saved;
    }
}
