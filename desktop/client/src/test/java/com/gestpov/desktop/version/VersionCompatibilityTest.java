package com.gestpov.desktop.version;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionCompatibilityTest {

    @Test
    void sameMajor_isCompatible() {
        assertEquals(CompatibilityStatus.COMPATIBLE, VersionCompatibility.compare("1.0.0", "1.4.2"));
        assertTrue(VersionCompatibility.canConnect(CompatibilityStatus.COMPATIBLE));
    }

    @Test
    void clientOlderMajor_requiresUpdate() {
        assertEquals(CompatibilityStatus.UPDATE_REQUIRED, VersionCompatibility.compare("1.0.0", "2.0.0"));
        assertFalse(VersionCompatibility.canConnect(CompatibilityStatus.UPDATE_REQUIRED));
    }

    @Test
    void serverOlderMajor_isTooOld() {
        assertEquals(CompatibilityStatus.SERVER_TOO_OLD, VersionCompatibility.compare("2.0.0", "1.9.0"));
        assertFalse(VersionCompatibility.canConnect(CompatibilityStatus.SERVER_TOO_OLD));
    }

    @Test
    void blank_isUnknown_andStillConnectable() {
        assertEquals(CompatibilityStatus.UNKNOWN, VersionCompatibility.compare("", "1.0.0"));
        assertTrue(VersionCompatibility.canConnect(CompatibilityStatus.UNKNOWN));
    }
}
