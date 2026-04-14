package com.school.management.infrastructure.casbin;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * 单元测试 CasbinConfig.splitLastColon:
 * <p>该方法是 permission_code → (resource, action) 的唯一入口。
 * 按最后一个冒号切分,而不是第一个 — 这是为了让 @CasbinAccess(resource="academic:course", action="view")
 * 能与 DB 中的 permission_code="academic:course:view" 匹配。若改回 split(":", 2) 会静默破坏双冒号权限。
 */
class CasbinConfigTest {

    @Test
    @DisplayName("双冒号按最后一个切分: academic:course:view → [academic:course, view]")
    void doubleColon_splitsAtLast() {
        assertArrayEquals(
                new String[]{"academic:course", "view"},
                CasbinConfig.splitLastColon("academic:course:view"));
    }

    @Test
    @DisplayName("单冒号按最后一个切分: system:user → [system, user]")
    void singleColon_splitsNormally() {
        assertArrayEquals(
                new String[]{"system", "user"},
                CasbinConfig.splitLastColon("system:user"));
    }

    @Test
    @DisplayName("三冒号按最后一个切分: a:b:c:d → [a:b:c, d]")
    void tripleColon_splitsAtLast() {
        assertArrayEquals(
                new String[]{"a:b:c", "d"},
                CasbinConfig.splitLastColon("a:b:c:d"));
    }

    @Test
    @DisplayName("无冒号降级为通配: calendar → [calendar, *]")
    void noColon_defaultsToStarAction() {
        assertArrayEquals(
                new String[]{"calendar", "*"},
                CasbinConfig.splitLastColon("calendar"));
    }

    @Test
    @DisplayName("尾冒号视作无有效 action,降级为通配: foo: → [foo:, *]")
    void trailingColon_defaultsToStarAction() {
        assertArrayEquals(
                new String[]{"foo:", "*"},
                CasbinConfig.splitLastColon("foo:"));
    }

    @Test
    @DisplayName("前导冒号(首位)也视为无效,降级为通配: :bar → [:bar, *]")
    void leadingColon_defaultsToStarAction() {
        assertArrayEquals(
                new String[]{":bar", "*"},
                CasbinConfig.splitLastColon(":bar"));
    }
}
