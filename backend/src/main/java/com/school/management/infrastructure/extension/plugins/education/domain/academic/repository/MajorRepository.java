package com.school.management.domain.academic.repository;

import com.school.management.domain.academic.model.Major;
import com.school.management.domain.academic.model.MajorDirection;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 专业仓储接口
 */
public interface MajorRepository extends Repository<Major, Long> {

    /**
     * 根据专业编码查找
     */
    Optional<Major> findByMajorCode(String majorCode);

    /**
     * 根据组织单元ID查找专业列表
     */
    List<Major> findByOrgUnitId(Long orgUnitId);

    /**
     * 查找所有启用的专业
     */
    List<Major> findAllEnabled();

    /**
     * 检查专业编码是否存在
     */
    boolean existsByMajorCode(String majorCode);

    /**
     * 统计组织单元下的专业数量
     */
    int countByOrgUnitId(Long orgUnitId);

    /**
     * 根据专业方向ID查找所属专业
     */
    Optional<Major> findByDirectionId(Long directionId);

    /**
     * 根据专业方向ID查找专业方向
     */
    Optional<MajorDirection> findDirectionById(Long directionId);

    /**
     * 根据专业方向编码查找专业方向
     */
    Optional<MajorDirection> findDirectionByCode(String directionCode);

    /**
     * 查找专业的所有专业方向
     */
    List<MajorDirection> findDirectionsByMajorId(Long majorId);

    /**
     * 批量根据ID查找专业
     */
    List<Major> findByIds(java.util.Collection<Long> ids);

    /**
     * 批量根据ID查找专业方向
     */
    List<MajorDirection> findDirectionsByIds(java.util.Collection<Long> ids);
}
