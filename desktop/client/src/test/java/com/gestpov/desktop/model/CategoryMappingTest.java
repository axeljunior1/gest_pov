package com.gestpov.desktop.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CategoryMappingTest {

    @Test
    void fromJsonTree() throws Exception {
        var node = new ObjectMapper().readTree("""
                {"id":1,"nom":"Électronique","children":[
                  {"id":2,"nom":"Téléphones","parentId":1,"parentNom":"Électronique","children":[]}
                ]}
                """);
        Category category = Category.fromJson(node);
        assertEquals(1L, category.id());
        assertEquals("Électronique", category.nom());
        assertNull(category.parentId());
        assertEquals(1, category.childrenOrEmpty().size());
        assertEquals(2L, category.childrenOrEmpty().get(0).id());
        assertEquals("Électronique", category.childrenOrEmpty().get(0).parentNom());
    }

    @Test
    void flattenWalksTree() throws Exception {
        var node = new ObjectMapper().readTree("""
                [{"id":1,"nom":"A","children":[{"id":2,"nom":"B","parentId":1,"children":[]}]}]
                """);
        Category root = Category.fromJson(node.get(0));
        List<Category> flat = Category.flatten(List.of(root));
        assertEquals(2, flat.size());
        assertEquals("A", flat.get(0).nom());
        assertEquals("B", flat.get(1).nom());
    }
}
