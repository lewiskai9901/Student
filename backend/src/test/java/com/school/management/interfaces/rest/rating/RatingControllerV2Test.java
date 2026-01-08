package com.school.management.interfaces.rest.rating;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.management.application.rating.RatingApplicationService;
import com.school.management.domain.rating.model.RatingConfig;
import com.school.management.domain.rating.model.RatingPeriodType;
import com.school.management.security.JwtTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for RatingControllerV2.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("Rating V2 API Tests")
class RatingControllerV2Test {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RatingApplicationService ratingService;

    private RatingConfig testConfig;

    @BeforeEach
    void setUp() {
        testConfig = RatingConfig.create(
            1L,
            "Weekly Top 10",
            RatingPeriodType.WEEKLY,
            com.school.management.domain.rating.model.DivisionMethod.TOP_N,
            BigDecimal.TEN,
            1L
        );
        // Set ID via reflection for testing
        try {
            var idField = testConfig.getClass().getSuperclass().getSuperclass().getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(testConfig, 1L);
        } catch (Exception e) {
            // Ignore
        }
    }

    @Nested
    @DisplayName("GET /api/v2/ratings/configs")
    class GetConfigsTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return all configs for admin")
        void shouldReturnAllConfigs() throws Exception {
            when(ratingService.getAllConfigs()).thenReturn(List.of(testConfig));

            mockMvc.perform(get("/api/v2/ratings/configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].ratingName").value("Weekly Top 10"));

            verify(ratingService).getAllConfigs();
        }

        @Test
        @DisplayName("Should return 401 for unauthenticated request")
        void shouldReturn401ForUnauthenticated() throws Exception {
            mockMvc.perform(get("/api/v2/ratings/configs"))
                .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("POST /api/v2/ratings/configs")
    class CreateConfigTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should create rating config")
        void shouldCreateConfig() throws Exception {
            CreateRatingConfigRequest request = new CreateRatingConfigRequest();
            request.setCheckPlanId(1L);
            request.setRatingName("Monthly Top 5");
            request.setPeriodType("MONTHLY");
            request.setDivisionMethod("TOP_N");
            request.setDivisionValue(BigDecimal.valueOf(5));

            when(ratingService.createConfig(any())).thenReturn(testConfig);

            mockMvc.perform(post("/api/v2/ratings/configs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ratingName").value("Weekly Top 10"));

            verify(ratingService).createConfig(any());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should return 400 for invalid request")
        void shouldReturn400ForInvalidRequest() throws Exception {
            CreateRatingConfigRequest request = new CreateRatingConfigRequest();
            // Missing required fields

            mockMvc.perform(post("/api/v2/ratings/configs")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v2/ratings/configs/{id}")
    class GetConfigByIdTests {

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return config by ID")
        void shouldReturnConfigById() throws Exception {
            when(ratingService.getConfigById(1L)).thenReturn(Optional.of(testConfig));

            mockMvc.perform(get("/api/v2/ratings/configs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.ratingName").value("Weekly Top 10"));

            verify(ratingService).getConfigById(1L);
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("Should return 404 for non-existent config")
        void shouldReturn404ForNonExistent() throws Exception {
            when(ratingService.getConfigById(999L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/v2/ratings/configs/999"))
                .andExpect(status().isNotFound());

            verify(ratingService).getConfigById(999L);
        }
    }

    @Nested
    @DisplayName("Rating Result State Transitions")
    class StateTransitionTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should submit result for approval")
        void shouldSubmitForApproval() throws Exception {
            doNothing().when(ratingService).submitForApproval(anyLong(), anyLong());

            mockMvc.perform(post("/api/v2/ratings/results/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(ratingService).submitForApproval(eq(1L), anyLong());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should approve result")
        void shouldApproveResult() throws Exception {
            doNothing().when(ratingService).approveResult(anyLong(), anyLong());

            mockMvc.perform(post("/api/v2/ratings/results/1/approve"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(ratingService).approveResult(eq(1L), anyLong());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("Should publish result")
        void shouldPublishResult() throws Exception {
            doNothing().when(ratingService).publishResult(anyLong(), anyLong());

            mockMvc.perform(post("/api/v2/ratings/results/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

            verify(ratingService).publishResult(eq(1L), anyLong());
        }
    }
}
