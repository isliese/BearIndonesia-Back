package com.example.demo.service;

import com.example.demo.repository.NewsRepository;
import com.example.demo.entity.News;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SearchService {
    
    @Autowired
    private NewsRepository newsRepository;
    
    public List<News> searchNews(String query, String sortBy, String filterType) {
        System.out.println("🔍 [SERVICE] 검색 서비스 호출: " + query);
        
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        
        try {
            // filterType에 따라 다른 검색 메소드 호출
            if ("all".equals(filterType)) {
                if ("relevance".equals(sortBy)) {
                    return newsRepository.searchByRelevance(query);
                } else {
                    return newsRepository.searchByKeyword(query, sortBy);
                }
            } else {
                return newsRepository.searchWithFilter(query, filterType);
            }
        } catch (Exception e) {
            System.out.println("💥 [SERVICE] 검색 서비스 오류: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }
}