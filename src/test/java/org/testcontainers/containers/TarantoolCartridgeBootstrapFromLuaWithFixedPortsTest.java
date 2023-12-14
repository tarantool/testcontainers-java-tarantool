package org.testcontainers.containers;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.exceptions.CartridgeTopologyException;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author Alexey Kuzin
 */
@Testcontainers
public class TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest {

    @Container
    private static final TarantoolCartridgeContainer container =
            new TarantoolCartridgeContainer(
                    "Dockerfile",
                    "cartridge",
                    "cartridge/instances_fixedport.yml",
                    "cartridge/topology_fixedport.lua")
                    .withEnv("TARANTOOL_INSTANCES_FILE", "instances_fixedport.yml")
                    .withStartupTimeout(Duration.ofMinutes(5))
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
    public void testTarantoolClusterCookieDefault() throws Exception {
        Map<String, String> env = container.getEnvMap();
        assertFalse(env.containsKey(TarantoolCartridgeContainer.ENV_TARANTOOL_CLUSTER_COOKIE));
        List<Object> result = container.executeCommandDecoded("return true");
        assertEquals(1, result.size());
        assertTrue((boolean) result.get(0));
    }

    @Test
    public void testTarantoolClusterCookieWithEnv() throws Exception {
        try(TarantoolCartridgeContainer newContainer = new TarantoolCartridgeContainer(
                "Dockerfile",
                "cartridge",
                "cartridge/instances.yml",
                "cartridge/replicasets.yml")
                .withEnv(TarantoolCartridgeContainer.ENV_TARANTOOL_CLUSTER_COOKIE, "secret")
                .withRouterUsername("admin")
                .withRouterPassword("secret")
                .withStartupTimeout(Duration.ofMinutes(5))
                .withLogConsumer(new Slf4jLogConsumer(
                        LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromYamlTest.class))))
        {
            newContainer.start();
            Map<String, String> env = container.getEnvMap();
            assertFalse(env.containsKey(TarantoolCartridgeContainer.ENV_TARANTOOL_CLUSTER_COOKIE));
            List<Object> result = container.executeCommandDecoded("return true");
            assertEquals(1, result.size());
            assertTrue((boolean) result.get(0));
        }
    }

    @Test
    public void test_retryingSetupTopology_shouldWork() {
        try (TarantoolCartridgeContainer testContainer =
                     new TarantoolCartridgeContainer(
                             "Dockerfile",
                             "cartridge",
                             "cartridge/instances.yml",
                             "cartridge/incorrect_topology.lua")
                             .withLogConsumer(new Slf4jLogConsumer(
                                     LoggerFactory.getLogger(
                                             TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest.class)))
                             .withStartupTimeout(Duration.ofMinutes(5))) {
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
