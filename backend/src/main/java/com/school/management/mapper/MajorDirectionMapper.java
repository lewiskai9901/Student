package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.MajorDirection;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专业方向Mapper接口
 */
@Mapper
public interface MajorDirectionMapper extends BaseMapper<MajorDirection> {

    /**
     * 分页查询专业方向列表
     */
    IPage<MajorDirection> selectDirectionPage(Page<MajorDirection> page, @Param("params") MajorDirection params);

    /**
     * 根据专业ID查询专业方向列表
     */
    List<MajorDirection> selectByMajorId(@Param("majorId") Long majorId);

    /**
     * 查询所有专业方向
     */
    List<MajorDirection> selectAllDirections();

    /**
     * 根据ID查询专业方向详情(包含专业名称)
     */
    MajorDirection selectDirectionById(@Param("id") Long id);

    /**
     * 根据专业ID删除所有方向
     */
    int deleteByMajorId(@Param("majorId") Long majorId);
}
