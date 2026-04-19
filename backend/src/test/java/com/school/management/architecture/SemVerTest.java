package com.school.management.architecture;

import com.school.management.infrastructure.extension.SemVer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SemVer 单元测试 (Phase 8.2).
 * 覆盖 parse + satisfies + range 语法.
 */
class SemVerTest {

    @Test
    void parseStandardVersions() {
        assertEquals("1.0.0", SemVer.parse("1.0.0").toString());
        assertEquals("2.5.10", SemVer.parse("2.5.10").toString());
        assertEquals("0.0.0", SemVer.parse("").toString());
        assertEquals("0.0.0", SemVer.parse(null).toString());
    }

    @Test
    void parseVersionWithPrefix() {
        assertEquals("1.2.3", SemVer.parse("v1.2.3").toString());
    }

    @Test
    void satisfiesExactMatch() {
        assertTrue(SemVer.satisfies("1.0.0", "=1.0.0"));
        assertTrue(SemVer.satisfies("1.0.0", "1.0.0"));
        assertFalse(SemVer.satisfies("1.0.1", "=1.0.0"));
    }

    @Test
    void satisfiesRangeBounds() {
        assertTrue(SemVer.satisfies("1.0.0", ">=1.0.0"));
        assertTrue(SemVer.satisfies("1.5.0", ">=1.0.0"));
        assertFalse(SemVer.satisfies("0.9.0", ">=1.0.0"));

        assertTrue(SemVer.satisfies("1.0.0", "<=1.0.0"));
        assertTrue(SemVer.satisfies("0.9.0", "<=1.0.0"));
        assertFalse(SemVer.satisfies("1.0.1", "<=1.0.0"));

        assertTrue(SemVer.satisfies("1.5.0", ">1.0.0"));
        assertFalse(SemVer.satisfies("1.0.0", ">1.0.0"));

        assertTrue(SemVer.satisfies("0.9.0", "<1.0.0"));
        assertFalse(SemVer.satisfies("1.0.0", "<1.0.0"));
    }

    @Test
    void satisfiesCompoundRange() {
        // 1.x 兼容 = [1.0.0, 2.0.0)
        assertTrue(SemVer.satisfies("1.0.0", ">=1.0.0 <2.0.0"));
        assertTrue(SemVer.satisfies("1.5.3", ">=1.0.0 <2.0.0"));
        assertTrue(SemVer.satisfies("1.99.99", ">=1.0.0 <2.0.0"));
        assertFalse(SemVer.satisfies("2.0.0", ">=1.0.0 <2.0.0"));
        assertFalse(SemVer.satisfies("0.9.9", ">=1.0.0 <2.0.0"));
    }

    @Test
    void satisfiesWildcardAndEmpty() {
        assertTrue(SemVer.satisfies("1.0.0", "*"));
        assertTrue(SemVer.satisfies("99.99.99", "*"));
        assertTrue(SemVer.satisfies("1.0.0", null));
        assertTrue(SemVer.satisfies("1.0.0", ""));
        assertTrue(SemVer.satisfies("1.0.0", "  "));
    }

    @Test
    void compareToOrdering() {
        assertTrue(SemVer.parse("1.0.0").compareTo(SemVer.parse("1.0.1")) < 0);
        assertTrue(SemVer.parse("1.1.0").compareTo(SemVer.parse("1.0.99")) > 0);
        assertTrue(SemVer.parse("2.0.0").compareTo(SemVer.parse("1.99.99")) > 0);
        assertEquals(0, SemVer.parse("1.2.3").compareTo(SemVer.parse("1.2.3")));
    }
}
