package com.school.management.application.organization.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 系统模块DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemModuleDTO {

    private Long id;
    private String moduleCode;
    private String moduleName;
    private String moduleDesc;
    private String parentCode;
    private String icon;
    private Integer sortOrder;
    private Boolean isEnabled;
    private List<SystemModuleDTO> children;
}
