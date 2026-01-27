package com.school.management.interfaces.rest.inspection.dto;

import com.school.management.domain.inspection.model.BonusItem;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class BonusItemResponse {

    private Long id;
    private Long categoryId;
    private String itemName;
    private String bonusMode;
    private BigDecimal fixedBonus;
    private String progressiveConfig;
    private BigDecimal improvementCoefficient;
    private String description;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;

    public static BonusItemResponse fromDomain(BonusItem item) {
        BonusItemResponse r = new BonusItemResponse();
        r.setId(item.getId());
        r.setCategoryId(item.getCategoryId());
        r.setItemName(item.getItemName());
        r.setBonusMode(item.getBonusMode().name());
        r.setFixedBonus(item.getFixedBonus());
        r.setProgressiveConfig(item.getProgressiveConfig());
        r.setImprovementCoefficient(item.getImprovementCoefficient());
        r.setDescription(item.getDescription());
        r.setSortOrder(item.getSortOrder());
        r.setStatus(item.getStatus());
        r.setCreatedAt(item.getCreatedAt());
        return r;
    }
}
