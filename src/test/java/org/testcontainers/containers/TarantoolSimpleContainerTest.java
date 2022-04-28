package org.testcontainers.containers;


import io.tarantool.driver.api.TarantoolClient;
import io.tarantool.driver.api.TarantoolClientFactory;
import io.tarantool.driver.api.TarantoolResult;
import io.tarantool.driver.api.tuple.TarantoolTuple;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.builder.TarantoolBundleContainerBuilderImpl;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class TarantoolSimpleContainerTest {

    private static final DefaultTarantoolContainer tarantoolSimpleContainer =
            new TarantoolBundleContainerBuilderImpl("tarantool-enterprise-bundle-2.8.3-21-g7d35cd2be-r470")
                    .withDirectoryBinding("simple")
                    .build();

    @Before
    public void setUp() {
        if (!tarantoolSimpleContainer.isRunning()) {
            tarantoolSimpleContainer.start();
        }
    }

    @Test
    public void test_should_createTarantoolEnterpriseContainer() {
        //given
        final TarantoolClient<TarantoolTuple, TarantoolResult<TarantoolTuple>> client =
                TarantoolClientFactory.createClient()
                        .withAddress(tarantoolSimpleContainer.getHost(),
                                tarantoolSimpleContainer.getMappedPort(3301))
                        .withCredentials("test_user", "test_password")
                        .build();

        //when
        final List<?> result = client.eval("return 'test'").join();

        //then
        assertEquals(result.get(0), "test");
    }
}
