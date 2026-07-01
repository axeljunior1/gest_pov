package com.erp.products.license;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LicenseEnforcementFilter extends OncePerRequestFilter {

    private final LicenseService licenseService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        if (!licenseService.isEnforcementEnabled() || licenseService.isLicenseValid()) {
            filterChain.doFilter(request, response);
            return;
        }

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();
        if (isExempt(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        LicenseStatusResponse status = licenseService.getStatus();
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Map.of(
                "error", "LICENSE_REQUIRED",
                "message", "Licence Gest_POV requise pour acceder a cette ressource",
                "reason", status.getReason() != null ? status.getReason() : LicenseInvalidReason.LICENSE_MISSING.name(),
                "installationId", status.getInstallationId() != null ? status.getInstallationId() : ""
        ));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return isPublicBootstrapPath(request.getRequestURI());
    }

    private boolean isPublicBootstrapPath(String path) {
        if (path == null) {
            return false;
        }
        return path.startsWith("/actuator/health")
                || path.startsWith("/api/license")
                || path.equals("/api/auth/login")
                || path.equals("/api/settings/public");
    }

    private boolean isExempt(String path) {
        return isPublicBootstrapPath(path);
    }
}
