package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

public class TarantoolCartridgeContainerReplicasetsTest {

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
                        buildArgs)
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                        .withStartupTimeout(Duration.ofSeconds(300))
                        .withLogConsumer(new Slf4jLogConsumer(
                                LoggerFactory.getLogger(TarantoolCartridgeContainerReplicasetsTest.class)));

        container.start();
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
        if(container.isRunning())
            container.stop();
    }

    @Test
    public void test_ClusterContainer_StartsSuccessfully_ifFixedPortsAreConfigured() throws Exception {
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
                        "testcontainers-java-tarantool-fixport:test",
                        "cartridge/instances_fixedport.yml",
                        buildArgs)
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                        .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge/instances_fixedport.yml"),"/app/instances.yml")
                        .withStartupTimeout(Duration.ofSeconds(300))
                        .withUseFixedPorts(true)
                        .withAPIPort(18081)
                        .withRouterPort(13301)
                        .waitingFor(
                                Wait.forLogMessage(".*Listening HTTP on.*", 2)
                        )
                        .withLogConsumer(new Slf4jLogConsumer(
                                LoggerFactory.getLogger(TarantoolCartridgeContainerTest.class)));

        container.start();
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
        if(container.isRunning())
            container.stop();
    }

}
