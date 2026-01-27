package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
@Schema(description = "用户查询请求")
public class UserQueryRequest {

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "真实姓名")
    private String realName;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "性别: 1-男 2-女")
    private Integer gender;

    @Schema(description = "用户状态: 1-启用 0-禁用")
    private Integer status;

    @Schema(description = "组织单元ID")
    private Long orgUnitId;

    @Schema(description = "班级ID")
    private Long classId;

    @Schema(description = "学号/工号")
    private String studentId;

    @Schema(description = "用户类型: 1-学生 2-教师 3-管理员")
    private Integer userType;

    @Schema(description = "角色代码")
    private String roleCode;

    @Schema(description = "创建时间开始")
    private String createTimeStart;

    @Schema(description = "创建时间结束")
    private String createTimeEnd;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "页码", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;

    @Schema(description = "排序字段", example = "createTime")
    private String sortBy = "createTime";

    @Schema(description = "排序方向: asc-升序 desc-降序", example = "desc")
    private String sortDir = "desc";
}