package com.school.management.application.user;

import com.school.management.application.access.AccessApplicationService;
import com.school.management.domain.access.repository.RoleRepository;
import com.school.management.domain.access.repository.UserRoleRepository;
import com.school.management.domain.user.event.UserCreatedEvent;
import com.school.management.domain.user.model.entity.UserType;
import com.school.management.domain.user.repository.UserTypeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

/**
 * Phase 2.1 — UserType 默认角色自动分配。
 *
 * 监听 UserCreatedEvent (AFTER_COMMIT)，按 UserType.defaultRoleCodes 为新用户分配默认角色。
 *
 * 幂等保证：如果用户在 createUser 主事务中已显式分配了角色（user_roles 非空），则跳过，
 * 避免与 UserApplicationService 内联默认角色逻辑重复分配。这样设计的好处：
 *   - createUser API 路径：内联逻辑走 user.assignRoles，listener 看到已有角色 → 跳过
 *   - 其他入口（WeChat 注册 / 批量导入 / API 直接 insert User PO）：listener 兜底分配默认角色
 *
 * 使用 AFTER_COMMIT 而非 BEFORE_COMMIT，避免事务嵌套触发循环 UserCreatedEvent。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserTypeDefaultProvisioner {

    private final UserTypeRepository userTypeRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final AccessApplicationService accessApplicationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserCreated(UserCreatedEvent event) {
        Long userId = parseUserId(event.getUserId());
        if (userId == null) {
            log.warn("[UserType默认角色] 跳过 - userId 不是有效数字: {}", event.getUserId());
            return;
        }

        String typeCode = event.getUserTypeCode();
        if (typeCode == null || typeCode.isBlank()) {
            return;
        }

        // 幂等：用户已有任意角色 → 跳过（createUser 主事务已分配）
        if (!userRoleRepository.findByUserId(userId).isEmpty()) {
            log.debug("[UserType默认角色] 用户 {} 已有角色分配，跳过默认角色", userId);
            return;
        }

        UserType type = userTypeRepository.findByTypeCode(typeCode).orElse(null);
        if (type == null) {
            log.warn("[UserType默认角色] 用户类型不存在: {}", typeCode);
            return;
        }

        List<String> codes = type.getDefaultRoleCodeList();
        if (codes.isEmpty()) {
            return;
        }

        for (String code : codes) {
            assignRoleByCode(userId, code);
        }
    }

    private void assignRoleByCode(Long userId, String code) {
        if (code == null || code.trim().isEmpty()) {
            return;
        }
        String trimmed = code.trim();
        roleRepository.findByRoleCode(trimmed).ifPresentOrElse(
                role -> {
                    try {
                        accessApplicationService.assignRoleToUser(userId, role.getId(), null);
                        log.info("[UserType默认角色] 已为用户 {} 分配 {} ({})",
                                userId, role.getRoleName(), trimmed);
                    } catch (IllegalArgumentException e) {
                        // 如已存在同 scope 角色，AccessApplicationService 会抛出，幂等忽略
                        log.debug("[UserType默认角色] 跳过已存在角色 {}: {}", trimmed, e.getMessage());
                    }
                },
                () -> log.warn("[UserType默认角色] 角色编码不存在: {}", trimmed)
        );
    }

    private Long parseUserId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(raw.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
