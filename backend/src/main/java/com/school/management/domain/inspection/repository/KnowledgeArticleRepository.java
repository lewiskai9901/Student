package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.knowledge.KnowledgeArticle;

import java.util.List;
import java.util.Optional;

public interface KnowledgeArticleRepository {

    KnowledgeArticle save(KnowledgeArticle article);

    Optional<KnowledgeArticle> findById(Long id);

    List<KnowledgeArticle> findAll();

    List<KnowledgeArticle> search(String keyword);

    List<KnowledgeArticle> findByCategory(String category);

    List<KnowledgeArticle> findPinned();

    void deleteById(Long id);
}
