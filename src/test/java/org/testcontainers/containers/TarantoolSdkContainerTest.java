package org.testcontainers.containers;


import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Oleg Kuznetsov
 */
public class TarantoolSdkContainerTest {

    private static final TarantoolContainer tarantoolContainer = new TarantoolContainer(
            new TarantoolImageParams("tarantool-enterprise-bundle-2.8.3-21-g7d35cd2be-r470")
    );

    @Before
    public void setUp() {
        if (!tarantoolContainer.isRunning()) {
            tarantoolContainer.start();
        }
    }

    @Test
    public void test_should_createTarantoolContainerFromSdk() {
        final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client =
                TarantoolClientFactory.createClient()
                        .withCredentials("api_user", "secret")
                        .withAddress(tarantoolContainer.getHost(), tarantoolContainer.getMappedPort(3301))
                        .build();

        final List<?> result = client.eval("return 'test'").join();
        assertEquals("test", result.get(0));
    }
}
