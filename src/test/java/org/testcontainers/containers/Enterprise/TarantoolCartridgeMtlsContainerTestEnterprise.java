package org.testcontainers.containers.Enterprise;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.CartridgeContainerTestUtils;
import org.testcontainers.containers.TarantoolCartridgeContainer;
import org.testcontainers.containers.TarantoolImageParams;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Dneprov
 */
@Testcontainers
public class TarantoolCartridgeMtlsContainerTestEnterprise {

    private static TarantoolCartridgeContainer containerWithSsl;

    @BeforeAll
    public static void setUp() throws Exception {
        final File dockerfile = new File(
            TarantoolMTlsContainerTestEnterprise.class.getClassLoader()
                .getResource("enterprise/ssl/mtls/Dockerfile").toURI()
        );
        final Map<String, String> buildArgs = new HashMap<>();
        buildArgs.put("DOWNLOAD_SDK_URI", System.getenv("DOWNLOAD_SDK_URI"));
        buildArgs.put("SDK_VERSION", System.getenv("SDK_VERSION"));

        containerWithSsl = new TarantoolCartridgeContainer(
            new TarantoolImageParams("tarantool-enterprise", dockerfile, buildArgs),
            "cartridge/instances.yml", "cartridge/replicasets.yml")
            .withCopyFileToContainer(MountableFile.forClasspathResource("cartridge"), "/app")
            .withCopyFileToContainer(MountableFile.forClasspathResource("enterprise/ssl"), "/app")
            .withStartupTimeout(Duration.ofSeconds(300))
            .withLogConsumer(new Slf4jLogConsumer(
                LoggerFactory.getLogger(TarantoolCartridgeMtlsContainerTestEnterprise.class)));

        if (!containerWithSsl.isRunning()) {
            containerWithSsl.start();
        }
    }

    @Test
    public void test_clientWithSsl_shouldWork() throws Exception {
        HashMap result = containerWithSsl.executeCommandDecoded("box.cfg.listen");
        HashMap params = (HashMap) result.get("params");
        assertEquals("ssl", params.get("transport"));
        assertEquals("server.key", params.get("ssl_key_file"));
        assertEquals("server.crt", params.get("ssl_cert_file"));
        assertEquals("ca.crt", params.get("ssl_ca_file"));
    }

    @Test
    public void test_StaticClusterContainer_StartsSuccessfully_ifFilesAreCopied() throws Exception {
        CartridgeContainerTestUtils.executeProfileReplaceSmokeTest(containerWithSsl);
    }
}
