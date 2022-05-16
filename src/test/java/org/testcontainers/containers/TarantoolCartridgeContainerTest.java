package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

/**
 * @author Vladimir Rogach
 */
public class TarantoolCartridgeContainerTest {

    @Test
    public void test_ClusterContainer_StartsSuccessfully_ifFilesAreCopied() throws Exception {
        TarantoolCartridgeContainer container =
                new TarantoolCartridgeContainer(
                        "Dockerfile",
                        "tarantool-community:latest",
                        "cartridge/instances.yml",
                        "cartridge/topology.lua")
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                        .withStartupTimeout(Duration.ofSeconds(300))
                        .withLogConsumer(new Slf4jLogConsumer(
                                LoggerFactory.getLogger(TarantoolCartridgeContainerTest.class)));

        container.start();
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
        if (container.isRunning())
            container.stop();
    }

    @Test
    public void test_ClusterContainer_StartsSuccessfully_ifFixedPortsAreConfigured() throws Exception {
        TarantoolCartridgeContainer container =
                new TarantoolCartridgeContainer(
                        "Dockerfile",
                        "tarantool-community:latest",
                        "cartridge/instances_fixedport.yml",
                        "cartridge/topology_fixedport.lua")
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge/instances_fixedport.yml"), "/app/instances.yml")
                        .withStartupTimeout(Duration.ofSeconds(300))
                        .withUseFixedPorts(true)
                        .withAPIPort(18081)
                        .withRouterPort(13301)
                        .withLogConsumer(new Slf4jLogConsumer(
                                LoggerFactory.getLogger(TarantoolCartridgeContainerTest.class)));

        container.start();
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
        if (container.isRunning())
            container.stop();
    }


    @Test
    public void test_CartridgeContainer_shouldBootStrapFromYaml() throws Exception {
        TarantoolCartridgeContainer container =
                new TarantoolCartridgeContainer(
                        "Dockerfile",
                        "tarantool-community:latest",
                        "cartridge/instances.yml",
                        "cartridge/replicasets.yml")
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                        .withStartupTimeout(Duration.ofSeconds(300))
                        .withLogConsumer(new Slf4jLogConsumer(
                                LoggerFactory.getLogger(TarantoolCartridgeContainerTest.class)));

        container.start();
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
        if (container.isRunning())
            container.stop();
    }
}
