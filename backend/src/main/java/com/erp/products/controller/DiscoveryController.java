package com.erp.products.controller;

import com.erp.products.discovery.ServerDiscoveryService;
import com.erp.products.dto.DiscoveryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/discovery")
@RequiredArgsConstructor
public class DiscoveryController {

    private final ServerDiscoveryService serverDiscoveryService;

    @GetMapping
    public DiscoveryResponse discovery() {
        return serverDiscoveryService.buildResponse();
    }
}
