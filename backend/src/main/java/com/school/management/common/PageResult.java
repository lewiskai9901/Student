package com.school.management.common;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 分页结果
 *
 * @author system
 * @since 1.0.0
 */
@Data
public class PageResult<T> {

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 总记录数
     */
    private Long total;

    /**
     * 每页大小
     */
    private Long size;

    /**
     * 当前页
     */
    private Long current;

    /**
     * 总页数
     */
    private Long pages;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Long size, Long current) {
        this.records = records;
        this.total = total;
        this.size = size;
        this.current = current;
        this.pages = (total + size - 1) / size;
    }

    /**
     * 从MyBatis Plus的IPage转换
     */
    public static <T> PageResult<T> from(IPage<T> page) {
        return new PageResult<>(
            page.getRecords(),
            page.getTotal(),
            page.getSize(),
            page.getCurrent()
        );
    }

    /**
     * 从MyBatis Plus的IPage转换，并转换数据类型
     */
    public static <T, R> PageResult<R> from(IPage<T> page, List<R> records) {
        return new PageResult<>(
            records,
            page.getTotal(),
            page.getSize(),
            page.getCurrent()
        );
    }

    /**
     * 静态工厂方法创建分页结果
     */
    public static <T> PageResult<T> of(List<T> records, long total, int pageNum, int pageSize) {
        return new PageResult<>(records, total, (long) pageSize, (long) pageNum);
    }
}