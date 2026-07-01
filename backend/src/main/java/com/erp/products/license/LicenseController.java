package com.erp.products.license;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/license")
@RequiredArgsConstructor
public class LicenseController {

    private final LicenseService licenseService;

    @GetMapping("/status")
    public LicenseStatusResponse status() {
        return licenseService.getStatus();
    }

    @GetMapping("/installation-id")
    public InstallationIdResponse installationId() {
        return InstallationIdResponse.builder()
                .installationId(licenseService.getInstallationId())
                .build();
    }

    @PostMapping("/import")
    public ResponseEntity<LicenseStatusResponse> importLicense(@RequestParam("file") MultipartFile file) throws Exception {
        return ResponseEntity.ok(licenseService.importLicense(file));
    }
}
