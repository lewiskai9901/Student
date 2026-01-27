package com.school.management.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 楼宇实体
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("buildings")
public class Building extends BaseEntity {

    /**
     * 楼号(纯数字,如: 1, 2, 3)
     */
    private String buildingNo;

    /**
     * 楼宇名称
     */
    private String buildingName;

    /**
     * 楼宇类型: 1-教学楼, 2-宿舍楼, 3-办公楼
     */
    private Integer buildingType;

    /**
     * 总楼层数
     */
    private Integer totalFloors;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 建造年份
     */
    private Integer constructionYear;

    /**
     * 楼宇描述
     */
    private String description;

    /**
     * 状态: 0-停用, 1-启用
     */
    private Integer status;

    /**
     * 房间数量(非数据库字段)
     */
    @TableField(exist = false)
    private Integer roomCount;

    /**
     * 楼宇类型名称(非数据库字段)
     */
    @TableField(exist = false)
    private String buildingTypeName;

    /**
     * 关联组织单元ID列表(非数据库字段)
     */
    @TableField(exist = false)
    private java.util.List<Long> orgUnitIds;

    /**
     * 关联组织单元名称列表(非数据库字段) - SQL返回的是逗号分隔的字符串
     */
    @TableField(exist = false)
    private String orgUnitNames;

    /**
     * 获取楼宇类型名称
     */
    public String getBuildingTypeName() {
        if (buildingTypeName != null) {
            return buildingTypeName;
        }
        if (buildingType == null) {
            return "";
        }
        switch (buildingType) {
            case 1:
                return "教学楼";
            case 2:
                return "宿舍楼";
            case 3:
                return "办公楼";
            default:
                return "未知";
        }
    }
}
