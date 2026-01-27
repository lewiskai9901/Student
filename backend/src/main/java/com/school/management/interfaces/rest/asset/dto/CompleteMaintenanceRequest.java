package com.school.management.interfaces.rest.asset.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 完成维修请求
 */
@Data
public class CompleteMaintenanceRequest {

    private String result;

    private BigDecimal cost;

    private String maintainer;
}
