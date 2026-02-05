package com.school.management.domain.shared;

import java.io.Serializable;

/**
 * Base interface for all domain entities.
 * Entities have identity and are compared by their ID.
 *
 * @param <ID> the type of the entity identifier
 */
public interface Entity<ID extends Serializable> {

    /**
     * Returns the unique identifier of this entity.
     *
     * @return the entity ID
     */
    ID getId();
}
