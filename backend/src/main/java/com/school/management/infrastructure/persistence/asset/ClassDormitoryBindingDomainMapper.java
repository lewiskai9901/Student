package com.school.management.infrastructure.persistence.asset;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 班级-宿舍关联 Mapper (DDD infrastructure layer)
 * Replaces V1 ClassDormitoryBindingMapper for use in asset domain services.
 */
@Mapper
public interface ClassDormitoryBindingDomainMapper extends BaseMapper<ClassDormitoryBindingPO> {

    /**
     * 根据宿舍ID查询班级关联列表
     */
    @Select("SELECT id, class_id, dormitory_id, student_count, created_time, updated_time " +
            "FROM class_dormitory_bindings WHERE dormitory_id = #{dormitoryId}")
    List<ClassDormitoryBindingPO> selectByDormitoryId(@Param("dormitoryId") Long dormitoryId);
}
