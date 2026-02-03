package com.bearindonesia.api;

import com.bearindonesia.wordcloud.WordCloudRequest;
import com.bearindonesia.wordcloud.WordCloudService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WordCloudController {

    private final WordCloudService wordCloudService;

    public WordCloudController(WordCloudService wordCloudService) {
        this.wordCloudService = wordCloudService;
    }

    @PostMapping("/wordcloud")
    public ResponseEntity<byte[]> generateWordCloud(@RequestBody WordCloudRequest request) {
        validateRequest(request);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(wordCloudService.generate(request));
    }

    private void validateRequest(WordCloudRequest request) {
        if (request == null || request.startDate() == null || request.endDate() == null) {
            throw new IllegalArgumentException("기간을 선택해 주세요.");
        }
        if (request.startDate().isBlank() || request.endDate().isBlank()) {
            throw new IllegalArgumentException("기간을 선택해 주세요.");
        }
        try {
            LocalDate.parse(request.startDate());
            LocalDate.parse(request.endDate());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("기간 형식이 올바르지 않습니다. (YYYY-MM-DD)");
        }
    }
}
