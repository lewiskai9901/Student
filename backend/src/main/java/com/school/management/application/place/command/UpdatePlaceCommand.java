package com.school.management.application.place.command;

import lombok.Data;

import java.util.Map;

/**
 * 更新场所命令
 */
@Data
public class UpdatePlaceCommand {

    private Long id;
    private String placeName;

    // 楼号和房间号
    private String buildingNo;       // 楼号（如 1, A, 甲）- 仅BUILDING类型
    private String roomNo;           // 房间号（如 101, 302）- 仅ROOM类型

    private Integer capacity;
    private Long orgUnitId;

    /**
     * 是否清除组织单元覆盖（V23: NULL继承模型）
     * true: 将 orgUnitId 设为 NULL，恢复从父级继承
     * false: 保持当前值或设置新值
     */
    private Boolean clearOrgOverride = false;

    private Long responsibleUserId;

    /**
     * 状态（V20: 审计系统）
     * - 1: NORMAL (正常)
     * - 2: DISABLED (停用)
     * - 3: MAINTENANCE (维护中)
     */
    private Integer status;

    private String description;
    private Map<String, Object> attributes;

    /**
     * 更新原因（用于审计日志）
     */
    private String reason;

    // 宿舍扩展
    private Integer genderType;
    private Integer bedCount;
    private String facilities;
    private String assignedClassIds;
    private Long supervisorId;

    // 教室扩展
    private String classroomCategory;
    private Long assignedClassId;
    private Boolean hasProjector;
    private Boolean hasAirConditioner;
    private Boolean hasComputer;
    private String equipmentInfo;

    // 实验室扩展
    private String labCategory;
    private Integer safetyLevel;
    private Long majorId;
    private String equipmentList;
    private String safetyNotice;

    // 办公室扩展
    private String officeType;
    private Long departmentId;
    private Integer workstationCount;
    private String phoneNumber;

    // ========== 验证方法 ==========

    /**
     * 验证命令有效性
     */
    public void validate() {
        if (id == null) {
            throw new IllegalArgumentException("场所ID不能为空");
        }
        if (clearOrgOverride != null && clearOrgOverride && orgUnitId != null) {
            throw new IllegalArgumentException("清除组织覆盖时，组织单元ID必须为NULL");
        }
        if (status != null && !isValidStatus(status)) {
            throw new IllegalArgumentException("非法的状态值: " + status);
        }
    }

    /**
     * 检查状态值是否合法
     */
    private boolean isValidStatus(Integer status) {
        return status == 1 || status == 2 || status == 3;
    }

    /**
     * 是否需要清除组织单元
     */
    public boolean shouldClearOrgUnit() {
        return clearOrgOverride != null && clearOrgOverride;
    }
}
