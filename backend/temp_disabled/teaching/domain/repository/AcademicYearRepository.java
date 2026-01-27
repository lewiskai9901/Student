package com.school.management.domain.teaching.repository;

import com.school.management.domain.teaching.model.aggregate.AcademicYear;

import java.util.List;
import java.util.Optional;

/**
 * 学年仓储接口
 */
public interface AcademicYearRepository {

    /**
     * 保存学年
     */
    AcademicYear save(AcademicYear academicYear);

    /**
     * 根据ID查询
     */
    Optional<AcademicYear> findById(Long id);

    /**
     * 根据学年代码查询
     */
    Optional<AcademicYear> findByYearCode(String yearCode);

    /**
     * 查询所有学年
     */
    List<AcademicYear> findAll();

    /**
     * 查询当前学年
     */
    Optional<AcademicYear> findCurrent();

    /**
     * 删除学年
     */
    void deleteById(Long id);

    /**
     * 清除所有当前学年标记
     */
    void clearAllCurrent();

    /**
     * 检查学年代码是否存在
     */
    boolean existsByYearCode(String yearCode);
}
