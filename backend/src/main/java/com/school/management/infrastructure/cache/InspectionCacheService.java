package com.school.management.infrastructure.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * 检查数据缓存服务
 *
 * <p>封装检查领域的缓存操作，委托给通用 {@link CacheService} 执行。
 *
 * <p>缓存策略:
 * <ul>
 *   <li>模板列表: 缓存1小时 (模板变更频率低)</li>
 *   <li>排名数据: 缓存30分钟 (检查发布后失效)</li>
 *   <li>分析快照: 缓存2小时 (每日生成)</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class InspectionCacheService {

    private final CacheService cacheService;

    private static final String INSPECTION_PREFIX = "inspection:";
    private static final Duration TEMPLATE_TTL = Duration.ofHours(1);
    private static final Duration RANKING_TTL = Duration.ofMinutes(30);
    private static final Duration ANALYTICS_TTL = Duration.ofHours(2);

    // ========== Generic Cache Operations ==========

    /**
     * 从缓存获取数据，如果缓存未命中则通过 loader 加载并写入缓存。
     *
     * @param key    缓存键（不含前缀）
     * @param ttl    缓存过期时间
     * @param loader 缓存未命中时的数据加载函数
     * @param <T>    数据类型
     * @return 缓存或加载的数据
     */
    public <T> T getOrLoad(String key, Duration ttl, Supplier<T> loader) {
        return cacheService.getOrLoad(INSPECTION_PREFIX + key, ttl, loader);
    }

    /**
     * 从缓存获取数据。
     *
     * @param key 缓存键（不含前缀）
     * @param <T> 数据类型
     * @return 缓存的数据，未命中时返回 null
     */
    public <T> T get(String key) {
        return cacheService.get(INSPECTION_PREFIX + key);
    }

    /**
     * 写入缓存。
     *
     * @param key   缓存键（不含前缀）
     * @param value 缓存值
     * @param ttl   缓存过期时间
     */
    public void put(String key, Object value, Duration ttl) {
        cacheService.put(INSPECTION_PREFIX + key, value, ttl);
    }

    // ========== Template Cache ==========

    /**
     * 获取或加载已发布的检查模板列表。
     *
     * @param loader 模板数据加载函数
     * @param <T>    返回类型
     * @return 模板列表
     */
    public <T> T getOrLoadPublishedTemplates(Supplier<T> loader) {
        return getOrLoad("templates:published", TEMPLATE_TTL, loader);
    }

    /**
     * 缓存已发布的模板列表。
     *
     * @param templates 模板数据
     */
    public void cachePublishedTemplates(Object templates) {
        put("templates:published", templates, TEMPLATE_TTL);
    }

    /**
     * 获取或加载单个模板详情。
     *
     * @param templateId 模板ID
     * @param loader     数据加载函数
     * @param <T>        返回类型
     * @return 模板详情
     */
    public <T> T getOrLoadTemplate(Long templateId, Supplier<T> loader) {
        return getOrLoad("templates:" + templateId, TEMPLATE_TTL, loader);
    }

    // ========== Ranking Cache ==========

    /**
     * 获取或加载部门排名数据。
     *
     * @param dateRange 日期范围标识 (如 "2026-01-01_2026-01-31")
     * @param loader    数据加载函数
     * @param <T>       返回类型
     * @return 排名数据
     */
    public <T> T getOrLoadDepartmentRanking(String dateRange, Supplier<T> loader) {
        return getOrLoad("ranking:department:" + dateRange, RANKING_TTL, loader);
    }

    /**
     * 缓存部门排名数据。
     *
     * @param dateRange 日期范围标识
     * @param ranking   排名数据
     */
    public void cacheDepartmentRanking(String dateRange, Object ranking) {
        put("ranking:department:" + dateRange, ranking, RANKING_TTL);
    }

    /**
     * 获取或加载班级排名数据。
     *
     * @param dateRange 日期范围标识
     * @param loader    数据加载函数
     * @param <T>       返回类型
     * @return 排名数据
     */
    public <T> T getOrLoadClassRanking(String dateRange, Supplier<T> loader) {
        return getOrLoad("ranking:class:" + dateRange, RANKING_TTL, loader);
    }

    // ========== Analytics Cache ==========

    /**
     * 获取或加载分析快照数据。
     *
     * @param snapshotType 快照类型标识
     * @param loader       数据加载函数
     * @param <T>          返回类型
     * @return 分析数据
     */
    public <T> T getOrLoadAnalyticsSnapshot(String snapshotType, Supplier<T> loader) {
        return getOrLoad("analytics:" + snapshotType, ANALYTICS_TTL, loader);
    }

    /**
     * 缓存分析快照数据。
     *
     * @param snapshotType 快照类型标识
     * @param data         分析数据
     */
    public void cacheAnalyticsSnapshot(String snapshotType, Object data) {
        put("analytics:" + snapshotType, data, ANALYTICS_TTL);
    }

    // ========== Invalidation ==========

    /**
     * 失效所有模板缓存。
     */
    public void invalidateTemplates() {
        cacheService.evictByPattern(INSPECTION_PREFIX + "templates:*");
        log.info("Invalidated inspection template cache");
    }

    /**
     * 失效指定模板缓存。
     *
     * @param templateId 模板ID
     */
    public void invalidateTemplate(Long templateId) {
        cacheService.evict(INSPECTION_PREFIX + "templates:" + templateId);
        log.debug("Invalidated template cache: templateId={}", templateId);
    }

    /**
     * 失效所有排名缓存。
     */
    public void invalidateRankings() {
        cacheService.evictByPattern(INSPECTION_PREFIX + "ranking:*");
        log.info("Invalidated inspection ranking cache");
    }

    /**
     * 失效所有分析缓存。
     */
    public void invalidateAnalytics() {
        cacheService.evictByPattern(INSPECTION_PREFIX + "analytics:*");
        log.info("Invalidated inspection analytics cache");
    }

    /**
     * 失效检查领域所有缓存。
     */
    public void invalidateAll() {
        cacheService.evictByPattern(INSPECTION_PREFIX + "*");
        log.info("Invalidated all inspection cache");
    }
}
