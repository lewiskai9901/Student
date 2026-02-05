package com.school.management.domain.asset.model.valueobject;

/**
 * 借用类型
 */
public enum BorrowType {
    USE(1, "领用"),      // 领用 - 无需归还
    BORROW(2, "借用");   // 借用 - 需要归还

    private final int code;
    private final String description;

    BorrowType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }

    public static BorrowType fromCode(int code) {
        for (BorrowType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown BorrowType code: " + code);
    }
}
