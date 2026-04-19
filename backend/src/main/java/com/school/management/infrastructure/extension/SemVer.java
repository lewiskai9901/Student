package com.school.management.infrastructure.extension;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 极简 SemVer + 版本范围比较器.
 *
 * 支持的版本范围语法:
 *   ">=1.0.0"          至少 1.0.0
 *   "<2.0.0"           低于 2.0.0
 *   ">=1.0.0 <2.0.0"   [1.0.0, 2.0.0) 即 1.x
 *   "*"                任意
 *   null / ""          任意
 *
 * 使用:
 *   SemVer.satisfies("1.2.3", ">=1.0.0 <2.0.0")  // true
 */
public final class SemVer {

    private final int major, minor, patch;

    private SemVer(int major, int minor, int patch) {
        this.major = major; this.minor = minor; this.patch = patch;
    }

    /** 解析 "1.2.3" 为 SemVer 对象 */
    public static SemVer parse(String s) {
        if (s == null || s.isBlank()) return new SemVer(0, 0, 0);
        String[] parts = s.replaceAll("[^0-9.]", "").split("\\.");
        int mj = parts.length > 0 ? parseOrZero(parts[0]) : 0;
        int mn = parts.length > 1 ? parseOrZero(parts[1]) : 0;
        int pt = parts.length > 2 ? parseOrZero(parts[2]) : 0;
        return new SemVer(mj, mn, pt);
    }

    private static int parseOrZero(String s) {
        try { return Integer.parseInt(s); } catch (Exception e) { return 0; }
    }

    /** 比较: 本版本 vs other. 返回 负/零/正 */
    public int compareTo(SemVer o) {
        if (major != o.major) return Integer.compare(major, o.major);
        if (minor != o.minor) return Integer.compare(minor, o.minor);
        return Integer.compare(patch, o.patch);
    }

    @Override
    public String toString() { return major + "." + minor + "." + patch; }

    /**
     * 判断版本是否满足范围条件.
     * 支持空格分隔的多个约束 (如 ">=1.0.0 <2.0.0"), 全部满足为 true.
     */
    public static boolean satisfies(String version, String range) {
        if (range == null || range.isBlank() || "*".equals(range.trim())) return true;
        SemVer v = parse(version);
        for (String clause : range.trim().split("\\s+")) {
            if (!matchesOne(v, clause)) return false;
        }
        return true;
    }

    private static final Pattern CLAUSE = Pattern.compile("^(>=|<=|>|<|=)?\\s*(.+)$");

    private static boolean matchesOne(SemVer v, String clause) {
        Matcher m = CLAUSE.matcher(clause);
        if (!m.matches()) return false;
        String op = m.group(1);
        SemVer target = parse(m.group(2));
        int cmp = v.compareTo(target);
        if (op == null || op.isEmpty() || "=".equals(op)) return cmp == 0;
        return switch (op) {
            case ">"  -> cmp >  0;
            case ">=" -> cmp >= 0;
            case "<"  -> cmp <  0;
            case "<=" -> cmp <= 0;
            default    -> false;
        };
    }
}
