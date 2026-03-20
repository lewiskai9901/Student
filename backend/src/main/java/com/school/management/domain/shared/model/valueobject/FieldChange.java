package com.school.management.domain.shared.model.valueobject;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * 字段变更记录值对象（不可变）
 * 所有领域共用
 */
public class FieldChange {

    private final String fieldName;
    private final String oldValue;
    private final String newValue;

    @JsonCreator
    public FieldChange(@JsonProperty("fieldName") String fieldName,
                       @JsonProperty("oldValue") String oldValue,
                       @JsonProperty("newValue") String newValue) {
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FieldChange that = (FieldChange) o;
        return Objects.equals(fieldName, that.fieldName)
            && Objects.equals(oldValue, that.oldValue)
            && Objects.equals(newValue, that.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fieldName, oldValue, newValue);
    }

    @Override
    public String toString() {
        return fieldName + ": " + oldValue + " → " + newValue;
    }
}
