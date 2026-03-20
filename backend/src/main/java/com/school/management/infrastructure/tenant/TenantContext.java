package com.school.management.infrastructure.tenant;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Tenant context holding current tenant information
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TenantContext {
    private Long tenantId;
    private String tenantCode;
}
