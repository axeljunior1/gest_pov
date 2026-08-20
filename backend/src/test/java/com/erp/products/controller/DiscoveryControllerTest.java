package com.erp.products.controller;

import com.erp.products.discovery.ServerIdentityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import java.nio.file.Files;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DiscoveryControllerTest extends com.erp.products.AbstractIntegrationTest {

    @Autowired
    private ServerIdentityService serverIdentityService;

    @Test
    void discovery_isPublicAndReturnsStableServerId() throws Exception {
        String first = mockMvc.perform(get("/api/discovery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application").value("GEST_POV"))
                .andExpect(jsonPath("$.serverId").isNotEmpty())
                .andExpect(jsonPath("$.serverName").isNotEmpty())
                .andExpect(jsonPath("$.version").isNotEmpty())
                .andExpect(jsonPath("$.status").value("READY"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String second = mockMvc.perform(get("/api/discovery"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String id1 = objectMapper.readTree(first).get("serverId").asText();
        String id2 = objectMapper.readTree(second).get("serverId").asText();
        assertThat(id1).isEqualTo(id2);
        assertThat(id1).isEqualTo(serverIdentityService.getServerId());
        assertThat(Files.readString(serverIdentityService.resolveFile()).trim()).isEqualTo(id1);
    }

    @Test
    void discovery_exposesNoSecrets() throws Exception {
        String body = mockMvc.perform(get("/api/discovery").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String lower = body.toLowerCase();
        Set<String> forbidden = Set.of(
                "password", "secret", "token", "jwt", "datasource",
                "bootstrap", "private", "credential");
        for (String word : forbidden) {
            assertThat(lower).doesNotContain(word);
        }
        var json = objectMapper.readTree(body);
        java.util.List<String> names = new java.util.ArrayList<>();
        json.fieldNames().forEachRemaining(names::add);
        assertThat(names).doesNotContain("password", "token", "jwtSecret", "datasource");
        assertThat(json.has("application")).isTrue();
        assertThat(json.has("serverId")).isTrue();
    }
}
