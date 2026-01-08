package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.Classroom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 教室Mapper接口
 *
 * @author system
 * @since 1.0.0
 */
@Mapper
public interface ClassroomMapper extends BaseMapper<Classroom> {

    /**
     * 分页查询教室(包含教学楼名称)
     *
     * @param page 分页参数
     * @param buildingId 教学楼ID
     * @param floor 楼层
     * @param classroomType 教室类型
     * @param status 状态
     * @return 教室分页列表
     */
    IPage<Classroom> selectClassroomPage(Page<Classroom> page,
                                         @Param("buildingId") Long buildingId,
                                         @Param("floor") Integer floor,
                                         @Param("classroomType") String classroomType,
                                         @Param("status") Integer status);
}
