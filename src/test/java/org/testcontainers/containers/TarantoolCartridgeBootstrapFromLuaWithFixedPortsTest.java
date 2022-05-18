package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

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
                    .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge/instances_fixedport.yml"),"/app/instances.yml")
                    .withStartupTimeout(Duration.ofSeconds(300))
                    .withAPIPort(18081)
                    .withRouterPort(13301)
                    .withLogConsumer(new Slf4jLogConsumer(
                            LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromLuaWithFixedPortsTest.class)));

    @Test
    public void test_StaticClusterContainer_StartsSuccessfully_ifFilesAreCopied() throws Exception {
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
    }
}
