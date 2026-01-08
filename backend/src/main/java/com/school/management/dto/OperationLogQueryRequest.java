package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 操作日志查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "操作日志查询请求")
public class OperationLogQueryRequest extends BaseQueryRequest {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "操作模块")
    private String operationModule;

    @Schema(description = "操作类型")
    private String operationType;

    @Schema(description = "操作名称关键字")
    private String operationName;

    @Schema(description = "IP地址")
    private String ipAddress;

    @Schema(description = "响应状态码")
    private Integer responseStatus;

    @Schema(description = "创建时间开始")
    private String createdAtStart;

    @Schema(description = "创建时间结束")
    private String createdAtEnd;

    @Schema(description = "排序字段", example = "created_at")
    private String sortBy = "created_at";

    @Schema(description = "排序方向: asc-升序 desc-降序", example = "desc")
    private String sortDir = "desc";
}
