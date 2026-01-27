package com.school.management.interfaces.rest.organization;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统模块响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemModuleResponse {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String moduleCode;
    private String moduleName;
    private String moduleDesc;
    private String parentCode;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
    private List<SystemModuleResponse> children;
}
