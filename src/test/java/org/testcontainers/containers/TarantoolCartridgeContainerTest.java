package org.testcontainers.containers;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;

/**
 * @author Vladimir Rogach
 */
public class TarantoolCartridgeContainerTest {

    @Container
    TarantoolCartridgeContainer container;

    @AfterEach
    public void setUp() {
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    @Test
    public void test_ClusterContainer_StartsSuccessfully_ifFilesAreCopied() throws Exception {
        container = new TarantoolCartridgeContainer(
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
    }

    @Test
    public void test_ClusterContainer_StartsSuccessfully_ifFixedPortsAreConfigured() throws Exception {
        container = new TarantoolCartridgeContainer(
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
    }


    @Test
    public void test_CartridgeContainer_shouldBootStrapFromYaml() throws Exception {
        container = new TarantoolCartridgeContainer(
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
    }
}
