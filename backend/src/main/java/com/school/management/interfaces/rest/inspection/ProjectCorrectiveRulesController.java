package com.school.management.interfaces.rest.inspection;

import com.school.management.application.inspection.ProjectCorrectiveRulesApplicationService;
import com.school.management.application.inspection.ProjectCorrectiveRulesApplicationService.ItemOverrideView;
import com.school.management.application.inspection.ProjectCorrectiveRulesApplicationService.ModeRuleView;
import com.school.management.application.inspection.ProjectCorrectiveRulesApplicationService.ProjectItemView;
import com.school.management.common.result.Result;
import com.school.management.infrastructure.casbin.CasbinAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 项目级整改规则 API (架构 E).
 *
 * <p>双层规则:
 * <ul>
 *   <li>{@code /by-mode} — 按 scoring_mode 批量规则</li>
 *   <li>{@code /item-overrides} — 按 template_item_id 个例覆盖</li>
 * </ul>
 *
 * <p>引擎查询优先级: 个例 &gt; 按题型 &gt; EMPTY 兜底.
 *
 * <p>数据逻辑全部下沉到 {@link ProjectCorrectiveRulesApplicationService}, 本类只做 HTTP 绑定。
 */
@RestController
@RequestMapping("/inspection/corrective/projects/{projectId}")
@RequiredArgsConstructor
public class ProjectCorrectiveRulesController {

    private final ProjectCorrectiveRulesApplicationService service;

    // ============================================================
    // 按题型规则
    // ============================================================

    /** 列出本项目所有按题型规则. */
    @GetMapping("/by-mode")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ModeRuleView>> listByMode(@PathVariable Long projectId) {
        return Result.success(service.listByMode(projectId));
    }

    /** 创建或更新某 scoring_mode 的规则 (upsert). */
    @PutMapping("/by-mode/{mode}")
    @CasbinAccess(resource = "insp:project", action = "update")
    public Result<ModeRuleView> upsertByMode(@PathVariable Long projectId,
                                             @PathVariable String mode,
                                             @RequestBody Map<String, Object> ruleBody) {
        return Result.success(service.upsertByMode(projectId, mode, ruleBody));
    }

    /** 清除某 scoring_mode 的规则 (回到智能默认兜底). */
    @DeleteMapping("/by-mode/{mode}")
    @CasbinAccess(resource = "insp:project", action = "update")
    public Result<Void> deleteByMode(@PathVariable Long projectId, @PathVariable String mode) {
        service.deleteByMode(projectId, mode);
        return Result.success(null);
    }

    // ============================================================
    // 题目个例覆盖
    // ============================================================

    /** 列出本项目所有题目个例规则. */
    @GetMapping("/item-overrides")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ItemOverrideView>> listItemOverrides(@PathVariable Long projectId) {
        return Result.success(service.listItemOverrides(projectId));
    }

    /** 创建或更新某题目的个例规则. */
    @PutMapping("/item-overrides/{itemId}")
    @CasbinAccess(resource = "insp:project", action = "update")
    public Result<ItemOverrideView> upsertItemOverride(@PathVariable Long projectId,
                                                       @PathVariable Long itemId,
                                                       @RequestBody Map<String, Object> ruleBody) {
        return Result.success(service.upsertItemOverride(projectId, itemId, ruleBody));
    }

    /** 清除题目个例规则 (回退到项目按题型规则). */
    @DeleteMapping("/item-overrides/{itemId}")
    @CasbinAccess(resource = "insp:project", action = "update")
    public Result<Void> deleteItemOverride(@PathVariable Long projectId, @PathVariable Long itemId) {
        service.deleteItemOverride(projectId, itemId);
        return Result.success(null);
    }

    // ============================================================
    // 列出项目用到的所有模板检查项 (题目整改设置主交互所需)
    // ============================================================

    /**
     * 列出本项目模板里所有评分题 (含 section 名 + scoring_mode + 整改规则现状).
     * <p>引擎查询是按题目个例 → 按题型 → 兜底, 此 API 主要给前端"逐题开关整改"用.
     */
    @GetMapping("/template-items")
    @CasbinAccess(resource = "insp:project", action = "view")
    public Result<List<ProjectItemView>> listProjectItems(@PathVariable Long projectId) {
        return Result.success(service.listProjectItems(projectId));
    }
}
