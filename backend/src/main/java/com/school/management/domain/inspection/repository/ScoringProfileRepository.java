package com.school.management.domain.inspection.repository;

import com.school.management.domain.inspection.model.scoring.ScoringProfile;

import java.util.List;
import java.util.Optional;

public interface ScoringProfileRepository {

    ScoringProfile save(ScoringProfile profile);

    Optional<ScoringProfile> findById(Long id);

    /**
     * 项目-owned 重构后, 同 sectionId 可能在多项目下并存, 此方法只返回首个匹配.
     * 新代码请用 {@link #findByProjectIdAndSectionId}.
     * @deprecated 语义不唯一, 仅供旧兼容路径.
     */
    @Deprecated
    Optional<ScoringProfile> findBySectionId(Long sectionId);

    /** 列出某项目下所有评分方案. */
    List<ScoringProfile> findByProjectId(Long projectId);

    /** 按 (项目, 分区) 唯一定位评分方案. */
    Optional<ScoringProfile> findByProjectIdAndSectionId(Long projectId, Long sectionId);

    List<ScoringProfile> findAll();

    void deleteById(Long id);

    /** 物理删除某项目下所有评分方案 (供项目级联删除使用). */
    int deleteByProjectId(Long projectId);
}
