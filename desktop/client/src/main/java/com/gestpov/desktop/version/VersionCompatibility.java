package com.gestpov.desktop.version;

/**
 * Comparaison simple major.minor — même major = compatible.
 */
public final class VersionCompatibility {

    private VersionCompatibility() {}

    public static CompatibilityStatus compare(String clientVersion, String serverVersion) {
        Integer clientMajor = major(clientVersion);
        Integer serverMajor = major(serverVersion);
        if (clientMajor == null || serverMajor == null) {
            return CompatibilityStatus.UNKNOWN;
        }
        if (clientMajor.equals(serverMajor)) {
            return CompatibilityStatus.COMPATIBLE;
        }
        if (clientMajor < serverMajor) {
            return CompatibilityStatus.UPDATE_REQUIRED;
        }
        return CompatibilityStatus.SERVER_TOO_OLD;
    }

    public static boolean canConnect(CompatibilityStatus status) {
        return status == CompatibilityStatus.COMPATIBLE || status == CompatibilityStatus.UNKNOWN;
    }

    static Integer major(String version) {
        if (version == null || version.isBlank()) {
            return null;
        }
        String numeric = version.trim().split("[^0-9.]")[0];
        String[] parts = numeric.split("\\.");
        if (parts.length == 0 || parts[0].isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
