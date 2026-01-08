package com.school.management.dto.inspector;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 更新打分人员请求
 */
@Data
public class InspectorUpdateRequest {

    /**
     * 打分人员ID
     */
    @NotNull(message = "打分人员ID不能为空")
    private Long id;

    /**
     * 状态：1启用 0禁用
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

    /**
     * 权限配置列表
     */
    @NotEmpty(message = "权限配置不能为空")
    private List<InspectorCreateRequest.PermissionConfig> permissions;
}
