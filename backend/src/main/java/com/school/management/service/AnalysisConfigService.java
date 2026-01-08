package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.school.management.dto.analysis.AnalysisConfigDTO;
import com.school.management.dto.analysis.AnalysisConfigQueryDTO;
import com.school.management.dto.analysis.AnalysisReportDTO;
import com.school.management.entity.AnalysisConfig;

/**
 * 统计分析配置Service接口
 *
 * @author Claude
 * @since 2025-12-05
 */
public interface AnalysisConfigService extends IService<AnalysisConfig> {

    /**
     * 分页查询配置列表
     *
     * @param query 查询条件
     * @return 配置列表
     */
    IPage<AnalysisConfigDTO> getConfigPage(AnalysisConfigQueryDTO query);

    /**
     * 获取配置详情
     *
     * @param id 配置ID
     * @return 配置详情
     */
    AnalysisConfigDTO getConfigById(Long id);

    /**
     * 创建配置
     *
     * @param dto 配置信息
     * @return 创建后的配置ID
     */
    Long createConfig(AnalysisConfigDTO dto);

    /**
     * 更新配置
     *
     * @param dto 配置信息
     * @return 是否成功
     */
    boolean updateConfig(AnalysisConfigDTO dto);

    /**
     * 删除配置
     *
     * @param id 配置ID
     * @return 是否成功
     */
    boolean deleteConfig(Long id);

    /**
     * 生成分析报告
     *
     * @param configId 配置ID
     * @return 分析报告
     */
    AnalysisReportDTO generateReport(Long configId);

    /**
     * 导出报告为Excel
     *
     * @param configId 配置ID
     * @return Excel文件字节数组
     */
    byte[] exportToExcel(Long configId);

    /**
     * 导出报告为PDF
     *
     * @param configId 配置ID
     * @return PDF文件字节数组
     */
    byte[] exportToPdf(Long configId);

    /**
     * 根据检查计划ID获取统计配置列表
     *
     * @param planId 检查计划ID
     * @return 配置列表
     */
    java.util.List<AnalysisConfigDTO> getConfigsByPlanId(Long planId);

    /**
     * 复制配置到指定检查计划
     *
     * @param sourceConfigId 源配置ID
     * @param targetPlanId 目标计划ID
     * @return 新配置ID
     */
    Long copyConfigToPlan(Long sourceConfigId, Long targetPlanId);
}
