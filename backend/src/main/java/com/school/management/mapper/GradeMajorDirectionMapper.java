package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.GradeMajorDirection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 学年专业方向关联Mapper接口
 */
@Mapper
public interface GradeMajorDirectionMapper extends BaseMapper<GradeMajorDirection> {

    /**
     * 分页查询学年专业方向关联列表
     */
    IPage<GradeMajorDirection> selectGradeMajorDirectionPage(Page<GradeMajorDirection> page,
                                                               @Param("params") GradeMajorDirection params);

    /**
     * 根据学年查询专业方向列表
     */
    List<GradeMajorDirection> selectByAcademicYear(@Param("academicYear") Integer academicYear);

    /**
     * 根据专业方向ID查询关联的学年列表
     */
    List<GradeMajorDirection> selectByMajorDirectionId(@Param("majorDirectionId") Long majorDirectionId);

    /**
     * 根据ID查询详情(包含关联信息)
     */
    GradeMajorDirection selectDetailById(@Param("id") Long id);

    /**
     * 根据学年和专业方向ID查询
     */
    GradeMajorDirection selectByYearAndDirection(@Param("academicYear") Integer academicYear,
                                                   @Param("majorDirectionId") Long majorDirectionId);
}
