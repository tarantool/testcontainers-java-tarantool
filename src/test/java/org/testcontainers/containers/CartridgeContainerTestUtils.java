package org.testcontainers.containers;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * @author Vladimir Rogach
 */
public class CartridgeContainerTestUtils {

    private CartridgeContainerTestUtils() {
    }

    static public void executeProfileReplaceSmokeTest(TarantoolCartridgeContainer container) throws Exception {
        container.executeCommand(
                "return profile_replace(...)", Arrays.asList(1, "Ivanov Ivan Ivanovich", 33, 100500)).get();

        List<?> result = container.executeCommand("return profile_get(...)", 1).get();
        assertEquals(1, result.size());
        assertEquals(33, ((List<?>) result.get(0)).get(3));
    }
}
