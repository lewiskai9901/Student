package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 学年Mapper
 */
@Mapper
public interface AcademicYearMapper extends BaseMapper<AcademicYearPO> {

    /**
     * 清除所有当前学年标记
     */
    @Update("UPDATE academic_years SET is_current = 0 WHERE is_current = 1 AND deleted = 0")
    void clearAllCurrent();
}
