package com.school.management.interfaces.rest.inspection.v7;

import com.school.management.application.inspection.v7.KnowledgeArticleApplicationService;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.domain.inspection.model.v7.knowledge.KnowledgeArticle;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v7/insp/knowledge-articles")
public class KnowledgeArticleController {

    private final KnowledgeArticleApplicationService articleService;

    @GetMapping
    @CasbinAccess(resource = "insp:knowledge", action = "view")
    public Result<List<KnowledgeArticle>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        if (keyword != null && !keyword.isBlank()) {
            return Result.success(articleService.searchArticles(keyword));
        }
        if (category != null && !category.isBlank()) {
            return Result.success(articleService.getArticlesByCategory(category));
        }
        return Result.success(articleService.getAllArticles());
    }

    @PostMapping
    @CasbinAccess(resource = "insp:knowledge", action = "create")
    public Result<KnowledgeArticle> create(@RequestBody CreateArticleRequest request) {
        KnowledgeArticle article = articleService.createArticle(
                request.getTitle(), request.getContent(), request.getCategory(),
                request.getTags(), request.getRelatedItemCodes(),
                request.getSourceCaseId(), SecurityUtils.getCurrentUserId());
        return Result.success(article);
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "insp:knowledge", action = "manage")
    public Result<KnowledgeArticle> update(@PathVariable Long id,
                                           @RequestBody UpdateArticleRequest request) {
        KnowledgeArticle article = articleService.updateArticle(id,
                request.getTitle(), request.getContent(), request.getCategory(),
                request.getTags(), request.getRelatedItemCodes());
        return Result.success(article);
    }

    @GetMapping("/{id}")
    @CasbinAccess(resource = "insp:knowledge", action = "view")
    public Result<KnowledgeArticle> getById(@PathVariable Long id) {
        return Result.success(articleService.getArticle(id));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "insp:knowledge", action = "manage")
    public Result<Void> delete(@PathVariable Long id) {
        articleService.deleteArticle(id);
        return Result.success(null);
    }

    @GetMapping("/pinned")
    @CasbinAccess(resource = "insp:knowledge", action = "view")
    public Result<List<KnowledgeArticle>> pinned() {
        return Result.success(articleService.getPinnedArticles());
    }

    @PostMapping("/{id}/helpful")
    @CasbinAccess(resource = "insp:knowledge", action = "view")
    public Result<KnowledgeArticle> markHelpful(@PathVariable Long id) {
        return Result.success(articleService.markHelpful(id));
    }

    @PutMapping("/{id}/pin")
    @CasbinAccess(resource = "insp:knowledge", action = "manage")
    public Result<KnowledgeArticle> pin(@PathVariable Long id) {
        return Result.success(articleService.pinArticle(id));
    }

    @PutMapping("/{id}/unpin")
    @CasbinAccess(resource = "insp:knowledge", action = "manage")
    public Result<KnowledgeArticle> unpin(@PathVariable Long id) {
        return Result.success(articleService.unpinArticle(id));
    }

    @PostMapping("/from-case/{caseId}")
    @CasbinAccess(resource = "insp:knowledge", action = "create")
    public Result<KnowledgeArticle> createFromCase(@PathVariable Long caseId,
                                                    @RequestBody CreateFromCaseRequest request) {
        KnowledgeArticle article = articleService.createFromCase(caseId,
                request.getTitle(), request.getContent(), request.getCategory(),
                request.getTags(), SecurityUtils.getCurrentUserId());
        return Result.success(article);
    }

    // ========== Request DTOs ==========

    @lombok.Data
    public static class CreateArticleRequest {
        private String title;
        private String content;
        private String category;
        private String tags;
        private String relatedItemCodes;
        private Long sourceCaseId;
    }

    @lombok.Data
    public static class UpdateArticleRequest {
        private String title;
        private String content;
        private String category;
        private String tags;
        private String relatedItemCodes;
    }

    @lombok.Data
    public static class CreateFromCaseRequest {
        private String title;
        private String content;
        private String category;
        private String tags;
    }
}
