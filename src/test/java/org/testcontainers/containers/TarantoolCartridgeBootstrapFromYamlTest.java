package org.testcontainers.containers;

import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import static org.junit.Assert.assertEquals;

/**
 * @author Alexey Kuzin
 * @author Valdimir Rogach
 * @author Ivan Dneprov
 */
@Testcontainers
public class TarantoolCartridgeBootstrapFromYamlTest {

    @Container
    private static final TarantoolCartridgeContainer container =
            new TarantoolCartridgeContainer(
                    "Dockerfile",
                    "cartridge",
                    "cartridge/instances.yml",
                    "cartridge/replicasets.yml")
                    .withStartupTimeout(Duration.ofSeconds(300))
                    .withLogConsumer(new Slf4jLogConsumer(
                            LoggerFactory.getLogger(TarantoolCartridgeBootstrapFromYamlTest.class)));

    @Test
    public void test_StaticClusterContainer_StartsSuccessfully_ifFilesAreCopied() throws Exception {
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(container);
    }


    @Test
    public void test_migrator_executesOk() throws Exception {
        ExecResult result = container.executeCommand("return require('migrator').up()");
        assertEquals("---\n" +
                "- ['001_ddl.lua']\n" +
                "...\n" +
                "\n", result.getStdout());
    }
}
