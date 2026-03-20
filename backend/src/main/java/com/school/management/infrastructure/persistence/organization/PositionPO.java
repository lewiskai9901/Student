package com.school.management.infrastructure.persistence.organization;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("positions")
public class PositionPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    @TableField("position_code")
    private String positionCode;
    @TableField("position_name")
    private String positionName;
    @TableField("org_unit_id")
    private Long orgUnitId;
    @TableField("job_level")
    private String jobLevel;
    @TableField("headcount")
    private Integer headcount;
    @TableField("reports_to_id")
    private Long reportsToId;
    @TableField("responsibilities")
    private String responsibilities;
    @TableField("requirements")
    private String requirements;
    @TableField("sort_order")
    private Integer sortOrder;
    @TableField("is_key_position")
    private Boolean keyPosition;
    @TableField("enabled")
    private Boolean enabled;
    @TableField("tenant_id")
    private Long tenantId;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("created_by")
    private Long createdBy;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    @TableField("updated_by")
    private Long updatedBy;
    @TableLogic
    @TableField("deleted")
    private Integer deleted;
    @Version
    @TableField("version")
    private Integer version;
}
