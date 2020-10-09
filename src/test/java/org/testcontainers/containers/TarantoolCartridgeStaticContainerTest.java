package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexey Kuzin
 */
@Testcontainers
public class TarantoolCartridgeStaticContainerTest {

    @Container
    private static final TarantoolCartridgeContainer container =
            new TarantoolCartridgeContainer(
                    TarantoolContainer.DEFAULT_TARANTOOL_BASE_IMAGE,
                    "testcontainers-java-tarantool:test",
                    "cartridge/instances.yml",
                    "cartridge/topology.lua")
                    .withDirectoryBinding("cartridge")
                    .cleanUpDirectory("cartridge/tmp");


    @Test
    public void testContainerWithParameters() throws Exception {
        container.executeCommand(
                "return profile_replace(...)", Arrays.asList(1, "Ivanov Ivan Ivanovich", 33, 100500)).get();

        List<Object> result = container.executeCommand("return profile_get(...)", 1).get();
        assertEquals(1, result.size());
        assertEquals(33, ((List<Object>) result.get(0)).get(3));
    }
}
