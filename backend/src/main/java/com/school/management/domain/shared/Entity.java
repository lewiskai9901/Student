package com.school.management.domain.shared;

import java.io.Serializable;
import java.util.Objects;

/**
 * Base class for all domain entities.
 * Entities have identity and are compared by their ID.
 *
 * @param <ID> the type of the entity identifier
 */
public abstract class Entity<ID extends Serializable> {

    private ID id;

    /**
     * Returns the unique identifier of this entity.
     *
     * @return the entity ID
     */
    public ID getId() {
        return id;
    }

    /**
     * Sets the unique identifier of this entity.
     *
     * @param id the entity ID
     */
    public void setId(ID id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Entity<?> entity = (Entity<?>) o;
        return Objects.equals(getId(), entity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
