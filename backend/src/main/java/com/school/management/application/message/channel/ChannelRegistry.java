package com.school.management.application.message.channel;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * S-2: 通道注册表 — Spring 启动时自动收集所有 ChannelDispatcher 实现.
 *
 * MessageDispatcher 通过 channelCode 查找对应实现, 不再硬编码 if/else.
 */
@Slf4j
@Component
public class ChannelRegistry {

    private final Map<String, ChannelDispatcher> byCode = new HashMap<>();

    public ChannelRegistry(List<ChannelDispatcher> dispatchers) {
        for (ChannelDispatcher d : dispatchers) {
            String code = d.channelCode().toUpperCase();
            ChannelDispatcher prev = byCode.put(code, d);
            if (prev != null) {
                log.warn("[channel] 重复注册 {}: {} -> {}", code, prev.getClass(), d.getClass());
            }
        }
        log.info("[channel] 注册 {} 个通道: {}", byCode.size(), byCode.keySet());
    }

    public Optional<ChannelDispatcher> find(String channelCode) {
        if (channelCode == null) return Optional.empty();
        return Optional.ofNullable(byCode.get(channelCode.toUpperCase()));
    }

    public boolean isAvailable(String channelCode) {
        return find(channelCode).map(ChannelDispatcher::isAvailable).orElse(false);
    }

    public List<String> availableChannels() {
        return byCode.values().stream()
                .filter(ChannelDispatcher::isAvailable)
                .map(ChannelDispatcher::channelCode)
                .toList();
    }
}
