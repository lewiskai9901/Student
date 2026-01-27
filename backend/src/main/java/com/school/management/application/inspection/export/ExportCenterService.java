package com.school.management.application.inspection.export;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ExportCenterService {

    private final Map<ExportScenario, ExportStrategy> strategyMap;

    public ExportCenterService(List<ExportStrategy> strategies) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(ExportStrategy::getScenario, s -> s));
        log.info("导出中心初始化: {} 种导出场景已注册", strategyMap.size());
    }

    public ExportResult export(ExportRequest request) {
        ExportStrategy strategy = strategyMap.get(request.getScenario());
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的导出场景: " + request.getScenario());
        }

        log.info("开始导出: scenario={}, format={}", request.getScenario(), request.getFormat());
        return strategy.execute(request);
    }

    public long estimateCount(ExportRequest request) {
        ExportStrategy strategy = strategyMap.get(request.getScenario());
        if (strategy == null) {
            return 0;
        }
        return strategy.estimateRecordCount(request);
    }

    public List<Map<String, String>> getAvailableScenarios() {
        List<Map<String, String>> scenarios = new ArrayList<>();
        for (ExportScenario scenario : ExportScenario.values()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("code", scenario.name());
            item.put("name", scenario.getName());
            item.put("description", scenario.getDescription());
            scenarios.add(item);
        }
        return scenarios;
    }
}
