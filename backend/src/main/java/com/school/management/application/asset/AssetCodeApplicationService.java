package com.school.management.application.asset;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * 资产编码生成应用服务 (M1, 2026-05-20).
 *
 * <p>L3 模式的延续: interfaces/rest/asset 共 101 处直 jdbc, 按域逐 controller
 * 抽 ApplicationService. 这是第一个模板, 后续 8 个 controller 按相同方式抽.
 *
 * <p>SVG 二维码/条码渲染留在 Controller 层 (不是 DB 操作, 是表现层逻辑).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssetCodeApplicationService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * 查既有同分类资产数, 用于决定下一个编码序号.
     */
    public int countExistingAssetCodes(String categoryCode) {
        Long count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM asset WHERE asset_code LIKE ? AND deleted = 0",
            Long.class, categoryCode + "-%");
        return count == null ? 0 : count.intValue();
    }
}
