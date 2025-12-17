package com.example.demo.controller;

import com.example.demo.dto.NewsDto;
import com.example.demo.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final NewsService newsService;

    public IngestController(NewsService newsService) {
        this.newsService = newsService;
    }

    // 루트: 배열 [] 로 전송받음
    @PostMapping("/news")
    public ResponseEntity<?> ingestNews(@RequestBody List<NewsDto> items) {
        if (items == null || items.isEmpty()) {
            return ResponseEntity.badRequest().body("Empty payload");
        }
        int saved = newsService.upsertAll(items);
        return ResponseEntity.ok().body(String.format("OK: %d items ingested", saved));
    }
}
