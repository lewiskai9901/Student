package com.school.management.infrastructure.persistence.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 系统配置 Mapper
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigPO> {

    /**
     * 根据配置分组获取配置列表
     */
    @Select("SELECT * FROM system_configs WHERE config_group = #{group} AND status = 1 AND deleted = 0 ORDER BY sort_order")
    List<SystemConfigPO> findByGroup(@Param("group") String group);

    /**
     * 根据配置键获取配置
     */
    @Select("SELECT * FROM system_configs WHERE config_key = #{key} AND deleted = 0")
    SystemConfigPO findByKey(@Param("key") String key);

    /**
     * 获取所有启用的配置
     */
    @Select("SELECT * FROM system_configs WHERE status = 1 AND deleted = 0 ORDER BY config_group, sort_order")
    List<SystemConfigPO> findAllEnabled();
}
