package org.testcontainers.containers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public static boolean isEnvInStdout(String stdout, Map<String, String> env) {
        Map<String, String> envMap = Arrays.stream(stdout.split("\n"))
                                           .collect(Collectors.toMap(toKey -> toKey.split("=")[0],
                                                   toValue -> {
                                                       String[] pair = toValue.split("=");
                                                       if (pair.length == 1) {
                                                           return "null";
                                                       }
                                                       return pair[1];
                                                   }));

        return envMap.entrySet().containsAll(env.entrySet());
    }
}
