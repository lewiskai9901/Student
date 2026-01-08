package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

/**
 * 检查记录加权配置实体类 (支持3级继承+覆盖)
 * 对应表: check_record_weight_configs
 * 
 * 配置层级:
 * - RECORD: 检查记录级别 (全局默认)
 * - CATEGORY: 类别级别
 * - ITEM: 检查项级别
 * 
 * 继承逻辑:
 * - is_inherited=1: 继承父级配置
 * - is_inherited=0: 覆盖父级配置
 *
 * @author Claude
 * @since 2025-11-23
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "check_record_weight_configs", autoResultMap = true)
public class CheckRecordWeightConfig extends BaseEntity {

    /**
     * 检查记录ID
     */
    private Long recordId;

    /**
     * 配置层级: RECORD, CATEGORY, ITEM
     */
    private String configLevel;

    /**
     * 父级配置ID (用于继承链)
     */
    private Long parentId;

    /**
     * 类别ID (当config_level=CATEGORY或ITEM时)
     */
    private Long categoryId;

    /**
     * 检查项ID (当config_level=ITEM时)
     */
    private Long itemId;

    /**
     * 目标名称 (快照)
     */
    private String targetName;

    /**
     * 加权配置ID
     */
    private Long weightConfigId;

    /**
     * 是否继承: 1=继承, 0=覆盖
     */
    private Integer isInherited;

    /**
     * 是否启用加权: 1=启用, 0=禁用
     */
    private Integer weightEnabled;

    /**
     * 加权配置快照 (JSON)
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> weightConfigSnapshot;
}
