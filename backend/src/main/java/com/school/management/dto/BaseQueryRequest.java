package com.school.management.dto;

import lombok.Data;

/**
 * 基础查询请求DTO
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class BaseQueryRequest {

    /**
     * 页码（从1开始）
     */
    private Integer pageNum = 1;

    /**
     * 页大小
     */
    private Integer pageSize = 10;

    /**
     * 验证并修正分页参数
     */
    public void validatePagination() {
        if (pageNum == null || pageNum < 1) {
            pageNum = 1;
        }
        if (pageSize == null || pageSize < 1) {
            pageSize = 10;
        }
        if (pageSize > 1000) {
            pageSize = 1000; // 限制最大页大小
        }
    }
}