package com.gestpov.desktop.net;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class JsonLists {

    private JsonLists() {
    }

    public static <T> List<T> mapArray(JsonNode node, Function<JsonNode, T> mapper) {
        List<T> list = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return list;
        }
        node.forEach(item -> {
            T mapped = mapper.apply(item);
            if (mapped != null) {
                list.add(mapped);
            }
        });
        return list;
    }
}
