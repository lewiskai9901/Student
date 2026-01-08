package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.entity.Major;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 专业Mapper接口
 */
@Mapper
public interface MajorMapper extends BaseMapper<Major> {

    /**
     * 分页查询专业列表
     */
    IPage<Major> selectMajorPage(Page<Major> page, @Param("params") Major params);

    /**
     * 根据部门ID查询专业列表
     */
    List<Major> selectByDepartmentId(@Param("departmentId") Long departmentId);

    /**
     * 查询所有启用的专业
     */
    List<Major> selectAllEnabled();

    /**
     * 根据ID查询专业详情
     */
    Major selectMajorById(@Param("id") Long id);
}
