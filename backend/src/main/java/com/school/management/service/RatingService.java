package com.school.management.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;
import java.util.Map;

/**
 * 评级服务接口 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
public interface RatingService {

    // ==================== 评级模板管理 ====================

    /**
     * 创建评级模板
     *
     * @param request 创建请求
     * @return 模板ID
     */
    Long createRatingTemplate(Map<String, Object> request);

    /**
     * 更新评级模板
     *
     * @param id 模板ID
     * @param request 更新请求
     */
    void updateRatingTemplate(Long id, Map<String, Object> request);

    /**
     * 删除评级模板
     *
     * @param id 模板ID
     */
    void deleteRatingTemplate(Long id);

    /**
     * 获取评级模板详情
     *
     * @param id 模板ID
     * @return 模板详情
     */
    Map<String, Object> getRatingTemplateDetail(Long id);

    /**
     * 分页查询评级模板
     *
     * @param page 分页参数
     * @param query 查询条件
     * @return 分页结果
     */
    IPage<Map<String, Object>> pageRatingTemplates(Page<Map<String, Object>> page, Map<String, Object> query);

    /**
     * 根据检查模板ID查询评级模板列表
     *
     * @param checkTemplateId 检查模板ID
     * @return 评级模板列表
     */
    List<Map<String, Object>> getRatingTemplatesByCheckTemplate(Long checkTemplateId);

    /**
     * 设置默认评级模板
     *
     * @param id 模板ID
     */
    void setDefaultRatingTemplate(Long id);

    // ==================== 评级规则管理 ====================

    /**
     * 创建评级规则
     *
     * @param templateId 模板ID
     * @param request 创建请求
     * @return 规则ID
     */
    Long createRatingRule(Long templateId, Map<String, Object> request);

    /**
     * 更新评级规则
     *
     * @param ruleId 规则ID
     * @param request 更新请求
     */
    void updateRatingRule(Long ruleId, Map<String, Object> request);

    /**
     * 删除评级规则
     *
     * @param ruleId 规则ID
     */
    void deleteRatingRule(Long ruleId);

    /**
     * 获取规则详情（含等级列表）
     *
     * @param ruleId 规则ID
     * @return 规则详情
     */
    Map<String, Object> getRatingRuleDetail(Long ruleId);

    /**
     * 根据模板ID查询规则列表
     *
     * @param templateId 模板ID
     * @return 规则列表
     */
    List<Map<String, Object>> getRatingRulesByTemplateId(Long templateId);

    // ==================== 评级等级管理 ====================

    /**
     * 批量设置评级等级
     *
     * @param ruleId 规则ID
     * @param levels 等级列表
     */
    void batchSetRatingLevels(Long ruleId, List<Map<String, Object>> levels);

    /**
     * 根据规则ID查询等级列表
     *
     * @param ruleId 规则ID
     * @return 等级列表
     */
    List<Map<String, Object>> getRatingLevelsByRuleId(Long ruleId);

    // ==================== 评级计算 ====================

    /**
     * 计算评级结果（调用存储过程）
     *
     * @param recordId 检查记录ID
     */
    void calculateRatings(Long recordId);

    /**
     * 查询班级评级结果
     *
     * @param recordId 检查记录ID
     * @return 评级结果列表
     */
    List<Map<String, Object>> getClassRatingResults(Long recordId);

    /**
     * 查询指定班级的评级结果
     *
     * @param recordId 检查记录ID
     * @param classId 班级ID
     * @return 班级评级结果
     */
    Map<String, Object> getClassRatingResult(Long recordId, Long classId);

    /**
     * 查询评级统计（各等级班级数量）
     *
     * @param recordId 检查记录ID
     * @return 评级统计
     */
    Map<String, Object> getRatingStatistics(Long recordId);
}
