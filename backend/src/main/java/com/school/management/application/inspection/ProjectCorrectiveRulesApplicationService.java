package com.school.management.application.inspection;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.infrastructure.persistence.inspection.correction.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 项目级整改规则应用服务 (架构 E) — 承载 {@link com.school.management.interfaces.rest.inspection.ProjectCorrectiveRulesController}
 * 的全部数据逻辑 (MyBatis mapper + raw jdbc + JSON 解析), controller 只留 HTTP 绑定。
 *
 * <p>双层规则: 按 scoring_mode 批量规则 + 按 template_item_id 个例覆盖; 引擎查询优先级
 * 个例 &gt; 按题型 &gt; EMPTY 兜底。
 */
@Service
@RequiredArgsConstructor
public class ProjectCorrectiveRulesApplicationService {

    private final ProjectCorrectiveRuleMapper modeRuleMapper;
    private final ProjectItemOverrideMapper itemOverrideMapper;
    private final ObjectMapper objectMapper;
    private final JdbcTemplate jdbcTemplate;

    // ============================================================
    // 按题型规则
    // ============================================================

    /** 列出本项目所有按题型规则. */
    public List<ModeRuleView> listByMode(Long projectId) {
        QueryWrapper<ProjectCorrectiveRulePO> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId).eq("deleted", 0);
        return modeRuleMapper.selectList(qw).stream().map(this::toModeView).collect(Collectors.toList());
    }

    /** 创建或更新某 scoring_mode 的规则 (upsert). */
    @Transactional
    public ModeRuleView upsertByMode(Long projectId, String mode, Map<String, Object> ruleBody) {
        if (mode == null || mode.isBlank()) throw new IllegalArgumentException("mode 必填");
        if (ruleBody == null) throw new IllegalArgumentException("规则 JSON 必填");

        String json = serialize(ruleBody);

        QueryWrapper<ProjectCorrectiveRulePO> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId).eq("scoring_mode", mode.toUpperCase()).eq("deleted", 0);
        ProjectCorrectiveRulePO existing = modeRuleMapper.selectOne(qw);

        if (existing == null) {
            ProjectCorrectiveRulePO po = new ProjectCorrectiveRulePO();
            po.setProjectId(projectId);
            po.setScoringMode(mode.toUpperCase());
            po.setRuleJson(json);
            po.setTenantId(1L);
            modeRuleMapper.insert(po);
            existing = po;
        } else {
            existing.setRuleJson(json);
            modeRuleMapper.updateById(existing);
        }
        return toModeView(existing);
    }

    /** 清除某 scoring_mode 的规则 (回到智能默认兜底). 用 raw DELETE 绕过全局软删. */
    @Transactional
    public void deleteByMode(Long projectId, String mode) {
        jdbcTemplate.update(
            "DELETE FROM insp_project_corrective_rules WHERE project_id=? AND scoring_mode=?",
            projectId, mode.toUpperCase());
    }

    // ============================================================
    // 题目个例覆盖
    // ============================================================

    /** 列出本项目所有题目个例规则. */
    public List<ItemOverrideView> listItemOverrides(Long projectId) {
        QueryWrapper<ProjectItemOverridePO> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId).eq("deleted", 0);
        return itemOverrideMapper.selectList(qw).stream().map(this::toItemView).collect(Collectors.toList());
    }

    /** 创建或更新某题目的个例规则. */
    @Transactional
    public ItemOverrideView upsertItemOverride(Long projectId, Long itemId, Map<String, Object> ruleBody) {
        if (ruleBody == null) throw new IllegalArgumentException("规则 JSON 必填");

        String json = serialize(ruleBody);

        QueryWrapper<ProjectItemOverridePO> qw = new QueryWrapper<>();
        qw.eq("project_id", projectId).eq("template_item_id", itemId).eq("deleted", 0);
        ProjectItemOverridePO existing = itemOverrideMapper.selectOne(qw);

        if (existing == null) {
            ProjectItemOverridePO po = new ProjectItemOverridePO();
            po.setProjectId(projectId);
            po.setTemplateItemId(itemId);
            po.setRuleJson(json);
            po.setTenantId(1L);
            itemOverrideMapper.insert(po);
            existing = po;
        } else {
            existing.setRuleJson(json);
            itemOverrideMapper.updateById(existing);
        }
        return toItemView(existing);
    }

    /** 清除题目个例规则 (回退到项目按题型规则). 用 raw DELETE 绕过全局软删. */
    @Transactional
    public void deleteItemOverride(Long projectId, Long itemId) {
        jdbcTemplate.update(
            "DELETE FROM insp_project_item_overrides WHERE project_id=? AND template_item_id=?",
            projectId, itemId);
    }

    // ============================================================
    // 列出项目用到的所有模板检查项 (题目整改设置主交互所需)
    // ============================================================

    /**
     * 列出本项目模板里所有评分题 (含 section 名 + scoring_mode + 整改规则现状).
     * <p>引擎查询是按题目个例 → 按题型 → 兜底, 此 API 主要给前端"逐题开关整改"用.
     */
    public List<ProjectItemView> listProjectItems(Long projectId) {
        // 项目的 root section id = projects.template_id (其实是 rootSectionId)
        Long rootSectionId;
        try {
            rootSectionId = jdbcTemplate.queryForObject(
                "SELECT template_id FROM insp_projects WHERE id = ? AND deleted = 0",
                Long.class, projectId);
        } catch (Exception e) {
            throw new IllegalArgumentException("项目不存在或未关联模板");
        }
        if (rootSectionId == null) return List.of();

        // 递归取 section 树下所有评分项 + 同时 JOIN sections 取分区名 + 取 item_overrides
        String sql =
            "WITH RECURSIVE section_tree AS (" +
            "  SELECT id, section_name FROM insp_template_sections WHERE id = ? AND deleted = 0 " +
            "  UNION ALL " +
            "  SELECT s.id, s.section_name FROM insp_template_sections s " +
            "  JOIN section_tree st ON s.parent_section_id = st.id WHERE s.deleted = 0 " +
            ") " +
            "SELECT i.id AS item_id, i.item_name, i.scoring_config, st.section_name, " +
            "       o.rule_json AS override_rule_json " +
            "  FROM insp_template_items i " +
            "  JOIN section_tree st ON i.section_id = st.id " +
            "  LEFT JOIN insp_project_item_overrides o " +
            "    ON o.project_id = ? AND o.template_item_id = i.id AND o.deleted = 0 " +
            " WHERE i.deleted = 0 AND i.is_scored = 1 " +
            " ORDER BY i.section_id, i.sort_order, i.id";

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, rootSectionId, projectId);

        List<ProjectItemView> out = new ArrayList<>(rows.size());
        for (Map<String, Object> r : rows) {
            ProjectItemView v = new ProjectItemView();
            v.itemId = ((Number) r.get("item_id")).longValue();
            v.itemName = (String) r.get("item_name");
            v.sectionName = (String) r.get("section_name");
            v.overrideRuleJson = (String) r.get("override_rule_json");

            // scoring_config 是 JSON, 解析出 mode + maxScore 等
            String cfg = (String) r.get("scoring_config");
            v.scoringMode = parseField(cfg, "mode");
            v.maxScore = parseNumField(cfg, "maxScore", "maxStars", "maxBonus", "maxDeduction", "maxCount");
            out.add(v);
        }
        return out;
    }

    // ============================================================
    // 内部辅助
    // ============================================================

    private String serialize(Map<String, Object> ruleBody) {
        try {
            return objectMapper.writeValueAsString(ruleBody);
        } catch (Exception e) {
            throw new IllegalArgumentException("规则 JSON 序列化失败: " + e.getMessage());
        }
    }

    private String parseField(String json, String key) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json).path(key).asText(null);
        } catch (Exception e) { return null; }
    }

    private Double parseNumField(String json, String... keys) {
        if (json == null || json.isBlank()) return null;
        try {
            var node = objectMapper.readTree(json);
            for (String k : keys) {
                if (node.has(k) && node.get(k).isNumber()) {
                    return Math.abs(node.get(k).doubleValue());
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private ModeRuleView toModeView(ProjectCorrectiveRulePO po) {
        ModeRuleView v = new ModeRuleView();
        v.id = po.getId();
        v.scoringMode = po.getScoringMode();
        v.ruleJson = po.getRuleJson();
        return v;
    }

    private ItemOverrideView toItemView(ProjectItemOverridePO po) {
        ItemOverrideView v = new ItemOverrideView();
        v.id = po.getId();
        v.templateItemId = po.getTemplateItemId();
        v.ruleJson = po.getRuleJson();
        return v;
    }

    // ============================================================
    // View 模型 (HTTP 响应 DTO)
    // ============================================================

    public static class ModeRuleView {
        public Long id;
        public String scoringMode;
        public String ruleJson;
    }

    public static class ItemOverrideView {
        public Long id;
        public Long templateItemId;
        public String ruleJson;
    }

    public static class ProjectItemView {
        public Long itemId;
        public String itemName;
        public String sectionName;
        public String scoringMode;
        public Double maxScore;
        public String overrideRuleJson;
    }
}
