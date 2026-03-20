package com.school.management.infrastructure.shared;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;

/**
 * Shared JSON serialization utilities for ConfigurableType repository implementations.
 * Eliminates duplicated fromJsonMap/fromJsonList/toJson across OrgType, UserType, PlaceType repos.
 */
@Slf4j
public final class TypeJsonUtils {

    private TypeJsonUtils() {}

    public static Map<String, Boolean> fromJsonMap(ObjectMapper objectMapper, String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Boolean>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON map: {}", e.getMessage());
            return null;
        }
    }

    public static List<String> fromJsonList(ObjectMapper objectMapper, String json) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON list: {}", e.getMessage());
            return null;
        }
    }

    public static <T> T fromJson(ObjectMapper objectMapper, String json, TypeReference<T> typeRef) {
        if (json == null || json.isEmpty()) return null;
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse JSON: {}", e.getMessage());
            return null;
        }
    }

    public static String toJson(ObjectMapper objectMapper, Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize to JSON: {}", e.getMessage());
            return null;
        }
    }
}
