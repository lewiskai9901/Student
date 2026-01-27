package com.school.management.infrastructure.persistence.teaching;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

/**
 * 学期Mapper
 */
@Mapper
public interface SemesterMapper extends BaseMapper<SemesterPO> {

    /**
     * 清除所有当前学期标记
     */
    @Update("UPDATE semesters SET is_current = 0 WHERE is_current = 1 AND deleted = 0")
    void clearAllCurrent();
}
