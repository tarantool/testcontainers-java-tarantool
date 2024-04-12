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
public class TarantoolSdkContainerEnterpriseIT {


    @Test
    void test_should_createTarantoolContainerFromSdk() throws Exception {
        final File dockerfile = new File(
                TarantoolSdkContainerEnterpriseIT.class.getClassLoader().getResource("enterprise/Dockerfile").toURI()
        );
        final Map<String, String> buildArgs = new HashMap<>();
        String tarantoolRegistry = System.getenv("TARANTOOL_REGISTRY");
        if (tarantoolRegistry != null) {
            buildArgs.put("TARANTOOL_REGISTRY", tarantoolRegistry);
        }
        buildArgs.put("DOWNLOAD_HOST", System.getenv("DOWNLOAD_HOST"));
        buildArgs.put("SDK_PATH", System.getenv("SDK_PATH"));

        try (final TarantoolContainer tarantoolContainer = new TarantoolContainer(
                new TarantoolImageParams("tarantool-enterprise-bundle:latest", dockerfile, buildArgs))
                .withDirectoryBinding("enterprise")) {

            tarantoolContainer.start();

            List<String> result = tarantoolContainer.executeCommandDecoded("return 'test'");

            Assertions.assertEquals("test", result.get(0));
        }
    }
}
