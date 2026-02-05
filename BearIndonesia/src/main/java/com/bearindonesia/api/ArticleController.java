package com.bearindonesia.api;

import com.bearindonesia.dto.ArticleDto;
import com.bearindonesia.service.ArticleService;
import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/search")
    public SearchResponse search(@RequestBody SearchRequest req) {
        String query = req.getQuery();
        String sortBy = req.getSortBy() != null ? req.getSortBy() : "relevance";
        String filterType = req.getFilterType() != null ? req.getFilterType() : "all";

        List<ArticleDto> results = articleService.searchProcessedArticles(query, sortBy, filterType);
        SearchResponse resp = new SearchResponse();
        resp.setResults(results);
        return resp;
    }

    @GetMapping("/articles")
    public List<ArticleDto> listArticles() {
        return articleService.listProcessedArticles();
    }

    @GetMapping("/articles/excel")
    public ResponseEntity<byte[]> downloadExcel(
            @RequestParam int year,
            @RequestParam int month
    ) {
        byte[] data = articleService.exportProcessedArticlesExcel(year, month);
        String filename = String.format("%04d-%02d-news.xlsx", year, month);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(data);
    }

    @Data
    public static class SearchRequest {
        private String query;
        private String sortBy;
        private String filterType;
    }

    @Data
    public static class SearchResponse {
        private List<ArticleDto> results;
    }
}
