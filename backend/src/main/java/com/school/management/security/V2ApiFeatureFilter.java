package com.school.management.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.common.Result;
import com.school.management.config.FeatureToggleConfig;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

/**
 * Filter to check feature toggles for V2 API endpoints.
 *
 * <p>Routes V2 API requests based on feature toggle configuration.
 * When a feature is disabled, returns a 503 Service Unavailable response
 * with instructions to use the V1 API instead.
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class V2ApiFeatureFilter extends OncePerRequestFilter {

    private final FeatureToggleConfig featureToggleConfig;
    private final ObjectMapper objectMapper;

    private static final Map<String, String> PATH_TO_FEATURE = Map.of(
        "/api/v2/ratings", FeatureToggleConfig.V2_RATING_API,
        "/api/v2/tasks", FeatureToggleConfig.V2_TASK_API,
        "/api/v2/inspection", FeatureToggleConfig.V2_INSPECTION_API,
        "/api/v2/org-units", FeatureToggleConfig.V2_ORGANIZATION_API,
        "/api/v2/organization", FeatureToggleConfig.V2_ORGANIZATION_API
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Only check V2 API paths
        if (!path.startsWith("/api/v2/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Find matching feature toggle
        String featureName = findFeatureForPath(path);
        if (featureName == null) {
            // No specific feature toggle, allow through
            filterChain.doFilter(request, response);
            return;
        }

        // Get current user ID if authenticated
        Long userId = getCurrentUserId();

        // Check if feature is enabled for this user
        if (featureToggleConfig.isEnabled(featureName, userId)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Feature is disabled - return 503
        log.info("V2 API disabled: {} for user {}", featureName, userId);

        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Result<?> result = Result.fail(503,
            "V2 API is currently unavailable. Please use V1 API instead.");

        objectMapper.writeValue(response.getWriter(), result);
    }

    private String findFeatureForPath(String path) {
        for (Map.Entry<String, String> entry : PATH_TO_FEATURE.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof CustomUserDetails) {
            return ((CustomUserDetails) auth.getPrincipal()).getId();
        }
        return null;
    }
}
