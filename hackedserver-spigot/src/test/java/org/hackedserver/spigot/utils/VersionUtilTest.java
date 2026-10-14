package org.hackedserver.spigot.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VersionUtilTest {

    @Test
    void parsesLegacyAndYearBasedVersions() {
        assertEquals(1, VersionUtil.parseMajorVersion("1.21.11-R0.1-SNAPSHOT"));
        assertEquals(1, VersionUtil.parseMajorVersion("1.20.4-R0.1-SNAPSHOT"));
        assertEquals(26, VersionUtil.parseMajorVersion("26.3-R0.1-SNAPSHOT"));
        assertEquals(26, VersionUtil.parseMajorVersion("26.1.2-R0.1-SNAPSHOT"));
        assertEquals(27, VersionUtil.parseMajorVersion("27.1"));
    }

    @Test
    void rejectsUnparseableVersions() {
        assertEquals(-1, VersionUtil.parseMajorVersion(null));
        assertEquals(-1, VersionUtil.parseMajorVersion(""));
        assertEquals(-1, VersionUtil.parseMajorVersion("unknown"));
    }
}
