package com.erp.products.license;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class InstallationIdResponse {
    String installationId;
}
