package com.school.management.infrastructure.persistence.query;

import com.school.management.application.shared.query.PageQuery;

import java.util.List;

/**
 * Base interface for CQRS query mappers.
 *
 * <p>Query mappers are optimized for read operations and return DTOs directly
 * instead of domain entities. They can:
 * <ul>
 *   <li>Use denormalized views or materialized views</li>
 *   <li>Join multiple tables in a single query</li>
 *   <li>Return aggregated data</li>
 *   <li>Use read replicas</li>
 * </ul>
 *
 * <p>Query mappers should NOT be used for write operations.
 * Write operations should go through the Repository pattern.
 *
 * @param <D> the type of DTO returned
 * @param <Q> the type of query criteria
 */
public interface QueryMapper<D, Q extends PageQuery> {

    /**
     * Finds a single record by ID.
     *
     * @param id the record ID
     * @return the DTO, or null if not found
     */
    D selectById(Long id);

    /**
     * Finds records matching the query criteria with pagination.
     *
     * @param query the query criteria including pagination
     * @return list of matching DTOs
     */
    List<D> selectList(Q query);

    /**
     * Counts records matching the query criteria.
     *
     * @param query the query criteria
     * @return count of matching records
     */
    long selectCount(Q query);

    /**
     * Finds all records (use with caution).
     *
     * @return list of all DTOs
     */
    default List<D> selectAll() {
        throw new UnsupportedOperationException("selectAll not implemented");
    }
}
