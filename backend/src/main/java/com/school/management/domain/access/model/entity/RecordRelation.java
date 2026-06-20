package com.school.management.domain.access.model.entity;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 记录↔主体关系实体 (统一锚定 R4) —— "某业务记录对某主体拥有某关系"。
 *
 * <p>与 {@link AccessRelation} (纯主体图, subject↔subject) <b>分立</b>: {@code recordId} 是业务记录
 * (非主体), 扁平、无传递 BFS、无主体锁键生成列。承载 {@code RECORD_RELATION} storage_kind 的多值
 * 关系 (reviewer / 多被检查 / shared_with 等)。对应表 {@code record_relations} (设计稿 §5.2)。
 *
 * <p>⚠ R4 地基: 实体 + 仓储已建, 但引擎 (ScopeEvaluator) 尚未消费 —— RECORD_RELATION 分支接线
 * 是 R4 后续步。
 */
@Data
@Builder
public class RecordRelation {

    private Long id;

    /** 资源码 (对应 {@code data_resources.resource_code})。 */
    private String resourceCode;

    /** 业务记录 id (非主体)。 */
    private Long recordId;

    /** 关系码: reviewer / inspected / shared_with ... */
    private String relationCode;

    /** 主体类型: USER / ORG_UNIT / PLACE / ASSET。 */
    private String subjectType;

    /** 主体 id。 */
    private Long subjectId;

    /** 访问级别: READ_ONLY / FULL / OWNER。 */
    @Builder.Default
    private String accessLevel = "READ_ONLY";

    /** 关系开始生效时间。null = 创建时立即生效。 */
    private LocalDateTime validFrom;

    /** 关系到期时间。null = 永久有效 (直到软删)。 */
    private LocalDateTime validTo;

    /** 扩展字段。 */
    private Map<String, Object> metadata;

    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long tenantId;
}
