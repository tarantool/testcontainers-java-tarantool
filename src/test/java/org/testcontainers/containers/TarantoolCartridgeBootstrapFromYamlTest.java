package org.testcontainers.containers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.List;

/**
 * @author Alexey Kuzin
 */
@Testcontainers
public class TarantoolCartridgeBootstrapFromYamlTest {

    @Container
    private static final TarantoolCartridgeContainer container =
            new TarantoolCartridgeContainer(
                    "Dockerfile",
                    "cartridge/instances.yml",
                    "cartridge/replicasets.yml")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
                    .withStartupTimeout(Duration.ofSeconds(300))
                    .withLogConsumer(new Slf4jLogConsumer(
                            LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromYamlTest.class)));

    @Test
    public void test_StaticClusterContainer_StartsSuccessfully_ifFilesAreCopied() throws Exception {
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
    }


    @Test
    public void test_migrator_executesOk() throws Exception {
        List<?> result = container.executeCommand("return require('migrator').up()").get();
        Assertions.assertEquals("001_ddl.lua", ((List<?>)result.get(0)).get(0));
    }
}
