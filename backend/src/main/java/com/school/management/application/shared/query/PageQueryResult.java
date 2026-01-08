package com.school.management.application.shared.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Standard paginated query result.
 *
 * <p>Provides a consistent structure for paginated responses across all query services.
 *
 * @param <T> the type of items in the result
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageQueryResult<T> {

    /**
     * Items in the current page.
     */
    private List<T> items;

    /**
     * Total count of all matching items.
     */
    private long total;

    /**
     * Current page number (1-based).
     */
    private int pageNum;

    /**
     * Items per page.
     */
    private int pageSize;

    /**
     * Total number of pages.
     */
    private int totalPages;

    /**
     * Whether there is a next page.
     */
    private boolean hasNext;

    /**
     * Whether there is a previous page.
     */
    private boolean hasPrevious;

    /**
     * Creates an empty result.
     *
     * @param <T> the item type
     * @return empty page result
     */
    public static <T> PageQueryResult<T> empty() {
        return PageQueryResult.<T>builder()
            .items(Collections.emptyList())
            .total(0)
            .pageNum(1)
            .pageSize(10)
            .totalPages(0)
            .hasNext(false)
            .hasPrevious(false)
            .build();
    }

    /**
     * Creates a page result from the given data.
     *
     * @param items    items in the current page
     * @param total    total count
     * @param pageNum  current page number
     * @param pageSize page size
     * @param <T>      the item type
     * @return page result
     */
    public static <T> PageQueryResult<T> of(List<T> items, long total, int pageNum, int pageSize) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;

        return PageQueryResult.<T>builder()
            .items(items != null ? items : Collections.emptyList())
            .total(total)
            .pageNum(pageNum)
            .pageSize(pageSize)
            .totalPages(totalPages)
            .hasNext(pageNum < totalPages)
            .hasPrevious(pageNum > 1)
            .build();
    }

    /**
     * Creates a page result from a query.
     *
     * @param items items in the current page
     * @param total total count
     * @param query the query that produced this result
     * @param <T>   the item type
     * @return page result
     */
    public static <T> PageQueryResult<T> of(List<T> items, long total, PageQuery query) {
        return of(items, total, query.getPageNum(), query.getPageSize());
    }

    /**
     * Maps items to a different type.
     *
     * @param mapper mapping function
     * @param <R>    target type
     * @return new page result with mapped items
     */
    public <R> PageQueryResult<R> map(Function<T, R> mapper) {
        List<R> mappedItems = items.stream()
            .map(mapper)
            .collect(Collectors.toList());

        return PageQueryResult.<R>builder()
            .items(mappedItems)
            .total(this.total)
            .pageNum(this.pageNum)
            .pageSize(this.pageSize)
            .totalPages(this.totalPages)
            .hasNext(this.hasNext)
            .hasPrevious(this.hasPrevious)
            .build();
    }

    /**
     * Checks if this result is empty.
     *
     * @return true if no items
     */
    public boolean isEmpty() {
        return items == null || items.isEmpty();
    }

    /**
     * Gets the number of items in the current page.
     *
     * @return item count
     */
    public int getSize() {
        return items != null ? items.size() : 0;
    }
}
