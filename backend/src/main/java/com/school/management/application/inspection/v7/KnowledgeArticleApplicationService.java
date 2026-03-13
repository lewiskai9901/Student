package com.school.management.application.inspection.v7;

import com.school.management.domain.inspection.model.v7.knowledge.KnowledgeArticle;
import com.school.management.domain.inspection.repository.v7.KnowledgeArticleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class KnowledgeArticleApplicationService {

    private final KnowledgeArticleRepository articleRepository;

    @Transactional
    public KnowledgeArticle createArticle(String title, String content, String category,
                                          String tags, String relatedItemCodes,
                                          Long sourceCaseId, Long createdBy) {
        KnowledgeArticle article = KnowledgeArticle.reconstruct(KnowledgeArticle.builder()
                .title(title)
                .content(content)
                .category(category)
                .tags(tags)
                .relatedItemCodes(relatedItemCodes)
                .sourceCaseId(sourceCaseId)
                .createdBy(createdBy));
        KnowledgeArticle saved = articleRepository.save(article);
        log.info("Created knowledge article: id={}, title={}", saved.getId(), title);
        return saved;
    }

    @Transactional
    public KnowledgeArticle updateArticle(Long id, String title, String content,
                                          String category, String tags,
                                          String relatedItemCodes) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库文章不存在: " + id));
        article.update(title, content, category, tags, relatedItemCodes);
        articleRepository.save(article);
        log.info("Updated knowledge article: id={}", id);
        return article;
    }

    @Transactional
    public void deleteArticle(Long id) {
        articleRepository.deleteById(id);
        log.info("Deleted knowledge article: id={}", id);
    }

    @Transactional
    public KnowledgeArticle getArticle(Long id) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库文章不存在: " + id));
        article.incrementViewCount();
        articleRepository.save(article);
        return article;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeArticle> searchArticles(String keyword) {
        return articleRepository.search(keyword);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeArticle> getArticlesByCategory(String category) {
        return articleRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeArticle> getPinnedArticles() {
        return articleRepository.findPinned();
    }

    @Transactional(readOnly = true)
    public List<KnowledgeArticle> getAllArticles() {
        return articleRepository.findAll();
    }

    @Transactional
    public KnowledgeArticle markHelpful(Long id) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库文章不存在: " + id));
        article.incrementHelpfulCount();
        articleRepository.save(article);
        return article;
    }

    @Transactional
    public KnowledgeArticle pinArticle(Long id) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库文章不存在: " + id));
        article.pin();
        articleRepository.save(article);
        log.info("Pinned knowledge article: id={}", id);
        return article;
    }

    @Transactional
    public KnowledgeArticle unpinArticle(Long id) {
        KnowledgeArticle article = articleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("知识库文章不存在: " + id));
        article.unpin();
        articleRepository.save(article);
        log.info("Unpinned knowledge article: id={}", id);
        return article;
    }

    @Transactional
    public KnowledgeArticle createFromCase(Long caseId, String title, String content,
                                           String category, String tags, Long createdBy) {
        KnowledgeArticle article = KnowledgeArticle.reconstruct(KnowledgeArticle.builder()
                .title(title)
                .content(content)
                .category(category)
                .tags(tags)
                .sourceCaseId(caseId)
                .createdBy(createdBy));
        KnowledgeArticle saved = articleRepository.save(article);
        log.info("Created knowledge article from case: caseId={}, articleId={}", caseId, saved.getId());
        return saved;
    }
}
