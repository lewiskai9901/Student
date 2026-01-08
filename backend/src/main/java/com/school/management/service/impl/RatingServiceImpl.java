package com.school.management.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.ResultCode;
import com.school.management.entity.CheckRatingResult;
import com.school.management.entity.RatingLevel;
import com.school.management.entity.RatingRule;
import com.school.management.entity.RatingTemplate;
import com.school.management.exception.BusinessException;
import com.school.management.mapper.CheckRatingResultMapper;
import com.school.management.mapper.RatingLevelMapper;
import com.school.management.mapper.RatingRuleMapper;
import com.school.management.mapper.RatingTemplateMapper;
import com.school.management.service.RatingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 评级服务实现类 (V3.0)
 *
 * @author Claude
 * @since 2025-11-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RatingServiceImpl implements RatingService {

    private final RatingTemplateMapper ratingTemplateMapper;
    private final RatingRuleMapper ratingRuleMapper;
    private final RatingLevelMapper ratingLevelMapper;
    private final CheckRatingResultMapper checkRatingResultMapper;
    private final JdbcTemplate jdbcTemplate;

    // ==================== 评级模板管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRatingTemplate(Map<String, Object> request) {
        log.info("创建评级模板: {}", request.get("templateName"));

        RatingTemplate template = new RatingTemplate();
        template.setTemplateName((String) request.get("templateName"));
        template.setTemplateCode((String) request.get("templateCode"));
        template.setDescription((String) request.get("description"));
        template.setCheckTemplateId(((Number) request.get("checkTemplateId")).longValue());
        template.setIsDefault((Integer) request.getOrDefault("isDefault", 0));
        template.setStatus(1);

        ratingTemplateMapper.insert(template);

        log.info("评级模板创建成功, ID: {}", template.getId());

        return template.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRatingTemplate(Long id, Map<String, Object> request) {
        log.info("更新评级模板: id={}", id);

        RatingTemplate template = ratingTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "评级模板不存在");
        }

        if (request.containsKey("templateName")) {
            template.setTemplateName((String) request.get("templateName"));
        }
        if (request.containsKey("description")) {
            template.setDescription((String) request.get("description"));
        }
        if (request.containsKey("status")) {
            template.setStatus((Integer) request.get("status"));
        }

        ratingTemplateMapper.updateById(template);

        log.info("评级模板更新成功, id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRatingTemplate(Long id) {
        log.info("删除评级模板: id={}", id);

        RatingTemplate template = ratingTemplateMapper.selectById(id);
        if (template == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "评级模板不存在");
        }

        // 删除关联的规则和等级
        List<Map<String, Object>> rules = ratingRuleMapper.selectRulesByTemplateId(id);
        for (Map<String, Object> rule : rules) {
            Long ruleId = ((Number) rule.get("id")).longValue();
            ratingLevelMapper.deleteByRuleId(ruleId);
        }
        ratingRuleMapper.deleteByTemplateId(id);

        // 删除模板
        ratingTemplateMapper.deleteById(id);

        log.info("评级模板删除成功, id={}", id);
    }

    @Override
    public Map<String, Object> getRatingTemplateDetail(Long id) {
        log.info("获取评级模板详情: id={}", id);

        return ratingTemplateMapper.selectTemplateDetail(id);
    }

    @Override
    public IPage<Map<String, Object>> pageRatingTemplates(Page<Map<String, Object>> page, Map<String, Object> query) {
        log.info("分页查询评级模板: page={}, query={}", page.getCurrent(), query);

        return ratingTemplateMapper.selectTemplatePage(page, query);
    }

    @Override
    public List<Map<String, Object>> getRatingTemplatesByCheckTemplate(Long checkTemplateId) {
        log.info("根据检查模板ID查询评级模板: checkTemplateId={}", checkTemplateId);

        List<RatingTemplate> templates = ratingTemplateMapper.selectByCheckTemplateId(checkTemplateId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (RatingTemplate template : templates) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", template.getId());
            map.put("templateName", template.getTemplateName());
            map.put("templateCode", template.getTemplateCode());
            map.put("description", template.getDescription());
            map.put("isDefault", template.getIsDefault());
            map.put("status", template.getStatus());
            result.add(map);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultRatingTemplate(Long id) {
        log.info("设置默认评级模板: id={}", id);

        // 先清空所有默认标记
        ratingTemplateMapper.clearAllDefaultFlags();

        // 设置新的默认模板
        ratingTemplateMapper.setDefaultTemplate(id);

        log.info("默认评级模板设置成功, id={}", id);
    }

    // ==================== 评级规则管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createRatingRule(Long templateId, Map<String, Object> request) {
        log.info("创建评级规则: templateId={}, ratingName={}", templateId, request.get("ratingName"));

        RatingRule rule = new RatingRule();
        rule.setRatingTemplateId(templateId);
        rule.setRatingName((String) request.get("ratingName"));
        rule.setRatingCode((String) request.get("ratingCode"));
        rule.setRatingDescription((String) request.get("ratingDescription"));
        rule.setRatingBasis((String) request.get("ratingBasis"));
        rule.setCategoryIds((String) request.get("categoryIds"));
        rule.setScoreType((String) request.get("scoreType"));
        rule.setConditionType((String) request.get("conditionType"));
        rule.setIcon((String) request.get("icon"));
        rule.setColor((String) request.get("color"));
        rule.setRewardPoints((Integer) request.get("rewardPoints"));
        rule.setRewardDescription((String) request.get("rewardDescription"));
        rule.setSortOrder((Integer) request.getOrDefault("sortOrder", 0));
        rule.setIsEnabled(1);

        ratingRuleMapper.insert(rule);

        log.info("评级规则创建成功, ID: {}", rule.getId());

        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateRatingRule(Long ruleId, Map<String, Object> request) {
        log.info("更新评级规则: ruleId={}", ruleId);

        RatingRule rule = ratingRuleMapper.selectById(ruleId);
        if (rule == null) {
            throw new BusinessException(ResultCode.DATA_NOT_FOUND, "评级规则不存在");
        }

        if (request.containsKey("ratingName")) {
            rule.setRatingName((String) request.get("ratingName"));
        }
        if (request.containsKey("ratingDescription")) {
            rule.setRatingDescription((String) request.get("ratingDescription"));
        }
        if (request.containsKey("isEnabled")) {
            rule.setIsEnabled((Integer) request.get("isEnabled"));
        }

        ratingRuleMapper.updateById(rule);

        log.info("评级规则更新成功, ruleId={}", ruleId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRatingRule(Long ruleId) {
        log.info("删除评级规则: ruleId={}", ruleId);

        // 删除关联的等级
        ratingLevelMapper.deleteByRuleId(ruleId);

        // 删除规则
        ratingRuleMapper.deleteById(ruleId);

        log.info("评级规则删除成功, ruleId={}", ruleId);
    }

    @Override
    public Map<String, Object> getRatingRuleDetail(Long ruleId) {
        log.info("获取评级规则详情: ruleId={}", ruleId);

        return ratingRuleMapper.selectRuleDetail(ruleId);
    }

    @Override
    public List<Map<String, Object>> getRatingRulesByTemplateId(Long templateId) {
        log.info("根据模板ID查询规则列表: templateId={}", templateId);

        return ratingRuleMapper.selectRulesByTemplateId(templateId);
    }

    // ==================== 评级等级管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSetRatingLevels(Long ruleId, List<Map<String, Object>> levels) {
        log.info("批量设置评级等级: ruleId={}, count={}", ruleId, levels.size());

        // 先删除原有等级
        ratingLevelMapper.deleteByRuleId(ruleId);

        // 批量插入新等级
        List<RatingLevel> levelList = new ArrayList<>();
        for (Map<String, Object> levelMap : levels) {
            RatingLevel level = new RatingLevel();
            level.setRatingRuleId(ruleId);
            level.setLevelName((String) levelMap.get("levelName"));
            level.setLevelCode((String) levelMap.get("levelCode"));
            level.setLevelOrder((Integer) levelMap.get("levelOrder"));

            if (levelMap.containsKey("minPercent")) {
                level.setMinPercent(new BigDecimal(levelMap.get("minPercent").toString()));
            }
            if (levelMap.containsKey("maxPercent")) {
                level.setMaxPercent(new BigDecimal(levelMap.get("maxPercent").toString()));
            }
            if (levelMap.containsKey("minScore")) {
                level.setMinScore(new BigDecimal(levelMap.get("minScore").toString()));
            }
            if (levelMap.containsKey("maxScore")) {
                level.setMaxScore(new BigDecimal(levelMap.get("maxScore").toString()));
            }

            level.setLevelColor((String) levelMap.get("levelColor"));
            level.setLevelIcon((String) levelMap.get("levelIcon"));
            level.setRewardPoints((Integer) levelMap.get("rewardPoints"));

            levelList.add(level);
        }

        ratingLevelMapper.batchInsert(levelList);

        log.info("批量设置评级等级成功");
    }

    @Override
    public List<Map<String, Object>> getRatingLevelsByRuleId(Long ruleId) {
        log.info("根据规则ID查询等级列表: ruleId={}", ruleId);

        List<RatingLevel> levels = ratingLevelMapper.selectByRatingRuleId(ruleId);

        List<Map<String, Object>> result = new ArrayList<>();
        for (RatingLevel level : levels) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", level.getId());
            map.put("levelName", level.getLevelName());
            map.put("levelCode", level.getLevelCode());
            map.put("levelOrder", level.getLevelOrder());
            map.put("minPercent", level.getMinPercent());
            map.put("maxPercent", level.getMaxPercent());
            map.put("minScore", level.getMinScore());
            map.put("maxScore", level.getMaxScore());
            map.put("levelColor", level.getLevelColor());
            map.put("levelIcon", level.getLevelIcon());
            map.put("rewardPoints", level.getRewardPoints());
            result.add(map);
        }

        return result;
    }

    // ==================== 评级计算 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void calculateRatings(Long recordId) {
        log.info("计算评级结果: recordId={}", recordId);

        try {
            // 调用存储过程: calculate_ratings(recordId)
            jdbcTemplate.update("CALL calculate_ratings(?)", recordId);

            log.info("评级结果计算成功, recordId: {}", recordId);
        } catch (Exception e) {
            log.error("评级结果计算失败: recordId={}, error={}", recordId, e.getMessage(), e);
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "评级结果计算失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getClassRatingResults(Long recordId) {
        log.info("查询班级评级结果: recordId={}", recordId);

        List<CheckRatingResult> results = checkRatingResultMapper.selectByRecordIdWithLevel(recordId);
        List<Map<String, Object>> resultList = new ArrayList<>();

        for (CheckRatingResult result : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", result.getId());
            map.put("recordId", result.getRecordId());
            map.put("classId", result.getClassId());
            map.put("className", result.getClassName());
            map.put("totalScore", result.getTotalScore());
            map.put("ranking", result.getRanking());
            map.put("totalClasses", result.getTotalClasses());
            map.put("levelId", result.getLevelId());
            map.put("levelName", result.getLevelName());
            map.put("percentageRank", result.getPercentageRank());
            map.put("createdAt", result.getCreatedAt());
            resultList.add(map);
        }

        log.info("查询到{}条评级结果", resultList.size());
        return resultList;
    }

    @Override
    public Map<String, Object> getClassRatingResult(Long recordId, Long classId) {
        log.info("查询指定班级的评级结果: recordId={}, classId={}", recordId, classId);

        CheckRatingResult result = checkRatingResultMapper.selectByRecordIdAndClassId(recordId, classId);
        if (result == null) {
            log.warn("未找到班级评级结果: recordId={}, classId={}", recordId, classId);
            return new HashMap<>();
        }

        Map<String, Object> map = new HashMap<>();
        map.put("id", result.getId());
        map.put("recordId", result.getRecordId());
        map.put("classId", result.getClassId());
        map.put("className", result.getClassName());
        map.put("totalScore", result.getTotalScore());
        map.put("ranking", result.getRanking());
        map.put("totalClasses", result.getTotalClasses());
        map.put("levelId", result.getLevelId());
        map.put("levelName", result.getLevelName());
        map.put("percentageRank", result.getPercentageRank());
        map.put("createdAt", result.getCreatedAt());

        return map;
    }

    @Override
    public Map<String, Object> getRatingStatistics(Long recordId) {
        log.info("查询评级统计: recordId={}", recordId);

        Map<String, Object> statistics = new HashMap<>();

        // 获取各等级班级数量
        List<Map<String, Object>> levelCounts = checkRatingResultMapper.countByLevelName(recordId);
        statistics.put("levelDistribution", levelCounts);

        // 获取总班级数
        List<CheckRatingResult> allResults = checkRatingResultMapper.selectByRecordIdWithLevel(recordId);
        statistics.put("totalClasses", allResults.size());

        // 计算各等级占比
        if (!allResults.isEmpty()) {
            List<Map<String, Object>> distribution = new ArrayList<>();
            for (Map<String, Object> levelCount : levelCounts) {
                Map<String, Object> item = new HashMap<>(levelCount);
                Object countObj = levelCount.get("count");
                int count = countObj != null ? ((Number) countObj).intValue() : 0;
                double percentage = (double) count / allResults.size() * 100;
                item.put("percentage", String.format("%.1f", percentage));
                distribution.add(item);
            }
            statistics.put("distribution", distribution);
        }

        log.info("评级统计完成: recordId={}, totalClasses={}", recordId, allResults.size());
        return statistics;
    }
}
