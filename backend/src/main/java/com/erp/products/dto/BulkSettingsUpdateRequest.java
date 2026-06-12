package com.erp.products.dto;

import lombok.Data;

import java.util.Map;

@Data
public class BulkSettingsUpdateRequest {

    private Map<String, String> settings;
}
