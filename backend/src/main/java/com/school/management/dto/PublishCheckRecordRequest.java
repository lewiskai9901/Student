package com.school.management.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 发布检查记录请求DTO
 *
 * @author system
 * @since 3.1.0
 */
@Data
@Schema(description = "发布检查记录请求")
public class PublishCheckRecordRequest {

    @Schema(description = "评级配置ID(可选,不传则使用默认配置)")
    private Long configId;
}
