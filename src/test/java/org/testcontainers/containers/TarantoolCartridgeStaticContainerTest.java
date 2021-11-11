package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
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
                    "Dockerfile",
                    "testcontainers-java-tarantool:test",
                    "cartridge/instances.yml",
                    "cartridge/topology.lua")
                    .withDirectoryBinding("cartridge")
                    .withStartupTimeout(Duration.ofSeconds(300))
                    .withLogConsumer(new Slf4jLogConsumer(
                            LoggerFactory.getLogger(TarantoolCartridgeStaticContainerTest.class)));

    @Test
    public void testContainerWithParameters() throws Exception {
        container.executeCommand(
                "return profile_replace(...)", Arrays.asList(1, "Ivanov Ivan Ivanovich", 33, 100500)).get();

        List<?> result = container.executeCommand("return profile_get(...)", 1).get();
        assertEquals(1, result.size());
        assertEquals(33, ((List<?>) result.get(0)).get(3));
    }
}
