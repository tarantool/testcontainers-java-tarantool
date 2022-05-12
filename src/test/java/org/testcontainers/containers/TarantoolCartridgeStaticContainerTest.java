package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

/**
 * @author Alexey Kuzin
 */
@Testcontainers
public class TarantoolCartridgeStaticContainerTest {

    @Container
    private static final TarantoolCartridgeContainer container =
            new TarantoolCartridgeContainer(
                    "Dockerfile",
                    "cartridge/instances.yml",
                    "cartridge/topology.lua")
                    .withDirectoryBinding("cartridge")
                    .withStartupTimeout(Duration.ofSeconds(300))
                    .withLogConsumer(new Slf4jLogConsumer(
                            LoggerFactory.getLogger(TarantoolCartridgeStaticContainerTest.class)));

    @Test
    public void test_StaticClusterContainer_StartsSuccessfully_ifDirectoryBindingIsUsed() throws Exception {
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
    }
}
