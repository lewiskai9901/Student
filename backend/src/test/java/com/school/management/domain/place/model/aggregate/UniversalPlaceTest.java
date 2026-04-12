package com.school.management.domain.place.model.aggregate;

import com.school.management.domain.place.model.valueobject.PlaceStatus;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * UniversalPlace 聚合根领域测试
 *
 * <p>验证核心不变式：
 * <ul>
 *     <li>容量限制：checkIn 不能超过 capacity</li>
 *     <li>占用非负：checkOut 不能使 currentOccupancy 变为负数</li>
 *     <li>无容量限制：capacity 为 null 时无上限</li>
 *     <li>层级关系：isAncestorOf 基于物化路径判断</li>
 * </ul>
 */
class UniversalPlaceTest {

    // ==================== checkIn ====================

    @Test
    void checkIn_whenAtFullCapacity_shouldThrow() {
        UniversalPlace place = buildPlaceWithCapacity(2, 2);

        assertThatThrownBy(place::checkIn)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkIn_whenUnderCapacity_shouldSucceed() {
        UniversalPlace place = buildPlaceWithCapacity(5, 2);

        place.checkIn();

        assertThat(place.getCurrentOccupancy()).isEqualTo(3);
    }

    @Test
    void checkIn_whenCapacityIsNull_shouldAlwaysSucceed() {
        // 无容量限制的场所（如楼栋）
        UniversalPlace place = buildPlaceWithCapacity(null, 100);

        place.checkIn();

        assertThat(place.getCurrentOccupancy()).isEqualTo(101);
    }

    // ==================== checkOut ====================

    @Test
    void checkOut_whenOccupancyIsZero_shouldThrow() {
        UniversalPlace place = buildPlaceWithCapacity(2, 0);

        assertThatThrownBy(place::checkOut)
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void checkOut_shouldDecrementOccupancy() {
        UniversalPlace place = buildPlaceWithCapacity(5, 3);

        place.checkOut();

        assertThat(place.getCurrentOccupancy()).isEqualTo(2);
    }

    // ==================== isAncestorOf ====================

    @Test
    void isAncestorOf_whenChildHasAncestorInPath_shouldReturnTrue() {
        UniversalPlace parent = UniversalPlace.builder()
                .id(1L).placeCode("A").placeName("楼A").typeCode("BUILDING")
                .path("/1/").level(0).status(PlaceStatus.NORMAL)
                .build();
        UniversalPlace child = UniversalPlace.builder()
                .id(5L).placeCode("A-1").placeName("楼A-1层").typeCode("FLOOR")
                .path("/1/5/").level(1).status(PlaceStatus.NORMAL)
                .build();

        assertThat(parent.isAncestorOf(child)).isTrue();
        assertThat(child.isAncestorOf(parent)).isFalse();
    }

    @Test
    void isAncestorOf_selfReference_shouldReturnFalse() {
        UniversalPlace place = UniversalPlace.builder()
                .id(1L).placeCode("A").placeName("A").typeCode("T")
                .path("/1/").level(0).status(PlaceStatus.NORMAL)
                .build();

        assertThat(place.isAncestorOf(place)).isFalse();
    }

    // ==================== Helper ====================

    private UniversalPlace buildPlaceWithCapacity(Integer capacity, Integer currentOccupancy) {
        return UniversalPlace.builder()
                .id(1L)
                .placeCode("R101")
                .placeName("101")
                .typeCode("ROOM")
                .capacity(capacity)
                .currentOccupancy(currentOccupancy)
                .status(PlaceStatus.NORMAL)
                .build();
    }
}
