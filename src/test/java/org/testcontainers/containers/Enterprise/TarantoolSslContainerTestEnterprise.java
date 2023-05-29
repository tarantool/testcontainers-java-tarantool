package org.testcontainers.containers.Enterprise;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.TarantoolImageParams;
import org.testcontainers.containers.output.Slf4jLogConsumer;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ivan Dneprov
 */
public class TarantoolSslContainerTestEnterprise {

    private static final Logger log = LoggerFactory.getLogger(TarantoolSslContainerTestEnterprise.class);

    private static TarantoolContainer containerWithSsl;

    @BeforeAll
    public static void setUp() throws Exception {
        final File dockerfile = new File(
            TarantoolSslContainerTestEnterprise.class.getClassLoader()
                .getResource("enterprise/Dockerfile").toURI()
        );
        final Map<String, String> buildArgs = new HashMap<>();
        buildArgs.put("DOWNLOAD_SDK_URI", System.getenv("DOWNLOAD_SDK_URI"));
        buildArgs.put("SDK_VERSION", System.getenv("SDK_VERSION"));

        containerWithSsl = new TarantoolContainer(
            new TarantoolImageParams("tarantool-enterprise", dockerfile, buildArgs))
            .withScriptFileName("ssl_server.lua")
            .withUsername("test_user")
            .withPassword("test_password")
            .withMemtxMemory(256 * 1024 * 1024)
            .withDirectoryBinding("enterprise/ssl")
            .withSsl()
            .withLogConsumer(new Slf4jLogConsumer(log));

        if (!containerWithSsl.isRunning()) {
            containerWithSsl.start();
        }
    }

    @Test
    public void test_clientWithSsl_shouldWork() throws Exception {
        HashMap result = containerWithSsl.executeCommandDecoded("box.cfg.listen");
        HashMap params = (HashMap) result.get("params");
        assertEquals("ssl", params.get("transport"));
        assertEquals("key.pem", params.get("ssl_key_file"));
        assertEquals("certificate.crt", params.get("ssl_cert_file"));
    }
}
