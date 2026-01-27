package com.school.management.infrastructure.persistence.analytics;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("analytics_snapshots")
public class AnalyticsSnapshotPO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String snapshotType;
    private String snapshotScope;
    private Long scopeId;
    private LocalDate snapshotDate;
    private String dataJson;
    private LocalDateTime generatedAt;
}
