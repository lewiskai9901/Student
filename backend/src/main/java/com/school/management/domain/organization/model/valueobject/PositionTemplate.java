package com.school.management.domain.organization.model.valueobject;

/**
 * 岗位模板值对象
 * 定义该组织类型下可以存在哪些岗位（菜单/调色板）
 * 创建组织时用户从中选择要启用的岗位
 */
public class PositionTemplate {

    private String positionName;
    private int sortOrder;

    public PositionTemplate() {}

    public PositionTemplate(String positionName, int sortOrder) {
        this.positionName = positionName;
        this.sortOrder = sortOrder;
    }

    public String getPositionName() { return positionName; }
    public void setPositionName(String positionName) { this.positionName = positionName; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}
