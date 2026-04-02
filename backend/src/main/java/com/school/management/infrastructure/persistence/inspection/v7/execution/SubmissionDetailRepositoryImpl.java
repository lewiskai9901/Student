package com.school.management.infrastructure.persistence.inspection.v7.execution;

import com.baomidou.mybatisplus.core.batch.MybatisBatch;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.domain.inspection.model.v7.execution.ScoringMode;
import com.school.management.domain.inspection.model.v7.execution.SubmissionDetail;
import com.school.management.domain.inspection.repository.v7.SubmissionDetailRepository;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class SubmissionDetailRepositoryImpl implements SubmissionDetailRepository {

    private final SubmissionDetailMapper mapper;
    private final SqlSessionFactory sqlSessionFactory;

    public SubmissionDetailRepositoryImpl(SubmissionDetailMapper mapper, SqlSessionFactory sqlSessionFactory) {
        this.mapper = mapper;
        this.sqlSessionFactory = sqlSessionFactory;
    }

    @Override
    public SubmissionDetail save(SubmissionDetail detail) {
        SubmissionDetailPO po = toPO(detail);
        if (detail.getId() == null) {
            mapper.insert(po);
            detail.setId(po.getId());
        } else {
            mapper.updateById(po);
        }
        return detail;
    }

    @Override
    public List<SubmissionDetail> saveAll(List<SubmissionDetail> details) {
        if (details == null || details.isEmpty()) {
            return details;
        }
        List<SubmissionDetailPO> poList = details.stream().map(this::toPO).collect(Collectors.toList());
        // Details don't need their generated IDs returned, so use MybatisBatch for true JDBC batching.
        MybatisBatch<SubmissionDetailPO> mybatisBatch = new MybatisBatch<>(sqlSessionFactory, poList);
        MybatisBatch.Method<SubmissionDetailPO> method = new MybatisBatch.Method<>(SubmissionDetailMapper.class);
        mybatisBatch.execute(method.insert());
        return details;
    }

    @Override
    public Optional<SubmissionDetail> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(this::toDomain);
    }

    @Override
    public List<SubmissionDetail> findBySubmissionId(Long submissionId) {
        return mapper.findBySubmissionId(submissionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public List<SubmissionDetail> findFlaggedBySubmissionId(Long submissionId) {
        return mapper.findFlaggedBySubmissionId(submissionId).stream().map(this::toDomain).collect(Collectors.toList());
    }

    @Override
    public void deleteById(Long id) {
        mapper.deleteById(id);
    }

    @Override
    public void deleteBySubmissionId(Long submissionId) {
        mapper.delete(new LambdaQueryWrapper<SubmissionDetailPO>().eq(SubmissionDetailPO::getSubmissionId, submissionId));
    }

    private SubmissionDetailPO toPO(SubmissionDetail d) {
        SubmissionDetailPO po = new SubmissionDetailPO();
        po.setId(d.getId());
        po.setTenantId(d.getTenantId() != null ? d.getTenantId() : 0L);
        po.setSubmissionId(d.getSubmissionId());
        po.setTemplateItemId(d.getTemplateItemId());
        po.setItemCode(d.getItemCode());
        po.setItemName(d.getItemName());
        po.setSectionId(d.getSectionId());
        po.setSectionName(d.getSectionName());
        po.setItemType(d.getItemType());
        po.setResponseValue(d.getResponseValue());
        po.setScoringMode(d.getScoringMode() != null ? d.getScoringMode().name() : null);
        po.setScore(d.getScore());
        po.setDimensions(d.getDimensions());
        po.setScoringConfig(d.getScoringConfig());
        po.setValidationRules(d.getValidationRules());
        po.setConditionLogic(d.getConditionLogic());
        po.setItemWeight(d.getItemWeight());
        po.setTimeSpentSeconds(d.getTimeSpentSeconds());
        po.setIsFlagged(d.getIsFlagged());
        po.setFlagReason(d.getFlagReason());
        po.setRemark(d.getRemark());
        po.setCreatedAt(d.getCreatedAt());
        po.setUpdatedAt(d.getUpdatedAt());
        return po;
    }

    private SubmissionDetail toDomain(SubmissionDetailPO po) {
        return SubmissionDetail.reconstruct(SubmissionDetail.builder()
                .id(po.getId())
                .tenantId(po.getTenantId())
                .submissionId(po.getSubmissionId())
                .templateItemId(po.getTemplateItemId())
                .itemCode(po.getItemCode())
                .itemName(po.getItemName())
                .sectionId(po.getSectionId())
                .sectionName(po.getSectionName())
                .itemType(po.getItemType())
                .responseValue(po.getResponseValue())
                .scoringMode(po.getScoringMode() != null ? ScoringMode.valueOf(po.getScoringMode()) : null)
                .score(po.getScore())
                .dimensions(po.getDimensions())
                .scoringConfig(po.getScoringConfig())
                .validationRules(po.getValidationRules())
                .conditionLogic(po.getConditionLogic())
                .itemWeight(po.getItemWeight())
                .timeSpentSeconds(po.getTimeSpentSeconds())
                .isFlagged(po.getIsFlagged())
                .flagReason(po.getFlagReason())
                .remark(po.getRemark())
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt()));
    }
}
