package com.school.management.domain.asset.model.valueobject;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 折旧方法值对象
 */
@Getter
public enum DepreciationMethod {
    /**
     * 不计提折旧
     */
    NONE(0, "不计提折旧"),

    /**
     * 直线法（年限平均法）
     * 月折旧额 = (原值 - 残值) / 使用年限 / 12
     */
    STRAIGHT_LINE(1, "直线法"),

    /**
     * 双倍余额递减法
     * 月折旧额 = 净值 * (2 / 使用年限 / 12)
     * 最后两年转为直线法
     */
    DOUBLE_DECLINING(2, "双倍余额递减法"),

    /**
     * 年数总和法
     * 月折旧额 = (原值 - 残值) * 剩余年限 / 年数总和 / 12
     */
    SUM_OF_YEARS(3, "年数总和法"),

    /**
     * 工作量法
     * 折旧额 = (原值 - 残值) / 预计总工作量 * 本期工作量
     */
    UNITS_OF_PRODUCTION(4, "工作量法");

    @EnumValue
    @JsonValue
    private final int code;
    private final String description;

    DepreciationMethod(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public static DepreciationMethod fromCode(int code) {
        for (DepreciationMethod method : values()) {
            if (method.code == code) {
                return method;
            }
        }
        return NONE;
    }
}
