package com.erp.products.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class PosTestSupport {

    private PosTestSupport() {
    }

    public static void setPosSalesFlowMode(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String adminToken,
            String mode) throws Exception {
        mockMvc.perform(put("/api/settings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "settings", Map.of("pos_sales_flow_mode", mode)))))
                .andExpect(status().isOk());
    }

    public static void useSellerCollectsMode(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String adminToken) throws Exception {
        setPosSalesFlowMode(mockMvc, objectMapper, adminToken, "SELLER_COLLECTS_PAYMENT");
    }
}
