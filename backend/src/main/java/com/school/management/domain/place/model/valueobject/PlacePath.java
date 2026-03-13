package com.school.management.domain.space.model.valueobject;

import lombok.Value;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 物化路径值对象
 * 用于表示场所的层级路径，如 /1/2/3/
 */
@Value
public class SpacePath {
    String value;

    private SpacePath(String value) {
        this.value = value != null ? value : "/";
    }

    /**
     * 创建根路径
     */
    public static SpacePath root() {
        return new SpacePath("/");
    }

    /**
     * 从字符串创建路径
     */
    public static SpacePath of(String path) {
        return new SpacePath(path);
    }

    /**
     * 为新节点创建路径（在当前路径后追加ID）
     */
    public SpacePath append(Long id) {
        if (id == null) {
            return this;
        }
        return new SpacePath(this.value + id + "/");
    }

    /**
     * 检查当前路径是否是另一个路径的祖先
     */
    public boolean isAncestorOf(SpacePath other) {
        if (other == null) return false;
        return other.value.startsWith(this.value) && !this.value.equals(other.value);
    }

    /**
     * 检查当前路径是否是另一个路径的后代
     */
    public boolean isDescendantOf(SpacePath other) {
        if (other == null) return false;
        return this.value.startsWith(other.value) && !this.value.equals(other.value);
    }

    /**
     * 获取祖先ID列表（从根到父）
     */
    public List<Long> getAncestorIds() {
        if (value == null || value.equals("/")) {
            return Collections.emptyList();
        }
        return Arrays.stream(value.split("/"))
            .filter(s -> !s.isEmpty())
            .map(Long::parseLong)
            .collect(Collectors.toList());
    }

    /**
     * 获取深度（层级数）
     */
    public int getDepth() {
        return getAncestorIds().size();
    }

    /**
     * 获取父路径
     */
    public SpacePath getParentPath() {
        List<Long> ancestors = getAncestorIds();
        if (ancestors.isEmpty() || ancestors.size() == 1) {
            return root();
        }
        StringBuilder sb = new StringBuilder("/");
        for (int i = 0; i < ancestors.size() - 1; i++) {
            sb.append(ancestors.get(i)).append("/");
        }
        return new SpacePath(sb.toString());
    }

    /**
     * 用于 LIKE 查询的模式
     */
    public String getLikePattern() {
        return this.value + "%";
    }

    @Override
    public String toString() {
        return value;
    }
}
