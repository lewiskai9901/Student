package com.school.management.application.shared;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.school.management.domain.shared.ConfigurableType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generic tree builder for ConfigurableType entities.
 * Eliminates duplicated TreeNode inner classes and buildTree() methods
 * across OrgUnitTypeApplicationService, UserTypeApplicationService, UniversalPlaceTypeApplicationService.
 */
public final class TypeTreeBuilder {

    private TypeTreeBuilder() {}

    /**
     * Generic tree node wrapping any ConfigurableType entity.
     * {@code @JsonUnwrapped} flattens entity fields so JSON output
     * is identical to the previous hand-rolled TreeNode classes.
     */
    public static class TypeTreeNode<T extends ConfigurableType> {

        @JsonUnwrapped
        private T data;

        private List<TypeTreeNode<T>> children;

        public TypeTreeNode() {}

        public TypeTreeNode(T data) {
            this.data = data;
        }

        public T getData() { return data; }
        public void setData(T data) { this.data = data; }

        public List<TypeTreeNode<T>> getChildren() { return children; }
        public void setChildren(List<TypeTreeNode<T>> children) { this.children = children; }
    }

    /**
     * Build a tree from a flat list of ConfigurableType entities,
     * grouped by parentTypeCode.
     */
    public static <T extends ConfigurableType> List<TypeTreeNode<T>> buildTree(List<T> allTypes) {
        Map<String, List<T>> grouped = allTypes.stream()
                .collect(Collectors.groupingBy(
                        t -> t.getParentTypeCode() == null ? "" : t.getParentTypeCode()
                ));
        return buildTreeRecursive(grouped, "");
    }

    private static <T extends ConfigurableType> List<TypeTreeNode<T>> buildTreeRecursive(
            Map<String, List<T>> grouped, String parentCode) {
        List<T> children = grouped.getOrDefault(parentCode, new ArrayList<>());
        return children.stream()
                .map(type -> {
                    TypeTreeNode<T> node = new TypeTreeNode<>(type);
                    node.setChildren(buildTreeRecursive(grouped, type.getTypeCode()));
                    return node;
                })
                .collect(Collectors.toList());
    }
}
