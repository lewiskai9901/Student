package com.school.management.domain.place.model.valueobject;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * GenderType 性别约束逻辑测试 — 场所混住校验的核心判定 (2026-06-13 构建)。
 *
 * 背景: 场所 gender 列 (MALE/FEMALE/MIXED 字符串) 与 users.gender (1男/2女 Integer)
 * 表示不同; GenderType 的 code 对齐 users.gender 口径, 用于 check-in 时比对入住者性别
 * 与场所性别限制。此前这套逻辑存在但从未在入住路径调用 (混住校验形同虚设)。
 */
@DisplayName("性别约束逻辑")
class GenderTypeTest {

    @Test
    @DisplayName("fromName 解析场所 gender 列: MALE/FEMALE/MIXED, 空/未知→MIXED")
    void fromName() {
        assertThat(GenderType.fromName("MALE")).isEqualTo(GenderType.MALE);
        assertThat(GenderType.fromName("FEMALE")).isEqualTo(GenderType.FEMALE);
        assertThat(GenderType.fromName("MIXED")).isEqualTo(GenderType.MIXED);
        assertThat(GenderType.fromName("male")).isEqualTo(GenderType.MALE); // 大小写不敏感
        assertThat(GenderType.fromName(null)).isEqualTo(GenderType.MIXED);
        assertThat(GenderType.fromName("")).isEqualTo(GenderType.MIXED);
        assertThat(GenderType.fromName("GARBAGE")).isEqualTo(GenderType.MIXED);
    }

    @Test
    @DisplayName("allowsOccupant: 不限(MIXED)允许任何人, 含未知性别")
    void allowsOccupant_mixed_allowsAll() {
        assertThat(GenderType.MIXED.allowsOccupant(1)).isTrue();
        assertThat(GenderType.MIXED.allowsOccupant(2)).isTrue();
        assertThat(GenderType.MIXED.allowsOccupant(null)).isTrue();
        assertThat(GenderType.MIXED.allowsOccupant(0)).isTrue();
    }

    @Test
    @DisplayName("allowsOccupant: 限男(MALE)只允许 gender=1, 拒女/未知")
    void allowsOccupant_male_onlyMale() {
        assertThat(GenderType.MALE.allowsOccupant(1)).isTrue();   // 男
        assertThat(GenderType.MALE.allowsOccupant(2)).isFalse();  // 女 → 拒
        assertThat(GenderType.MALE.allowsOccupant(null)).isFalse(); // 未知 → 拒
        assertThat(GenderType.MALE.allowsOccupant(0)).isFalse();
    }

    @Test
    @DisplayName("allowsOccupant: 限女(FEMALE)只允许 gender=2, 拒男/未知")
    void allowsOccupant_female_onlyFemale() {
        assertThat(GenderType.FEMALE.allowsOccupant(2)).isTrue();  // 女
        assertThat(GenderType.FEMALE.allowsOccupant(1)).isFalse(); // 男 → 拒
        assertThat(GenderType.FEMALE.allowsOccupant(null)).isFalse();
    }

    @Test
    @DisplayName("code 与 users.gender 口径对齐 (1男/2女)")
    void codesAlignWithUserGender() {
        assertThat(GenderType.MALE.getCode()).isEqualTo(1);
        assertThat(GenderType.FEMALE.getCode()).isEqualTo(2);
        assertThat(GenderType.MIXED.getCode()).isEqualTo(0);
    }
}
