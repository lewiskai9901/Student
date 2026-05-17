package com.school.management.domain.inspection.model.template.snapshot;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.school.management.domain.inspection.model.execution.TargetType;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 模板分区快照 POJO (J1 真重放).
 *
 * <p>镜像 {@link com.school.management.domain.inspection.model.template.TemplateSection}
 * 的关键字段, 用于从 {@code TemplateVersion.structureSnapshot} JSON 反序列化.
 * 不参与持久化, 只作为 in-memory snapshot tree 节点.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SnapshotSection {

    private Long id;
    private Long templateId;
    private Long parentSectionId;
    private String sectionCode;
    private String sectionName;
    private TargetType targetType;
    private String targetSourceMode;
    private String targetTypeFilter;
    private String scoringConfig;
}
