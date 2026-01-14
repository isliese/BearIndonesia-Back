package com.example.bearindonesia.ingest;

import com.example.bearindonesia.dto.ArticleDto;
import com.example.bearindonesia.service.ArticleService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final ArticleService articleService;

    public IngestController(ArticleService articleService) {
        this.articleService = articleService;
    }

    @PostMapping("/news")
    public ResponseEntity<?> ingestNews(@RequestBody List<ArticleDto> items) {
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body("Empty payload");
        }
        int saved = articleService.upsertAll(items);
        return ResponseEntity.ok().body(String.format("OK: %d items ingested", saved));
    }
}
