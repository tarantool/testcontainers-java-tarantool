package org.testcontainers.containers.Enterprise;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.TarantoolImageParams;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Oleg Kuznetsov
 * @author Ivan Dneprov
 */
public class TarantoolSdkContainerTestEnterprise {


    @Test
    void test_should_createTarantoolContainerFromSdk() throws Exception {
        final File dockerfile = new File(
                TarantoolSdkContainerTestEnterprise.class.getClassLoader().getResource("testsdk/Dockerfile").toURI()
        );
        final Map<String, String> buildArgs = new HashMap<>();
        buildArgs.put("DOWNLOAD_SDK_URI", System.getenv("DOWNLOAD_SDK_URI"));
        buildArgs.put("SDK_VERSION", "tarantool-enterprise-sdk-nogc64-2.10.6-0-r557.linux.x86_64");

        try (final TarantoolContainer tarantoolContainer = new TarantoolContainer(
                new TarantoolImageParams("tarantool-enterprise-bundle:latest", dockerfile, buildArgs))
                .withDirectoryBinding("testsdk")) {

            tarantoolContainer.start();

            final String result = tarantoolContainer.executeCommandDecoded("'test'");
            final String versionAnswer = tarantoolContainer.executeCommandDecoded("_TARANTOOL");

            Assertions.assertEquals("test", result);
            Assertions.assertEquals("2.10.6-0-g5d09e81a6-r557-nogc64", versionAnswer);
        }
    }
}
