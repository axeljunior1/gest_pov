package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * DTO client — miroir de CategoryResponse. Pas de règle métier (cycles, enfants, produits).
 */
public record Category(
        Long id,
        String nom,
        Long parentId,
        String parentNom,
        List<Category> children,
        String createdAt,
        String updatedAt
) {

    public static Category fromJson(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }
        Long id = node.hasNonNull("id") ? node.get("id").asLong() : null;
        String nom = node.path("nom").asText("");
        Long parentId = node.hasNonNull("parentId") ? node.get("parentId").asLong() : null;
        String parentNom = node.hasNonNull("parentNom") ? node.get("parentNom").asText() : null;
        List<Category> children = List.of();
        JsonNode childrenNode = node.get("children");
        if (childrenNode != null && childrenNode.isArray()) {
            List<Category> parsed = new ArrayList<>();
            childrenNode.forEach(child -> {
                Category mapped = fromJson(child);
                if (mapped != null) {
                    parsed.add(mapped);
                }
            });
            children = List.copyOf(parsed);
        }
        return new Category(id, nom, parentId, parentNom, children,
                textOrNull(node, "createdAt"), textOrNull(node, "updatedAt"));
    }

    public List<Category> childrenOrEmpty() {
        return children == null ? List.of() : children;
    }

    public static List<Category> flatten(List<Category> roots) {
        List<Category> out = new ArrayList<>();
        if (roots == null) {
            return out;
        }
        for (Category root : roots) {
            collect(root, out);
        }
        return out;
    }

    private static void collect(Category category, List<Category> out) {
        if (category == null) {
            return;
        }
        out.add(category);
        for (Category child : category.childrenOrEmpty()) {
            collect(child, out);
        }
    }

    private static String textOrNull(JsonNode node, String field) {
        if (!node.hasNonNull(field)) {
            return null;
        }
        return node.get(field).asText();
    }
}
