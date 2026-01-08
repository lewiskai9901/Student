package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.ClassDormitory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 班级宿舍关联Mapper
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface ClassDormitoryMapper extends BaseMapper<ClassDormitory> {
}
