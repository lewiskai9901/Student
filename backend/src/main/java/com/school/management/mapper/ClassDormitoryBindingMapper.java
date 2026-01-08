package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.ClassDormitoryBinding;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 班级-宿舍关联Mapper接口
 *
 * @author system
 * @since 2.0.0
 */
@Mapper
public interface ClassDormitoryBindingMapper extends BaseMapper<ClassDormitoryBinding> {

    /**
     * 根据班级ID查询宿舍关联列表
     *
     * @param classId 班级ID
     * @return 关联列表
     */
    List<ClassDormitoryBinding> selectByClassId(@Param("classId") Long classId);

    /**
     * 根据宿舍ID查询班级关联列表
     *
     * @param dormitoryId 宿舍ID
     * @return 关联列表
     */
    List<ClassDormitoryBinding> selectByDormitoryId(@Param("dormitoryId") Long dormitoryId);

    /**
     * 批量插入关联
     *
     * @param bindings 关联列表
     * @return 插入数量
     */
    Integer batchInsert(@Param("bindings") List<ClassDormitoryBinding> bindings);

    /**
     * 根据班级ID删除所有关联
     *
     * @param classId 班级ID
     * @return 删除数量
     */
    Integer deleteByClassId(@Param("classId") Long classId);
}
