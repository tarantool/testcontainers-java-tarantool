package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.testcontainers.containers.PathUtils.normalizePath;

/**
 * Tests for path utils .
 *
 * @author Vladimir Rogach
 */
class PathUtilsTest {

    @Test
    void normalizePathTest() {
        assertEquals("c:/work/server.lua",
                normalizePath("c:/work/server.lua"));

        assertEquals("c:/work/server.lua",
                normalizePath("/c:/work/server.lua"));

        assertEquals("c:/work/server.lua",
                normalizePath("/c:\\work\\server.lua"));

        assertEquals("c:/work/server.lua",
                normalizePath("c:\\work\\server.lua"));

        assertEquals("c:/", normalizePath("c:\\"));

        assertEquals("/dummy", normalizePath("/dummy"));

        assertEquals("/c", normalizePath("/c"));

        assertThrows(NullPointerException.class, () -> normalizePath((String) null));
    }
}
