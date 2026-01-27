package com.school.management.interfaces.rest.asset.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 批量更新宿舍部门分配请求
 */
@Data
@Schema(description = "批量更新宿舍部门分配请求")
public class BatchUpdateDepartmentRequest {

    @Schema(description = "宿舍ID列表", required = true)
    private List<String> dormitoryIds;

    @Schema(description = "组织单元ID（部门），null表示取消分配")
    private Long orgUnitId;

    /**
     * 获取Long类型的宿舍ID列表
     * 处理前端可能发送字符串类型的大整数ID
     */
    public List<Long> getDormitoryIdsAsLong() {
        if (dormitoryIds == null) {
            return null;
        }
        return dormitoryIds.stream()
                .map(id -> {
                    if (id == null) return null;
                    try {
                        return Long.parseLong(id.toString().trim());
                    } catch (NumberFormatException e) {
                        return null;
                    }
                })
                .filter(id -> id != null)
                .collect(Collectors.toList());
    }
}
