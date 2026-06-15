package com.school.management.domain.access.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

@DisplayName("OrgAnchor 枚举测试 (数据范围轴①组织锚点)")
class OrgAnchorTest {

    @Test
    @DisplayName("fromCode 解析已知 code, 未知返回 null")
    void fromCode_parsesAll() {
        assertThat(OrgAnchor.fromCode("RELATION")).isEqualTo(OrgAnchor.RELATION);
        assertThat(OrgAnchor.fromCode("nope")).isNull();
    }

    @Test
    @DisplayName("所有取值可按自身 name() 往返解析, null 返回 null")
    void fromCode_roundTripsAllValuesAndNull() {
        for (OrgAnchor a : OrgAnchor.values()) {
            assertThat(OrgAnchor.fromCode(a.name())).isSameAs(a);
        }
        assertThat(OrgAnchor.fromCode(null)).isNull();
    }
}
