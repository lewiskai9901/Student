package com.school.management.domain.place.model.valueobject;

import com.school.management.exception.BusinessException;
import lombok.Value;

/**
 * 容量值对象
 */
@Value
public class Capacity {
    Integer maxCapacity;
    Integer currentOccupancy;

    private Capacity(Integer maxCapacity, Integer currentOccupancy) {
        this.maxCapacity = maxCapacity;
        this.currentOccupancy = currentOccupancy != null ? currentOccupancy : 0;
    }

    public static Capacity of(Integer max) {
        return new Capacity(max, 0);
    }

    public static Capacity of(Integer max, Integer current) {
        return new Capacity(max, current);
    }

    public static Capacity empty() {
        return new Capacity(null, 0);
    }

    public Capacity increment() {
        if (isFull()) {
            throw new BusinessException("容量已满");
        }
        return new Capacity(maxCapacity, currentOccupancy + 1);
    }

    public Capacity decrement() {
        if (currentOccupancy <= 0) {
            throw new BusinessException("当前无占用");
        }
        return new Capacity(maxCapacity, currentOccupancy - 1);
    }

    public Capacity updateMax(Integer newMax) {
        if (newMax != null && newMax < currentOccupancy) {
            throw new BusinessException("新容量不能小于当前占用数");
        }
        return new Capacity(newMax, currentOccupancy);
    }

    public boolean isFull() {
        return maxCapacity != null && currentOccupancy >= maxCapacity;
    }

    public boolean isEmpty() {
        return currentOccupancy == null || currentOccupancy == 0;
    }

    public int getAvailable() {
        if (maxCapacity == null) return Integer.MAX_VALUE;
        return Math.max(0, maxCapacity - currentOccupancy);
    }

    public double getOccupancyRate() {
        if (maxCapacity == null || maxCapacity == 0) return 0;
        return (double) currentOccupancy / maxCapacity * 100;
    }

    public boolean hasCapacity() {
        return maxCapacity != null && maxCapacity > 0;
    }
}
