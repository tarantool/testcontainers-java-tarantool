package org.testcontainers.containers;


import io.tarantool.driver.TarantoolVersion;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;

import java.io.File;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Kuznetsov
 */
public class TarantoolSdkContainerTestEnterprise {

    @Test
    void test_should_createTarantoolContainerFromSdk() throws URISyntaxException {
        final File dockerfile = new File(
                TarantoolSdkContainerTestEnterprise.class.getClassLoader().getResource("testsdk/Dockerfile").toURI()
        );
        final Map<String, String> buildArgs = new HashMap<>();
        buildArgs.put("DOWNLOAD_SDK_URI", System.getenv("DOWNLOAD_SDK_URI"));
        buildArgs.put("SDK_VERSION", "tarantool-enterprise-bundle-2.7.3-0-gdddf926c3-r443");

        try (final TarantoolContainer tarantoolContainer = new TarantoolContainer(
                new TarantoolImageParams("tarantool-enterprise-bundle:latest", dockerfile, buildArgs))
                .withDirectoryBinding("testsdk")) {

            tarantoolContainer.start();

            final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client =
                    TarantoolClientFactory.createClient()
                            .withCredentials("api_user", "secret")
                            .withAddress(tarantoolContainer.getHost(), tarantoolContainer.getMappedPort(3301))
                            .build();

            final List<?> result = client.eval("return 'test'").join();
            final TarantoolVersion version = client.getVersion();

            assertEquals("test", result.get(0));
            assertTrue(version.toString().startsWith("Tarantool 2.7.3 (Binary)"));
        }
    }
}
