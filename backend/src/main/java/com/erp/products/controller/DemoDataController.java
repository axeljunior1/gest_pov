package com.erp.products.controller;

import com.erp.products.dto.DemoDataActionResponse;
import com.erp.products.dto.DemoDataStatusResponse;
import com.erp.products.service.DemoDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/demo-data")
@RequiredArgsConstructor
public class DemoDataController {

    private final DemoDataService demoDataService;

    @GetMapping("/status")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.update')")
    public DemoDataStatusResponse status() {
        return demoDataService.getStatus();
    }

    @PostMapping("/generate")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.update')")
    public DemoDataActionResponse generate(@RequestParam(defaultValue = "false") boolean force) {
        return demoDataService.generate(force);
    }

    @DeleteMapping
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.update')")
    public DemoDataActionResponse cleanup() {
        return demoDataService.cleanup();
    }

    @GetMapping("/info")
    @PreAuthorize("@permissionChecker.has(authentication, 'settings.read')")
    public Map<String, Object> info() {
        DemoDataStatusResponse status = demoDataService.getStatus();
        return Map.of(
                "demoPresent", status.isDemoPresent(),
                "demoProducts", status.getDemoProducts(),
                "markerSku", status.getMarkerSku(),
                "warning", "Les donnees de demonstration sont reservees aux tests et formations.");
    }
}
