package com.school.management.domain.shared;

import java.io.Serializable;
import java.util.Optional;

/**
 * Base repository interface for aggregate roots.
 * Repositories provide persistence abstraction for aggregates.
 *
 * @param <T>  the aggregate root type
 * @param <ID> the type of the aggregate identifier
 */
public interface Repository<T extends Entity<ID>, ID extends Serializable> {

    /**
     * Finds an aggregate by its ID.
     *
     * @param id the aggregate ID
     * @return an Optional containing the aggregate if found
     */
    Optional<T> findById(ID id);

    /**
     * Saves an aggregate (create or update).
     *
     * @param aggregate the aggregate to save
     * @return the saved aggregate
     */
    T save(T aggregate);

    /**
     * Deletes an aggregate.
     *
     * @param aggregate the aggregate to delete
     */
    void delete(T aggregate);

    /**
     * Deletes an aggregate by its ID.
     *
     * @param id the aggregate ID to delete
     */
    default void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    /**
     * Checks if an aggregate exists by its ID.
     *
     * @param id the aggregate ID
     * @return true if exists, false otherwise
     */
    default boolean existsById(ID id) {
        return findById(id).isPresent();
    }
}
