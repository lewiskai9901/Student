package com.school.management.domain.shared;

import java.io.Serializable;

/**
 * Marker interface for value objects in DDD.
 * Value objects are immutable and compared by their attributes.
 */
public interface ValueObject extends Serializable {

    /**
     * Validates the value object's invariants.
     * Should throw IllegalArgumentException if validation fails.
     */
    default void validate() {
        // Override in subclasses to add validation
    }
}
