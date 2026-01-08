package com.school.management.dto;

import lombok.Data;

import java.util.List;

/**
 * 扣分配置 - 量化1.0版本
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class DeductConfig {

    /**
     * 扣分项列表
     */
    private List<DeductItem> items;
}
