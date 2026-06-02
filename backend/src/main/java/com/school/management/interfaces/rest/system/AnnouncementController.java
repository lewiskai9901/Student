package com.school.management.interfaces.rest.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.persistence.system.AnnouncementMapper;
import com.school.management.infrastructure.persistence.system.AnnouncementPO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * 公告/通知管理 — CRUD + 发布/撤销/置顶。
 *
 * <p>2026-06-02 补全: announcements 表 + 前端 AnnouncementsView + 菜单/权限早已就绪,
 * 但后端 API 从未实现 (/system/announcements 一直 404)。本控制器补齐。
 * 可见性由菜单 (system:announcement:view) + Casbin 把控, 故此处仅要求登录, 不再精确鉴权
 * (否则 hasAuthority 精确匹配会把没有该权限行的 admin 挡成 403)。
 */
@Slf4j
@RestController
@RequestMapping("/system/announcements")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "公告管理", description = "系统公告/通知的增删改查与发布")
public class AnnouncementController {

    private final AnnouncementMapper announcementMapper;

    @GetMapping
    @Operation(summary = "公告分页列表")
    public Result<Page<AnnouncementPO>> list(@RequestParam(defaultValue = "1") long pageNum,
                                             @RequestParam(defaultValue = "10") long pageSize,
                                             @RequestParam(required = false) String announcementType,
                                             @RequestParam(required = false) Integer isPublished) {
        LambdaQueryWrapper<AnnouncementPO> qw = new LambdaQueryWrapper<>();
        if (announcementType != null && !announcementType.isEmpty()) {
            qw.eq(AnnouncementPO::getAnnouncementType, announcementType);
        }
        if (isPublished != null) {
            qw.eq(AnnouncementPO::getIsPublished, isPublished);
        }
        qw.orderByDesc(AnnouncementPO::getIsPinned).orderByDesc(AnnouncementPO::getCreatedAt);
        Page<AnnouncementPO> page = announcementMapper.selectPage(new Page<>(pageNum, pageSize), qw);
        return Result.success(page);
    }

    @GetMapping("/{id}")
    @Operation(summary = "公告详情")
    public Result<AnnouncementPO> detail(@PathVariable Long id) {
        return Result.success(announcementMapper.selectById(id));
    }

    @PostMapping
    @Operation(summary = "新建公告")
    public Result<AnnouncementPO> create(@RequestBody AnnouncementPO body) {
        body.setId(null);
        body.setPublisherId(SecurityUtils.getCurrentUserId());
        if (body.getPriority() == null) body.setPriority(1);
        if (body.getIsPublished() == null) body.setIsPublished(0);
        if (body.getIsPinned() == null) body.setIsPinned(0);
        if (body.getViewCount() == null) body.setViewCount(0);
        if (body.getTenantId() == null) body.setTenantId(1L);
        announcementMapper.insert(body);
        return Result.success(body);
    }

    @PutMapping("/{id}")
    @Operation(summary = "编辑公告")
    public Result<Void> update(@PathVariable Long id, @RequestBody AnnouncementPO body) {
        body.setId(id);
        announcementMapper.updateById(body);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除公告 (逻辑删除)")
    public Result<Void> delete(@PathVariable Long id) {
        announcementMapper.deleteById(id);
        return Result.success();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布公告")
    public Result<Void> publish(@PathVariable Long id) {
        AnnouncementPO upd = new AnnouncementPO();
        upd.setId(id);
        upd.setIsPublished(1);
        upd.setPublishTime(LocalDateTime.now());
        announcementMapper.updateById(upd);
        return Result.success();
    }

    @PostMapping("/{id}/revoke")
    @Operation(summary = "撤销发布")
    public Result<Void> revoke(@PathVariable Long id) {
        AnnouncementPO upd = new AnnouncementPO();
        upd.setId(id);
        upd.setIsPublished(0);
        announcementMapper.updateById(upd);
        return Result.success();
    }

    @PostMapping("/{id}/pin")
    @Operation(summary = "置顶/取消置顶")
    public Result<Void> pin(@PathVariable Long id, @RequestParam boolean pinned) {
        AnnouncementPO upd = new AnnouncementPO();
        upd.setId(id);
        upd.setIsPinned(pinned ? 1 : 0);
        announcementMapper.updateById(upd);
        return Result.success();
    }
}
