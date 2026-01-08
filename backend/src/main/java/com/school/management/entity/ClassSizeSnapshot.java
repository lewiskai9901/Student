package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 班级人数快照实体类
 * 用于记录历史某一天的班级人数,保证历史数据的公平性
 *
 * @author system
 * @version 3.0.0
 * @since 2024-12-29
 */
@Data
@TableName("class_size_snapshots")
public class ClassSizeSnapshot {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 班级ID
     */
    private Long classId;

    /**
     * 快照日期
     */
    private LocalDate snapshotDate;

    /**
     * 学生总数
     */
    private Integer studentCount;

    /**
     * 在校人数(排除休学/请假)
     */
    private Integer activeCount;

    /**
     * 男生人数
     */
    private Integer maleCount;

    /**
     * 女生人数
     */
    private Integer femaleCount;

    /**
     * 快照来源(AUTO=自动,MANUAL=手动,PUBLISH=发布检查时,MIGRATION=数据迁移)
     */
    private String snapshotSource;

    /**
     * 关联的检查记录ID
     */
    private Long recordId;

    /**
     * 标准人数
     */
    private Integer standardSize;

    /**
     * 是否已使用(0=未使用,1=已使用)
     */
    private Integer isUsed;

    /**
     * 使用次数
     */
    private Integer usageCount;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    // ========== 关联字段(非数据库字段) ==========

    /**
     * 班级名称
     */
    @TableField(exist = false)
    private String className;

    /**
     * 年级名称
     */
    @TableField(exist = false)
    private String gradeName;

    /**
     * 休学人数
     */
    @TableField(exist = false)
    private Integer suspendedCount;

    /**
     * 计算休学人数
     */
    public Integer getSuspendedCount() {
        if (studentCount == null || activeCount == null) {
            return 0;
        }
        return studentCount - activeCount;
    }
}
