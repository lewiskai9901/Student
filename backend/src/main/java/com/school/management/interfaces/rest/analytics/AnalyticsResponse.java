package com.school.management.interfaces.rest.analytics;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AnalyticsResponse {
    private String type;
    private List<Map<String, Object>> data;

    public static AnalyticsResponse of(String type, List<Map<String, Object>> data) {
        AnalyticsResponse response = new AnalyticsResponse();
        response.setType(type);
        response.setData(data);
        return response;
    }
}
