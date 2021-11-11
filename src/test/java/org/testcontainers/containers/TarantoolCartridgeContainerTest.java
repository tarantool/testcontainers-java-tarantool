package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vladimir Rogach
 */
public class TarantoolCartridgeContainerTest {

    @Test
    public void test_ClusterContainer_StartsSuccessfully_ifFilesAreCopiedUnderRoot() throws Exception {
        Map<String, String> buildArgs = new HashMap<String, String>() {
            {
                put("TARANTOOL_SERVER_USER", "root");
                put("TARANTOOL_SERVER_UID", "0");
                put("TARANTOOL_SERVER_GROUP", "root");
                put("TARANTOOL_SERVER_GID", "0");
            }
        };
        TarantoolCartridgeContainer container =
                new TarantoolCartridgeContainer(
                        "Dockerfile",
                        "testcontainers-java-tarantool:test",
                        "cartridge/instances.yml",
                        "cartridge/topology.lua",
                        buildArgs)
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                        .withStartupTimeout(Duration.ofSeconds(300))
                        .withLogConsumer(new Slf4jLogConsumer(
                                LoggerFactory.getLogger(TarantoolCartridgeContainerTest.class)));

        container.start();

        container.executeCommand(
                "return profile_replace(...)", Arrays.asList(1, "Ivanov Ivan Ivanovich", 33, 100500)).get();

        List<?> result = container.executeCommand("return profile_get(...)", 1).get();
        assertEquals(1, result.size());
        assertEquals(33, ((List<?>) result.get(0)).get(3));
    }
}
