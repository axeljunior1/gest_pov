package com.erp.products.controller;

import com.erp.products.AbstractIntegrationTest;
import com.erp.products.domain.enums.BarcodeType;
import com.erp.products.dto.BarcodeGenerateRequest;
import com.erp.products.dto.UnitConversionRequest;
import com.erp.products.dto.UnitOfMeasureRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UnitAndBarcodeControllerTest extends AbstractIntegrationTest {

    @Test
    void shouldCreateGlobalUnitConversion() throws Exception {
        UnitOfMeasureRequest kg = new UnitOfMeasureRequest();
        kg.setNom("Kilogramme");
        kg.setSymbole("kg");

        String kgResponse = mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(kg)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long kgId = objectMapper.readTree(kgResponse).get("id").asLong();

        UnitOfMeasureRequest g = new UnitOfMeasureRequest();
        g.setNom("Gramme");
        g.setSymbole("g");

        String gResponse = mockMvc.perform(post("/api/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(g)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long gId = objectMapper.readTree(gResponse).get("id").asLong();

        UnitConversionRequest conversion = new UnitConversionRequest();
        conversion.setFromUnitId(kgId);
        conversion.setToUnitId(gId);
        conversion.setFactor(new BigDecimal("1000"));

        mockMvc.perform(post("/api/units/conversions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conversion)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.factor").value(1000));

        mockMvc.perform(get("/api/units/convert")
                        .param("fromUnitId", kgId.toString())
                        .param("toUnitId", gId.toString())
                        .param("quantity", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(2000));
    }

    @Test
    void shouldGenerateBarcode() throws Exception {
        BarcodeGenerateRequest request = new BarcodeGenerateRequest();
        request.setContent("5901234123457");
        request.setType(BarcodeType.QR_CODE);

        mockMvc.perform(post("/api/barcodes/generate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageBase64").isNotEmpty())
                .andExpect(jsonPath("$.type").value("QR_CODE"));
    }
}
