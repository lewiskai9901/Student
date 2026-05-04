package com.school.management.infrastructure.extension.plugins.education.interfaces.rest.teaching;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.management.common.result.Result;
import com.school.management.common.util.SecurityUtils;
import com.school.management.infrastructure.casbin.CasbinAccess;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.preference.TeacherPreferenceMapper;
import com.school.management.infrastructure.extension.plugins.education.infrastructure.persistence.teaching.preference.TeacherPreferencePO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 教师排课偏好 REST 端点.
 * 教师设置自己的不可用时间 / 偏好时段 / 偏好教室,
 * 由 AutoSchedulingService 在排课时作为软/硬约束读取.
 */
@RestController
@RequestMapping("/teaching/teacher-preferences")
@RequiredArgsConstructor
public class TeacherPreferenceController {

    private final TeacherPreferenceMapper mapper;

    /**
     * 列出某学期某教师的全部偏好. teacherId 不传则取当前登录用户.
     */
    @GetMapping
    @CasbinAccess(resource = "teaching:schedule", action = "view")
    public Result<List<TeacherPreferencePO>> list(
            @RequestParam Long semesterId,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Integer preferenceType) {
        Long t = teacherId != null ? teacherId : SecurityUtils.getCurrentUserId();
        LambdaQueryWrapper<TeacherPreferencePO> wrapper = new LambdaQueryWrapper<TeacherPreferencePO>()
                .eq(TeacherPreferencePO::getSemesterId, semesterId)
                .eq(TeacherPreferencePO::getTeacherId, t)
                .eq(TeacherPreferencePO::getStatus, 1);
        if (preferenceType != null) wrapper.eq(TeacherPreferencePO::getPreferenceType, preferenceType);
        wrapper.orderByDesc(TeacherPreferencePO::getPriority).orderByAsc(TeacherPreferencePO::getId);
        return Result.success(mapper.selectList(wrapper));
    }

    @PostMapping
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<TeacherPreferencePO> create(@RequestBody TeacherPreferencePO body) {
        if (body.getTeacherId() == null) body.setTeacherId(SecurityUtils.getCurrentUserId());
        if (body.getStatus() == null) body.setStatus(1);
        if (body.getPriority() == null) body.setPriority(0);
        mapper.insert(body);
        return Result.success(body);
    }

    @PutMapping("/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<TeacherPreferencePO> update(@PathVariable Long id, @RequestBody TeacherPreferencePO body) {
        body.setId(id);
        mapper.updateById(body);
        return Result.success(mapper.selectById(id));
    }

    @DeleteMapping("/{id}")
    @CasbinAccess(resource = "teaching:schedule", action = "edit")
    public Result<Void> delete(@PathVariable Long id) {
        // 软删: status=0
        TeacherPreferencePO po = mapper.selectById(id);
        if (po != null) { po.setStatus(0); mapper.updateById(po); }
        return Result.success(null);
    }
}
