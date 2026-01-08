package com.school.management.casbin.dto;

import com.school.management.casbin.entity.UserScopeAssignment;
import com.school.management.casbin.enums.ScopeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 范围分配DTO
 *
 * @author system
 * @version 1.0.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScopeAssignmentDTO {

    /**
     * 分配记录ID
     */
    private Long id;

    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户真实姓名
     */
    private String realName;

    /**
     * 范围类型
     */
    @NotBlank(message = "范围类型不能为空")
    private String scopeType;

    /**
     * 范围类型名称
     */
    private String scopeTypeName;

    /**
     * 范围表达式
     */
    @NotBlank(message = "范围表达式不能为空")
    private String scopeExpression;

    /**
     * 显示名称
     */
    private String displayName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 过期时间
     */
    private LocalDateTime expiresAt;

    /**
     * 分配人ID
     */
    private Long assignedBy;

    /**
     * 分配人姓名
     */
    private String assignedByName;

    /**
     * 分配时间
     */
    private LocalDateTime assignedAt;

    /**
     * 转换为实体
     */
    public UserScopeAssignment toEntity() {
        return UserScopeAssignment.builder()
                .userId(this.userId)
                .scopeType(this.scopeType)
                .scopeExpression(this.scopeExpression)
                .displayName(this.displayName)
                .remark(this.remark)
                .expiresAt(this.expiresAt)
                .build();
    }

    /**
     * 从实体转换
     */
    public static ScopeAssignmentDTO fromEntity(UserScopeAssignment entity) {
        if (entity == null) {
            return null;
        }
        ScopeType type = ScopeType.fromCode(entity.getScopeType());
        return ScopeAssignmentDTO.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .scopeType(entity.getScopeType())
                .scopeTypeName(type != null ? type.getName() : null)
                .scopeExpression(entity.getScopeExpression())
                .displayName(entity.getDisplayName())
                .remark(entity.getRemark())
                .expiresAt(entity.getExpiresAt())
                .assignedBy(entity.getAssignedBy())
                .assignedAt(entity.getAssignedAt())
                .build();
    }
}
