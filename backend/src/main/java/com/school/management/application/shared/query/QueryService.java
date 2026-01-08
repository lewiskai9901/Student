package com.school.management.application.shared.query;

import org.springframework.transaction.annotation.Transactional;

/**
 * Base interface for CQRS query services.
 *
 * <p>Query services should:
 * <ul>
 *   <li>Be read-only (no state modifications)</li>
 *   <li>Return DTOs (not domain entities)</li>
 *   <li>Use optimized queries (possibly bypassing the repository)</li>
 *   <li>Support pagination and filtering</li>
 * </ul>
 *
 * <p>Implementation classes should be annotated with:
 * <pre>
 * {@code @Service}
 * {@code @Transactional(readOnly = true)}
 * </pre>
 *
 * @param <ID> the type of entity identifier
 * @param <D>  the type of the DTO returned
 */
@Transactional(readOnly = true)
public interface QueryService<ID, D> {

    /**
     * Finds an entity by its ID and returns as DTO.
     *
     * @param id the entity ID
     * @return the DTO representation
     * @throws com.school.management.exception.BusinessException if not found
     */
    D findById(ID id);

    /**
     * Checks if an entity exists with the given ID.
     *
     * @param id the entity ID
     * @return true if exists, false otherwise
     */
    boolean existsById(ID id);
}
