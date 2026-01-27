package com.school.management.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.management.entity.ClassWeightConfig;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 班级加权配置Mapper接口
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Mapper
public interface ClassWeightConfigMapper extends BaseMapper<ClassWeightConfig> {

    /**
     * 根据配置编码查询配置
     *
     * @param configCode 配置编码
     * @return 配置
     */
    ClassWeightConfig selectByConfigCode(@Param("configCode") String configCode);

    /**
     * 查询有效的加权配置(在有效期内且状态为启用)
     *
     * @param applyScope 应用范围
     * @param applyScopeId 应用范围ID
     * @param currentDate 当前日期
     * @return 配置列表
     */
    List<ClassWeightConfig> selectEffectiveConfigs(
            @Param("applyScope") String applyScope,
            @Param("applyScopeId") Long applyScopeId,
            @Param("currentDate") LocalDate currentDate
    );

    /**
     * 查询班级的加权配置(按优先级)
     *
     * @param classId 班级ID
     * @param gradeId 年级ID
     * @param orgUnitId 组织单元ID
     * @param currentDate 当前日期
     * @return 配置
     */
    ClassWeightConfig selectConfigForClass(
            @Param("classId") Long classId,
            @Param("gradeId") Long gradeId,
            @Param("orgUnitId") Long orgUnitId,
            @Param("currentDate") LocalDate currentDate
    );

    /**
     * 检查配置编码是否存在
     *
     * @param configCode 配置编码
     * @param excludeId 排除的ID
     * @return 存在数量
     */
    int checkConfigCodeExists(
            @Param("configCode") String configCode,
            @Param("excludeId") Long excludeId
    );
}
