package com.school.management.domain.asset.model.aggregate;

import com.school.management.domain.asset.model.valueobject.BuildingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Building 领域模型测试")
class BuildingTest {

    @Nested
    @DisplayName("创建楼宇")
    class CreateBuildingTests {

        @Test
        @DisplayName("应成功创建楼宇")
        void shouldCreateBuildingSuccessfully() {
            // When
            Building building = Building.create(
                    "B001",
                    "教学楼A",
                    BuildingType.TEACHING,
                    5,
                    "校区东侧"
            );

            // Then
            assertThat(building.getBuildingNo()).isEqualTo("B001");
            assertThat(building.getBuildingName()).isEqualTo("教学楼A");
            assertThat(building.getBuildingType()).isEqualTo(BuildingType.TEACHING);
            assertThat(building.getTotalFloors()).isEqualTo(5);
            assertThat(building.getLocation()).isEqualTo("校区东侧");
            assertThat(building.getStatus()).isEqualTo(1);
            assertThat(building.getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("应成功创建宿舍楼")
        void shouldCreateDormitoryBuilding() {
            // When
            Building building = Building.create(
                    "D001",
                    "男生宿舍1号楼",
                    BuildingType.DORMITORY,
                    6,
                    "生活区北侧"
            );

            // Then
            assertThat(building.getBuildingType()).isEqualTo(BuildingType.DORMITORY);
            assertThat(building.getBuildingType().isDormitory()).isTrue();
        }

        @Test
        @DisplayName("创建时应注册领域事件")
        void shouldRegisterEventOnCreate() {
            // When
            Building building = Building.create(
                    "B001",
                    "教学楼A",
                    BuildingType.TEACHING,
                    5,
                    "校区东侧"
            );

            // Then
            assertThat(building.getDomainEvents()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("更新楼宇信息")
    class UpdateBuildingTests {

        @Test
        @DisplayName("应成功更新楼宇信息")
        void shouldUpdateBuildingInfo() {
            // Given
            Building building = Building.create(
                    "B001",
                    "教学楼A",
                    BuildingType.TEACHING,
                    5,
                    "校区东侧"
            );
            building.clearDomainEvents();

            // When
            building.updateInfo(
                    "新教学楼A",
                    BuildingType.TEACHING,
                    6,
                    "校区西侧",
                    2020,
                    "翻新后的教学楼"
            );

            // Then
            assertThat(building.getBuildingName()).isEqualTo("新教学楼A");
            assertThat(building.getTotalFloors()).isEqualTo(6);
            assertThat(building.getLocation()).isEqualTo("校区西侧");
            assertThat(building.getConstructionYear()).isEqualTo(2020);
            assertThat(building.getDescription()).isEqualTo("翻新后的教学楼");
            assertThat(building.getDomainEvents()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("重建楼宇")
    class ReconstructBuildingTests {

        @Test
        @DisplayName("应成功从持久化重建楼宇")
        void shouldReconstructBuildingSuccessfully() {
            // When
            Building building = Building.reconstruct(
                    1L,
                    "B001",
                    "教学楼A",
                    BuildingType.TEACHING,
                    5,
                    "校区东侧",
                    2018,
                    "主教学楼",
                    1,
                    java.time.LocalDateTime.of(2023, 1, 1, 0, 0),
                    java.time.LocalDateTime.of(2023, 6, 1, 0, 0)
            );

            // Then
            assertThat(building.getId()).isEqualTo(1L);
            assertThat(building.getBuildingNo()).isEqualTo("B001");
            assertThat(building.getConstructionYear()).isEqualTo(2018);
            assertThat(building.getStatus()).isEqualTo(1);
            assertThat(building.getDomainEvents()).isEmpty(); // 重建不注册事件
        }
    }

    @Nested
    @DisplayName("楼宇类型值对象")
    class BuildingTypeTests {

        @Test
        @DisplayName("应正确识别宿舍楼类型")
        void shouldIdentifyDormitoryType() {
            assertThat(BuildingType.DORMITORY.isDormitory()).isTrue();
            assertThat(BuildingType.TEACHING.isDormitory()).isFalse();
            assertThat(BuildingType.OFFICE.isDormitory()).isFalse();
        }

        @Test
        @DisplayName("应正确识别教学楼类型")
        void shouldIdentifyTeachingType() {
            assertThat(BuildingType.TEACHING.isTeaching()).isTrue();
            assertThat(BuildingType.DORMITORY.isTeaching()).isFalse();
        }

        @Test
        @DisplayName("应正确从编码转换类型")
        void shouldConvertFromCode() {
            assertThat(BuildingType.fromCode(1)).isEqualTo(BuildingType.TEACHING);
            assertThat(BuildingType.fromCode(2)).isEqualTo(BuildingType.DORMITORY);
            assertThat(BuildingType.fromCode(3)).isEqualTo(BuildingType.OFFICE);
        }

        @Test
        @DisplayName("未知编码应抛出异常")
        void shouldThrowForUnknownCode() {
            assertThatThrownBy(() -> BuildingType.fromCode(99))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Unknown building type code");
        }

        @Test
        @DisplayName("null编码应返回null")
        void shouldReturnNullForNullCode() {
            assertThat(BuildingType.fromCode(null)).isNull();
        }
    }
}
