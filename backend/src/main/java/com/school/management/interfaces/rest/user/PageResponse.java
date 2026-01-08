package com.school.management.interfaces.rest.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse {

    /**
     * 数据列表
     */
    private List<?> records;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 每页数量
     */
    private int size;

    /**
     * 当前页码
     */
    private int current;

    /**
     * 总页数
     */
    private int pages;

    public PageResponse(List<?> records, long total, int size, int current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
    }
}
