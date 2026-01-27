package com.school.management.infrastructure.cache;

import com.school.management.domain.inspection.event.InspectionRecordPublishedEvent;
import com.school.management.domain.inspection.event.InspectionTemplateCreatedEvent;
import com.school.management.domain.inspection.event.SessionPublishedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 缓存失效事件处理器
 *
 * <p>监听检查领域事件并自动失效相关缓存，确保缓存一致性。
 *
 * <p>失效策略:
 * <ul>
 *   <li>模板创建/变更 -> 失效模板缓存</li>
 *   <li>检查会话发布 -> 失效排名和分析缓存</li>
 *   <li>检查记录发布 -> 失效排名和分析缓存</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InspectionCacheEventHandler {

    private final InspectionCacheService cacheService;

    /**
     * 模板创建时失效模板列表缓存。
     */
    @Async
    @EventListener
    public void onTemplateCreated(InspectionTemplateCreatedEvent event) {
        log.info("模板变更, 失效模板缓存: templateId={}, templateCode={}",
                event.getTemplateId(), event.getTemplateCode());
        cacheService.invalidateTemplates();
    }

    /**
     * 检查会话发布时失效排名和分析缓存。
     */
    @Async
    @EventListener
    public void onSessionPublished(SessionPublishedEvent event) {
        log.info("检查会话发布, 失效排名和分析缓存: sessionId={}, sessionCode={}",
                event.getSessionId(), event.getSessionCode());
        cacheService.invalidateRankings();
        cacheService.invalidateAnalytics();
    }

    /**
     * 检查记录发布时失效排名和分析缓存。
     */
    @Async
    @EventListener
    public void onRecordPublished(InspectionRecordPublishedEvent event) {
        log.info("检查记录发布, 失效排名和分析缓存: recordId={}, recordCode={}",
                event.getRecordId(), event.getRecordCode());
        cacheService.invalidateRankings();
        cacheService.invalidateAnalytics();
    }
}
