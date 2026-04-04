package com.school.management.domain.student.repository;

import com.school.management.domain.student.model.GradeOpenedDirection;
import com.school.management.domain.shared.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 年级开设专业方向仓储接口
 */
public interface GradeOpenedDirectionRepository extends Repository<GradeOpenedDirection, Long> {

    /**
     * 根据年级ID查找开设的专业方向
     */
    List<GradeOpenedDirection> findByGradeId(Long gradeId);

    /**
     * 根据专业方向ID查找开设的年级
     */
    List<GradeOpenedDirection> findByMajorDirectionId(Long majorDirectionId);

    /**
     * 根据年级ID和专业方向ID查找
     */
    Optional<GradeOpenedDirection> findByGradeIdAndMajorDirectionId(Long gradeId, Long majorDirectionId);

    /**
     * 检查年级是否开设了某专业方向
     */
    boolean existsByGradeIdAndMajorDirectionId(Long gradeId, Long majorDirectionId);

    /**
     * 统计年级开设的专业方向数量
     */
    int countByGradeId(Long gradeId);

    /**
     * 统计专业方向被多少个年级开设
     */
    int countByMajorDirectionId(Long majorDirectionId);

    /**
     * 删除年级下所有开设记录
     */
    void deleteByGradeId(Long gradeId);

    /**
     * 删除专业方向的所有开设记录
     */
    void deleteByMajorDirectionId(Long majorDirectionId);
}
