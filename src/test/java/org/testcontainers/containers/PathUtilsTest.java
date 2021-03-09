package org.testcontainers.containers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.testcontainers.containers.PathUtils.normalizePath;

/**
 * Tests for path utils .
 *
 * @author Vladimir Rogach
 */
class PathUtilsTest {

    @Test
    void normalizePathTest() {
        Assertions.assertEquals("c:/work/server.lua",
                normalizePath("c:/work/server.lua"));

        Assertions.assertEquals("c:/work/server.lua",
                normalizePath("/c:/work/server.lua"));

        Assertions.assertEquals("c:/work/server.lua",
                normalizePath("/c:\\work\\server.lua"));

        Assertions.assertEquals("c:/work/server.lua",
                normalizePath("c:\\work\\server.lua"));

        Assertions.assertThrows(NullPointerException.class, () -> normalizePath((String)null));
    }
}