package com.school.management.infrastructure.persistence.inspection.v7.knowledge;

import com.school.management.domain.inspection.model.v7.knowledge.KnowledgeArticle;
import com.school.management.domain.inspection.repository.v7.KnowledgeArticleRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class KnowledgeArticleRepositoryImpl implements KnowledgeArticleRepository {

    private final KnowledgeArticleMapper mapper;

    public KnowledgeArticleRepositoryImpl(KnowledgeArticleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public KnowledgeArticle save(KnowledgeArticle article) {
        KnowledgeArticlePO po = toPO(article);
        if (article.getId() == null) {
            mapper.insert(po);
            article.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return article;
    }

    @Override
    public Optional<KnowledgeArticle> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<KnowledgeArticle> findAll() {
        return mapper.selectList(null).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeArticle> search(String keyword) {
        return mapper.search(keyword).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeArticle> findByCategory(String category) {
        return mapper.findByCategory(category).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<KnowledgeArticle> findPinned() {
        return mapper.findPinned().stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    private KnowledgeArticlePO toPO(KnowledgeArticle d) {
        KnowledgeArticlePO po = new KnowledgeArticlePO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setTitle(d.getTitle());
        po.setContent(d.getContent());
        po.setCategory(d.getCategory());
        po.setTags(d.getTags());
        po.setRelatedItemCodes(d.getRelatedItemCodes());
        po.setSourceCaseId(d.getSourceCaseId());
        po.setViewCount(d.getViewCount());
        po.setHelpfulCount(d.getHelpfulCount());
        po.setIsPinned(d.getIsPinned());
        po.setCreatedBy(d.getCreatedBy());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private KnowledgeArticle toDomain(KnowledgeArticlePO po) {
        return KnowledgeArticle.reconstruct(KnowledgeArticle.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .title(po.getTitle())
                .content(po.getContent())
                .category(po.getCategory())
                .tags(po.getTags())
                .relatedItemCodes(po.getRelatedItemCodes())
                .sourceCaseId(po.getSourceCaseId())
                .viewCount(po.getViewCount())
                .helpfulCount(po.getHelpfulCount())
                .isPinned(po.getIsPinned())
                .createdBy(po.getCreatedBy())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
