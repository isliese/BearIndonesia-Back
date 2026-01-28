package com.bearindonesia.scrap;

import com.bearindonesia.auth.AuthUser;
import com.bearindonesia.auth.SecurityUtils;
import com.bearindonesia.dto.ArticleDto;
import com.bearindonesia.service.ArticleService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<?> add(@PathVariable Long rawNewsId) {
        AuthUser user = SecurityUtils.requireUser();
        userScrapRepository.addScrap(user.id(), rawNewsId);
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
