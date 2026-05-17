package com.school.management.domain.inspection.model.template.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.management.domain.inspection.model.template.ItemType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板检查项快照 POJO (J1 真重放).
 *
 * <p>镜像 {@link com.school.management.domain.inspection.model.template.TemplateItem}
 * 的关键字段 (populate 路径使用).
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnapshotItem {

    private Long id;
    private Long sectionId;
    private String itemCode;
    private String itemName;
    private ItemType itemType;
    private String scoringConfig;
    private String validationRules;
    private String conditionLogic;
}
