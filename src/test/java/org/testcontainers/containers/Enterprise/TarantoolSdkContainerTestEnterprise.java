package org.testcontainers.containers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
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

            final ArrayList<?> result = tarantoolContainer.executeCommandDecoded("return 'test'");
            final ArrayList<?> versionAnswer = tarantoolContainer.executeCommandDecoded("return _TARANTOOL");

            Assertions.assertEquals("test", result.get(0));
            Assertions.assertEquals("2.10.6-0-g5d09e81a6-r557-nogc64", versionAnswer.get(0));
        }
    }
}
