package com.school.management.application.shared.query;

import lombok.Data;

/**
 * Base class for paginated query requests.
 *
 * <p>Provides standard pagination parameters that can be extended
 * by specific query criteria classes.
 *
 * <p>Example usage:
 * <pre>{@code
 * public class StudentQuery extends PageQuery {
 *     private String keyword;
 *     private Long orgUnitId;
 *     private Integer status;
 * }
 * }</pre>
 */
@Data
public abstract class PageQuery {

    /**
     * Page number (1-based).
     */
    private Integer pageNum = 1;

    /**
     * Page size (items per page).
     */
    private Integer pageSize = 10;

    /**
     * Sort field name.
     */
    private String sortBy;

    /**
     * Sort direction: "asc" or "desc".
     */
    private String sortOrder = "desc";

    /**
     * Gets the offset for SQL LIMIT clause.
     *
     * @return the offset (0-based)
     */
    public int getOffset() {
        return (getPageNum() - 1) * getPageSize();
    }

    /**
     * Gets safe page number (minimum 1).
     *
     * @return page number, at least 1
     */
    public int getPageNum() {
        return pageNum != null && pageNum > 0 ? pageNum : 1;
    }

    /**
     * Gets safe page size (between 1 and 1000).
     *
     * @return page size, between 1 and 1000
     */
    public int getPageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10;
        }
        return Math.min(pageSize, 1000);
    }

    /**
     * Checks if this query requests ascending sort.
     *
     * @return true if ascending, false if descending
     */
    public boolean isAscending() {
        return "asc".equalsIgnoreCase(sortOrder);
    }

    /**
     * Creates a new query with specified page parameters.
     *
     * @param pageNum  page number
     * @param pageSize page size
     * @return this query for chaining
     */
    public PageQuery withPage(int pageNum, int pageSize) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        return this;
    }

    /**
     * Creates a new query with specified sort parameters.
     *
     * @param sortBy    sort field
     * @param ascending true for ascending, false for descending
     * @return this query for chaining
     */
    public PageQuery withSort(String sortBy, boolean ascending) {
        this.sortBy = sortBy;
        this.sortOrder = ascending ? "asc" : "desc";
        return this;
    }
}
