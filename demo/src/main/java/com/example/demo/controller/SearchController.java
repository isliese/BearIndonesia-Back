// src/main/java/com/example/demo/controller/SearchController.java
package com.example.demo.controller;

import com.example.demo.entity.News;
import com.example.demo.service.SearchService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class SearchController {

    private final SearchService searchService;
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @PostMapping("/search")
    public SearchResponse search(@RequestBody SearchRequest req) {
        String query = req.getQuery();
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "relevance";
        String filterType = req.getFilterType() != null ? req.getFilterType() : "all";

        List<News> results = searchService.searchNews(query, sortBy, filterType);
        SearchResponse resp = new SearchResponse();
        resp.setResults(results);
        return resp; // 프론트가 기대하는 { results: [...] }
    }

    @Data
    public static class SearchRequest {
        private String query;
        private String sortBy;
        private String filterType;
    }

    @Data
    public static class SearchResponse {
        private List<News> results;
    }
}
