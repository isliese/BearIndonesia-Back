package com.bearindonesia.scrap;

import com.bearindonesia.auth.AuthUser;
import com.bearindonesia.auth.SecurityUtils;
import com.bearindonesia.dto.ArticleDto;
import com.bearindonesia.service.ArticleService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/scrap")
public class ScrapController {

    private final UserScrapRepository userScrapRepository;
    private final ArticleService articleService;

    public ScrapController(UserScrapRepository userScrapRepository, ArticleService articleService) {
        this.userScrapRepository = userScrapRepository;
        this.articleService = articleService;
    }

    @PostMapping("/{rawNewsId}")
    public ResponseEntity<?> add(
            @PathVariable Long rawNewsId,
            @RequestBody(required = false) ScrapCreateRequest body
    ) {
        AuthUser user = SecurityUtils.requireUser();
        String comment = body == null ? null : body.comment;
        if (comment != null && comment.length() > 300) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "comment must be <= 300 characters");
        }
        userScrapRepository.addScrap(user.id(), rawNewsId, comment);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{rawNewsId}")
    public ResponseEntity<?> remove(@PathVariable Long rawNewsId) {
        AuthUser user = SecurityUtils.requireUser();
        userScrapRepository.removeScrap(user.id(), rawNewsId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<ArticleDto> list() {
        AuthUser user = SecurityUtils.requireUser();
        return articleService.listScrappedArticles(user.id());
    }
}
