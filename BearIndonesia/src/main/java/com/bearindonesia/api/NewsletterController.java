package com.bearindonesia.api;

import com.bearindonesia.dto.NewsletterCoreNewsItemDto;
import com.bearindonesia.newsletter.NewsletterService;
import com.bearindonesia.service.ArticleService;
import java.time.YearMonth;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class NewsletterController {

    private final NewsletterService newsletterService;
    private final ArticleService articleService;

    public NewsletterController(NewsletterService newsletterService, ArticleService articleService) {
        this.newsletterService = newsletterService;
        this.articleService = articleService;
    }

    @GetMapping("/newsletter")
    public ResponseEntity<byte[]> getNewsletter(
            @RequestParam String year,
            @RequestParam String month
    ) {
        int yearValue = parseYear(year);
        int monthValue = parseMonth(month);
        YearMonth.of(yearValue, monthValue);

        ResponseEntity<byte[]> response = newsletterService.fetchNewsletter(yearValue, monthValue);
        MediaType contentType = response.getHeaders().getContentType();
        return ResponseEntity.ok()
                .contentType(contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM)
                .body(response.getBody());
    }

    @GetMapping("/newsletter/core-news")
    public List<NewsletterCoreNewsItemDto> getCoreNews(
            @RequestParam String year,
            @RequestParam String month,
            @RequestParam(required = false, defaultValue = "10") int limit
    ) {
        int yearValue = parseYear(year);
        int monthValue = parseMonth(month);
        YearMonth.of(yearValue, monthValue);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return articleService.listTopCoreNewsByMonth(yearValue, monthValue, safeLimit);
    }

    private int parseYear(String year) {
        if (year == null || !year.matches("\\d{4}")) {
            throw new IllegalArgumentException("연도 형식이 올바르지 않습니다. (YYYY)");
        }
        return Integer.parseInt(year);
    }

    private int parseMonth(String month) {
        if (month == null || !month.matches("\\d{2}")) {
            throw new IllegalArgumentException("월 형식이 올바르지 않습니다. (MM)");
        }
        int value = Integer.parseInt(month);
        if (value < 1 || value > 12) {
            throw new IllegalArgumentException("월은 01부터 12까지 입력해 주세요.");
        }
        return value;
    }
}
