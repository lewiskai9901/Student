package com.school.management.application.asset.command;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 创建资产分类命令
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCategoryCommand {

    private Long parentId;

    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    private Integer categoryType;  // 1-固定资产 2-低值易耗品 3-消耗品

    // 默认管理模式: 1-单品管理 2-批量管理
    // 如果不指定，则根据categoryType自动确定
    private Integer defaultManagementMode;

    private Integer depreciationYears;

    private String unit;

    private Integer sortOrder;

    private String remark;
}
