package com.erp.products.license;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class LicenseEnforcementFilterPathTest {

    @Test
    void stripsContextPathAndTrailingSlash() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/discovery/");
        request.setContextPath("");
        request.setRequestURI("/api/discovery/");
        assertThat(LicenseEnforcementFilter.normalizePath(request)).isEqualTo("/api/discovery");
    }
}
