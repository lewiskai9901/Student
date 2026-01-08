package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 归档规则配置实体类
 *
 * @author system
 * @since 2.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("archive_rules")
public class ArchiveRule extends BaseEntity {

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 表名
     */
    private String tableName;

    /**
     * 归档类型:1-按时间 2-按学期
     */
    private Integer archiveType;

    /**
     * 归档天数(如180天前的数据)
     */
    private Integer archiveDays;

    /**
     * 归档学期数(如2个学期前的数据)
     */
    private Integer archiveSemesters;

    /**
     * 是否启用:1-是 0-否
     */
    private Integer isEnabled;

    /**
     * 上次执行时间
     */
    private LocalDateTime lastExecutedAt;
}
