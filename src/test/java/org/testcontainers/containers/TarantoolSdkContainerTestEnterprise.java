package org.testcontainers.containers;


import io.tarantool.driver.TarantoolVersion;
import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Oleg Kuznetsov
 */
public class TarantoolSdkContainerTestEnterprise {

    @Test
    void test_should_createTarantoolContainerFromSdk() throws URISyntaxException {
        final Map<String, String> buildArgs = new HashMap<>();
        //Official SDK URIs look like: https://user:password@download.tarantool.io/enterprise
        buildArgs.put("DOWNLOAD_SDK_URI", System.getenv("DOWNLOAD_SDK_URI"));
        buildArgs.put("SDK_VERSION", "tarantool-enterprise-bundle-2.7.3-0-gdddf926c3-r443");

        try (final TarantoolContainer tarantoolContainer = new TarantoolContainer(
                new TarantoolImageParams("tarantool-enterprise-bundle:latest", "testsdk/Dockerfile", buildArgs))
                .withDirectoryBinding("testsdk")) {

            tarantoolContainer.start();

            final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client =
                    TarantoolClientFactory.createClient()
                            .withCredentials("api_user", "secret")
                            .withAddress(tarantoolContainer.getHost(), tarantoolContainer.getMappedPort(3301))
                            .build();

            final List<?> result = client.eval("return 'test'").join();
            final TarantoolVersion version = client.getVersion();

            Assertions.assertEquals("test", result.get(0));
            Assertions.assertTrue(version.toString().startsWith("Tarantool 2.7.3 (Binary)"));
        }
    }
}
