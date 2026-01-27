package com.bearindonesia.api;

import com.bearindonesia.dto.CombinedNewsDto;
import com.bearindonesia.service.CombinedNewsService;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final CombinedNewsService combinedNewsService;

    public ArticleController(CombinedNewsService combinedNewsService) {
        this.combinedNewsService = combinedNewsService;
    }

    @PostMapping("/search")
    public SearchResponse search(@RequestBody SearchRequest req) {
        // 검색 엔드포인트
        String query = req.getQuery();
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "relevance";
        String filterType = req.getFilterType() != null ? req.getFilterType() : "all";

        List<CombinedNewsDto> results = combinedNewsService.search(query, sortBy, filterType);
        SearchResponse resp = new SearchResponse();
        resp.setResults(results);
        return resp;
    }

    @GetMapping("/news")
    public NewsResponse news(
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String sortBy) {
        // 전체 뉴스 엔드포인트: 날짜 선택 가능
        LocalDate parsedDate = null;
        if (date != null && !date.isBlank()) {
            parsedDate = LocalDate.parse(date);
        }
        String resolvedSortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "relevance";
        List<CombinedNewsDto> results = combinedNewsService.listNews(parsedDate, resolvedSortBy);
        NewsResponse resp = new NewsResponse();
        resp.setResults(results);
        return resp;
    }

    @Data
    public static class SearchRequest {
        private String query;
        private String sortBy;
        private String filterType;
    }

    @Data
    public static class SearchResponse {
        private List<CombinedNewsDto> results;
    }

    @Data
    public static class NewsResponse {
        private List<CombinedNewsDto> results;
    }
}
