package com.school.management.application.user;

import com.school.management.domain.user.model.entity.UserType;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Phase 2.4 — UserType features 中的能力标志 (canXxx) 到 SELF 权限编码的映射服务。
 *
 * 为什么需要显式注册表：
 *   features 中的键混合了两类语义，无法通过命名约定区分：
 *     - 行为标志：canLogin / requiresOrg / canTeach / canManageAll / canManageStudents / requiresClass
 *       （用于聚合校验、登录判定等，非权限）
 *     - 能力标志：canViewOwnSchedule / canViewOwnStudents / canRecordAttendance / canBeSubstitute / canSubmitGrades
 *       （应当映射到 my:* SELF 权限，供 Casbin/Data Permission 鉴权使用）
 *
 * 因此必须有一张白名单 Map 决定哪些 canXxx 才真正产生权限。
 *
 * TODO(Phase 3+)：若此列表扩展到 20+ 条，可迁至 DB 表 capability_registry
 * (capability_code, permission_code, description) 并由运营侧维护。当前数量少，
 * 硬编码更直观、可审计。
 */
@Service
public class CapabilityPermissionService {

    private static final Map<String, String> CAPABILITY_TO_PERMISSION;

    static {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("canViewOwnSchedule",   "my:schedule:view");
        map.put("canViewOwnStudents",   "my:students:view");
        map.put("canRecordAttendance",  "my:attendance:record");
        map.put("canBeSubstitute",      "my:substitute:view");
        map.put("canSubmitGrades",      "my:grades:submit");
        CAPABILITY_TO_PERMISSION = Collections.unmodifiableMap(map);
    }

    /**
     * 读取 UserType.features 中值为 true 且在白名单内的 canXxx，返回对应的 SELF 权限编码列表。
     * features 为 null 或空 → 返回空列表。
     */
    public List<String> getCapabilityPermissionCodes(UserType type) {
        if (type == null) {
            return Collections.emptyList();
        }
        Map<String, Boolean> features = type.getFeatures();
        if (features == null || features.isEmpty()) {
            return Collections.emptyList();
        }
        return features.entrySet().stream()
                .filter(e -> Boolean.TRUE.equals(e.getValue()))
                .map(e -> CAPABILITY_TO_PERMISSION.get(e.getKey()))
                .filter(code -> code != null)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 暴露白名单的只读视图，方便管理端 UI 列出"可选能力"。
     */
    public Map<String, String> getCapabilityRegistry() {
        return CAPABILITY_TO_PERMISSION;
    }
}
