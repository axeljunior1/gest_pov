package com.erp.products.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DiscoveryResponse {

    private String application;
    private String serverId;
    private String serverName;
    private String version;
    private String status;
    private String companyName;
    private Integer port;
}
