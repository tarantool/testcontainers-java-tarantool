package org.testcontainers.containers;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * @author Alexey Kuzin
 * @author Ivan Dneprov
 */
class TarantoolContainerIT {

    private static final String ENV_TARANTOOL_VERSION = "TARANTOOL_VERSION";
    protected static final String tarantoolVersion = System.getenv(ENV_TARANTOOL_VERSION);

    private void addEnv(String key, String value) throws NoSuchFieldException, IllegalAccessException {
        Class<?> classOfMap = System.getenv().getClass();
        Field field = classOfMap.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writeableEnvironmentVariables = (Map<String, String>) field.get(System.getenv());
        writeableEnvironmentVariables.put(key, value);
        field.setAccessible(false);
    }

    private void removeEnv(String key, String value) throws NoSuchFieldException, IllegalAccessException {
        Class<?> classOfMap = System.getenv().getClass();
        Field field = classOfMap.getDeclaredField("m");
        field.setAccessible(true);
        Map<String, String> writeableEnvironmentVariables = (Map<String, String>) field.get(System.getenv());
        writeableEnvironmentVariables.remove(key);
        field.setAccessible(false);
    }

    @Test
    public void testExecuteScript() throws Exception {
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
            container.executeScript("org/testcontainers/containers/test.lua");
            List<?> result = container.executeCommandDecoded("return user_function_no_param()");
            assertEquals(1, result.size());
            assertEquals(5, result.get(0));
        }
    }

    @Test
    public void testContainerWithParameters() throws Exception {
        int memory = 256 * 1024 * 1024;
        try (TarantoolContainer container = new TarantoolContainer()
                .withDirectoryBinding("io/tarantool")
                .withScriptFileName("custom.lua")
                .withUsername("uuuser")
                .withPassword("secret")
                .withMemtxMemory(memory)
                .withLogLevel(TarantoolLogLevel.INFO)) {
            container.start();

            List<?> result = container.executeCommandDecoded("return box.cfg.memtx_memory");
            assertEquals(1, result.size());
            assertEquals(memory, result.get(0));

            result = container.executeCommandDecoded("return box.cfg.log_level");
            assertEquals(1, result.size());
            assertEquals(5, result.get(0));

            result = container.executeCommandDecoded("return user_function_no_param()");
            assertEquals(result.size(), 1);
            assertEquals(result.get(0), 5);
        }
    }

    @Test
    public void testContainerWithTrueVersion() throws Exception {
        List<String> result;
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
            result = container.executeCommandDecoded("return _TARANTOOL");
        }

        assertEquals(1, result.size());
        assertTrue(result.get(0).startsWith(String.valueOf(tarantoolVersion.charAt(0))));
    }

    @Test
    public void testContainerWithEmptyVersion() throws Exception {
        final String version = "  ";
        addEnv(ENV_TARANTOOL_VERSION, version);
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
        }
        removeEnv(ENV_TARANTOOL_VERSION, version);
    }

    @Test
    public void testContainerWithWrongVersion() throws Exception {
        final String version = "wrong_version";
        addEnv(ENV_TARANTOOL_VERSION, version);
        try (TarantoolContainer container = new TarantoolContainer()) {
            container.start();
        }catch (Exception exc) {
            assertEquals(ContainerFetchException.class, exc.getClass());
        }
        removeEnv(ENV_TARANTOOL_VERSION, version);
    }
}
