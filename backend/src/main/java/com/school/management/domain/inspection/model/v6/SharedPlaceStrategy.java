package com.school.management.domain.inspection.model.v6;

/**
 * V6共享场所分数策略
 */
public enum SharedSpaceStrategy {
    RATIO("按比例", "根据组织归属比例分配分数"),
    AVERAGE("平均", "平均分配给所有相关组织"),
    FULL("全额", "每个相关组织都获得全部分数"),
    MAIN_ONLY("仅主归属", "仅计入主归属组织");

    private final String label;
    private final String description;

    SharedSpaceStrategy(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return name();
    }

    public static SharedSpaceStrategy fromCode(String code) {
        if (code == null) return RATIO;
        for (SharedSpaceStrategy strategy : values()) {
            if (strategy.name().equalsIgnoreCase(code)) {
                return strategy;
            }
        }
        return RATIO;
    }
}
