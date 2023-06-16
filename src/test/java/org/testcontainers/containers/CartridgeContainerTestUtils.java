package org.testcontainers.containers;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Rogach
 * @author Ivan Dneprov
 */
public class CartridgeContainerTestUtils {

    private CartridgeContainerTestUtils() {
    }

    static public void executeProfileReplaceSmokeTest(TarantoolCartridgeContainer container) throws Exception {
        container.executeCommand(
                "return profile_replace({1, \"Ivanov Ivan Ivanovich\", 33, 100500})");

        List<?> result = container.executeCommandDecoded("return profile_get(1)");
        assertEquals(1, result.size());
        assertEquals(33, ((List<?>) result.get(0)).get(3));
    }
}
