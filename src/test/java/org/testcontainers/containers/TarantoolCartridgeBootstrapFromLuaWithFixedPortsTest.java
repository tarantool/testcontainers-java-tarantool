package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.exceptions.CartridgeTopologyException;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Alexey Kuzin
 */
@Testcontainers
public class TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest {

    @Container
    private static final TarantoolCartridgeContainer container =
            new TarantoolCartridgeContainer(
                    "Dockerfile",
                    "cartridge/instances_fixedport.yml",
                    "cartridge/topology_fixedport.lua")
                    .withDirectoryBinding("cartridge")
                    .withEnv("TARANTOOL_INSTANCES_FILE", "instances_fixedport.yml")
                    .withStartupTimeout(Duration.ofSeconds(300))
                    .withUseFixedPorts(true)
                    .withAPIPort(18081)
                    .withRouterPort(13301)
                    .withLogConsumer(new Slf4jLogConsumer(
                            LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest.class)));

    @Test
    public void test_StaticClusterContainer_StartsSuccessfully_ifFilesAreCopied() throws Exception {
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
    }

    @Test
    public void test_retryingSetupTopology_shouldWork() {
        try (TarantoolCartridgeContainer testContainer =
                 new TarantoolCartridgeContainer(
                     "Dockerfile",
                     "cartridge/instances.yml",
                     "cartridge/incorrect_topology.lua")
                     .withDirectoryBinding("cartridge")
                     .withLogConsumer(new Slf4jLogConsumer(
                         LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest.class)))) {
            ContainerLaunchException ex = assertThrows(ContainerLaunchException.class, testContainer::start);
            Throwable cause = ex.getCause();
            assertEquals(RetryCountExceededException.class, cause.getClass());
            cause = cause.getCause();
            assertEquals(ContainerLaunchException.class, cause.getClass());
            cause = cause.getCause();
            assertEquals(CartridgeTopologyException.class, cause.getClass());
            assertEquals("Failed to change the app topology after retry", cause.getMessage());
        }
    }
}
