package org.testcontainers.containers.enterprise;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.TarantoolContainer;
import org.testcontainers.containers.TarantoolImageParams;

/**
 * @author Oleg Kuznetsov
 * @author Ivan Dneprov
 */
public class TarantoolSdkContainerTestEnterprise {


    @Test
    void test_should_createTarantoolContainerFromSdk() throws Exception {
        final File dockerfile = new File(
                TarantoolSdkContainerTestEnterprise.class.getClassLoader().getResource("enterprise/Dockerfile").toURI()
        );
        final Map<String, String> buildArgs = new HashMap<>();
        buildArgs.put("DOWNLOAD_SDK_URI", System.getenv("DOWNLOAD_SDK_URI"));
        buildArgs.put("SDK_VERSION", System.getenv("SDK_VERSION"));

        try (final TarantoolContainer tarantoolContainer = new TarantoolContainer(
                new TarantoolImageParams("tarantool-enterprise-bundle:latest", dockerfile, buildArgs))
                .withDirectoryBinding("enterprise")) {

            tarantoolContainer.start();

            List<String> result = tarantoolContainer.executeCommandDecoded("return 'test'");
            List<String> versionAnswer = tarantoolContainer.executeCommandDecoded("return _TARANTOOL");

            Assertions.assertEquals("test", result.get(0));
            Assertions.assertEquals("2.10.7-0-g2e9b20365-r563-nogc64", versionAnswer.get(0));
        }
    }
}
