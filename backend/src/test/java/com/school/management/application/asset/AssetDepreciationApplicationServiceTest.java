package com.school.management.application.asset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AssetDepreciationApplicationService 单元测试")
class AssetDepreciationApplicationServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AssetDepreciationApplicationService service;

    /** 构造一行 asset 查询结果. method: 1=直线 2=双倍余额递减 3=年数总和. */
    private Map<String, Object> assetRow(int method, BigDecimal original, BigDecimal residual,
                                         BigDecimal accDep, Integer usefulLife) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", 1L);
        m.put("asset_code", "A-001");
        m.put("original_value", original);
        m.put("residual_value", residual);
        m.put("accumulated_depreciation", accDep);
        m.put("useful_life", usefulLife);
        m.put("depreciation_method", method);
        m.put("purchase_date", "2024-01-01");
        return m;
    }

    /** 让 queryForMap 返回给定的 asset 行. */
    private void stubAssetRow(Map<String, Object> row) {
        when(jdbcTemplate.queryForMap(anyString(), any(Object[].class))).thenReturn(row);
    }

    /** 让 used months count 查询返回给定值. */
    private void stubUsedMonths(long count) {
        lenient().when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ?"),
                eq(Long.class), any(Object[].class))).thenReturn(count);
    }

    @Nested
    @DisplayName("currentPeriod")
    class CurrentPeriod {
        @Test
        @DisplayName("返回 6 位 yyyyMM 格式字符串")
        void format() {
            String p = AssetDepreciationApplicationService.currentPeriod();
            assertThat(p).hasSize(6).matches("\\d{6}");
        }
    }

    @Nested
    @DisplayName("calculateAndSave")
    class CalculateAndSave {

        @Test
        @DisplayName("本期已计提抛 IllegalStateException")
        void alreadyDepreciated() {
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(1L);

            assertThatThrownBy(() -> service.calculateAndSave(1L, "202605"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("本期已计提");
        }

        @Test
        @DisplayName("直线法计提成功并写库, 返回结果含 period/date 字段")
        void straightLineSuccess() {
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(0L);
            // 原值 12000, 残值 0, 已折旧 0, 寿命 12 月 -> 月折旧 1000
            stubAssetRow(assetRow(1, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, 12));
            stubUsedMonths(0L);

            Map<String, Object> result = service.calculateAndSave(1L, "202605");

            assertThat(result).isNotNull();
            assertThat((BigDecimal) result.get("depreciationAmount"))
                    .isEqualByComparingTo("1000.00");
            assertThat((BigDecimal) result.get("endingAccumulatedDepreciation"))
                    .isEqualByComparingTo("1000.00");
            assertThat(result).containsEntry("depreciationPeriod", "202605")
                    .containsKey("depreciationDate")
                    .containsKey("createdAt")
                    .containsKey("id");
            // 写 asset_depreciation + 更新 asset 共两次 update
            verify(jdbcTemplate, times(2)).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("资产已折旧到残值返回 null, 不写库")
        void netAtResidualReturnsNull() {
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(0L);
            // 原值 10000, 已折旧 9000, 残值 1000 -> 净值 1000 <= 残值 1000
            stubAssetRow(assetRow(1, new BigDecimal("10000"), new BigDecimal("1000"),
                    new BigDecimal("9000"), 12));

            Map<String, Object> result = service.calculateAndSave(1L, "202605");

            assertThat(result).isNull();
            verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("资产不存在 (queryForMap 抛异常) 返回 null")
        void assetNotFound() {
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(0L);
            when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
                    .thenThrow(new EmptyResultDataAccessException(1));

            assertThat(service.calculateAndSave(1L, "202605")).isNull();
        }

        @Test
        @DisplayName("折旧方法为 0 返回 null")
        void methodZero() {
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(0L);
            stubAssetRow(assetRow(0, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, 12));

            assertThat(service.calculateAndSave(1L, "202605")).isNull();
        }
    }

    @Nested
    @DisplayName("preview")
    class Preview {

        @Test
        @DisplayName("双倍余额递减法预览, 不写库")
        void decliningBalancePreview() {
            // 原值 12000, 寿命 12 -> rate = 2/12 = 0.166667, 净值 12000 -> dep ≈ 2000
            stubAssetRow(assetRow(2, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, 12));
            stubUsedMonths(0L);

            Map<String, Object> result = service.preview(1L, "202605");

            assertThat(result).isNotNull();
            assertThat((BigDecimal) result.get("depreciationAmount"))
                    .isEqualByComparingTo("2000.00");
            assertThat(result).containsEntry("depreciationPeriod", "202605");
            verify(jdbcTemplate, never()).update(anyString(), any(Object[].class));
        }

        @Test
        @DisplayName("年数总和法预览")
        void sumOfYearsPreview() {
            // 原值 12000 残值 0 寿命 12 -> totalSum=78, remainingMonths=12
            // dep = 12000 * 12 / 78 ≈ 1846.15
            stubAssetRow(assetRow(3, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, 12));
            stubUsedMonths(0L);

            Map<String, Object> result = service.preview(1L, "202605");

            assertThat((BigDecimal) result.get("depreciationAmount"))
                    .isEqualByComparingTo("1846.15");
        }

        @Test
        @DisplayName("寿命已用尽 (remainingMonths<=0) 返回 null")
        void noRemainingMonths() {
            stubAssetRow(assetRow(1, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, 12));
            stubUsedMonths(12L);

            assertThat(service.preview(1L, "202605")).isNull();
        }

        @Test
        @DisplayName("寿命为 null 返回 null")
        void nullUsefulLife() {
            stubAssetRow(assetRow(1, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, null));

            assertThat(service.preview(1L, "202605")).isNull();
        }
    }

    @Nested
    @DisplayName("calculateAllAndSave")
    class CalculateAllAndSave {

        @Test
        @DisplayName("无可折旧资产时返回 0")
        void noAssets() {
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of());

            assertThat(service.calculateAllAndSave("202605")).isZero();
        }

        @Test
        @DisplayName("跳过本期已计提的资产, 统计成功处理数")
        void skipsAlreadyDepreciated() {
            Map<String, Object> a1 = Map.of("id", 1L);
            Map<String, Object> a2 = Map.of("id", 2L);
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(a1, a2));
            // 资产1 已计提 -> 跳过; 资产2 未计提 -> 处理
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(1L, 0L);
            stubAssetRow(assetRow(1, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, 12));
            stubUsedMonths(0L);

            int processed = service.calculateAllAndSave("202605");

            assertThat(processed).isEqualTo(1);
        }

        @Test
        @DisplayName("doCalculate 返回 null 的资产不计入处理数")
        void skipsNullResults() {
            when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(Map.of("id", 1L)));
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ? AND depreciation_period = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(0L);
            // 折旧方法 0 -> doCalculate 返回 null
            stubAssetRow(assetRow(0, new BigDecimal("12000"), BigDecimal.ZERO,
                    BigDecimal.ZERO, 12));

            assertThat(service.calculateAllAndSave("202605")).isZero();
        }
    }

    @Nested
    @DisplayName("listHistory")
    class ListHistory {

        @Test
        @DisplayName("按 asset_id 查询并 ORDER BY period DESC, 传参正确")
        void queryByAssetId() {
            List<Map<String, Object>> rows = List.of(Map.of("id", 1L), Map.of("id", 2L));
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> argsCap = ArgumentCaptor.forClass(Object[].class);
            when(jdbcTemplate.queryForList(sqlCap.capture(), argsCap.capture())).thenReturn(rows);

            List<Map<String, Object>> result = service.listHistory(42L);

            assertThat(result).hasSize(2);
            assertThat(sqlCap.getValue()).contains("WHERE asset_id = ?")
                    .contains("ORDER BY depreciation_period DESC");
            assertThat(argsCap.getValue()).containsExactly(42L);
        }
    }

    @Nested
    @DisplayName("listHistoryPaged")
    class ListHistoryPaged {

        @Test
        @DisplayName("返回 records/total/pageNum/pageSize, LIMIT/OFFSET 参数顺序正确")
        void pagedAssembly() {
            when(jdbcTemplate.queryForObject(
                    eq("SELECT COUNT(*) FROM asset_depreciation WHERE asset_id = ?"),
                    eq(Long.class), any(Object[].class))).thenReturn(25L);
            ArgumentCaptor<Object[]> argsCap = ArgumentCaptor.forClass(Object[].class);
            when(jdbcTemplate.queryForList(anyString(), argsCap.capture()))
                    .thenReturn(List.of(Map.of("id", 1L)));

            Map<String, Object> result = service.listHistoryPaged(3L, 2, 10);

            assertThat(result).containsEntry("total", 25L)
                    .containsEntry("pageNum", 2)
                    .containsEntry("pageSize", 10);
            assertThat(result.get("records")).isInstanceOf(List.class);
            // assetId, pageSize(LIMIT), offset(OFFSET=(2-1)*10=10)
            assertThat(argsCap.getValue()).containsExactly(3L, 10, 10);
        }
    }

    @Nested
    @DisplayName("listByPeriod")
    class ListByPeriod {

        @Test
        @DisplayName("按 period 查询并 ORDER BY asset_code")
        void queryByPeriod() {
            ArgumentCaptor<String> sqlCap = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<Object[]> argsCap = ArgumentCaptor.forClass(Object[].class);
            when(jdbcTemplate.queryForList(sqlCap.capture(), argsCap.capture()))
                    .thenReturn(List.of(Map.of("id", 1L)));

            List<Map<String, Object>> result = service.listByPeriod("202605");

            assertThat(result).hasSize(1);
            assertThat(sqlCap.getValue()).contains("ORDER BY asset_code");
            assertThat(argsCap.getValue()).containsExactly("202605");
        }
    }
}
