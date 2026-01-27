package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统模块MyBatis Mapper
 */
@Mapper
public interface SystemModuleMapper extends BaseMapper<SystemModulePO> {

    /**
     * 根据模块编码查找
     */
    @Select("SELECT * FROM system_modules WHERE module_code = #{moduleCode}")
    SystemModulePO findByModuleCode(@Param("moduleCode") String moduleCode);

    /**
     * 查找所有启用的模块
     */
    @Select("SELECT * FROM system_modules WHERE status = 1 ORDER BY sort_order")
    List<SystemModulePO> findAllEnabled();

    /**
     * 查找顶级模块
     */
    @Select("SELECT * FROM system_modules WHERE (parent_code IS NULL OR parent_code = '') AND status = 1 ORDER BY sort_order")
    List<SystemModulePO> findTopLevelModules();

    /**
     * 根据父模块编码查找子模块
     */
    @Select("SELECT * FROM system_modules WHERE parent_code = #{parentCode} AND status = 1 ORDER BY sort_order")
    List<SystemModulePO> findByParentCode(@Param("parentCode") String parentCode);
}
