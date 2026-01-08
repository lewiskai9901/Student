package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.AppealConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 申诉配置Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface AppealConfigMapper extends BaseMapper<AppealConfig> {

    /**
     * 查询默认申诉配置
     *
     * @return 申诉配置
     */
    AppealConfig selectDefaultConfig();

    /**
     * 根据配置编码查询配置
     *
     * @param configCode 配置编码
     * @return 申诉配置
     */
    AppealConfig selectByConfigCode(@Param("configCode") String configCode);
}
